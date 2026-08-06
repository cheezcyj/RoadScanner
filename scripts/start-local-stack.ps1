[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$WebPort = 18080,

    [ValidateRange(1, 65535)]
    [int]$MlPort = 5000,

    [ValidateRange(10, 300)]
    [int]$StartupTimeoutSeconds = 120,

    [switch]$CheckOnly,

    [switch]$SmokeTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pythonPath = Join-Path $repositoryRoot '.venv\Scripts\python.exe'
$classifierModelPath = Join-Path $repositoryRoot 'road_scanner.h5'
$detectorModelPath = Join-Path $repositoryRoot 'traffic_sign_detector.onnx'
$localStateDirectory = Join-Path $repositoryRoot '.roadscanner-local'
$mavenCommand = Get-Command 'mvn.cmd' -ErrorAction Stop

function Assert-RequiredFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Description 파일이 없습니다: $Path"
    }
}

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    $waitHandle = $null
    try {
        $asyncResult = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        $waitHandle = $asyncResult.AsyncWaitHandle
        if (-not $waitHandle.WaitOne(300)) {
            return $false
        }
        $client.EndConnect($asyncResult)
        return $true
    } catch {
        return $false
    } finally {
        if ($null -ne $waitHandle) {
            $waitHandle.Close()
        }
        $client.Close()
    }
}

function Test-HttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [switch]$RequireReadyStatus
    )

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 3
        if ($response.StatusCode -ne 200) {
            return $false
        }
        if ($RequireReadyStatus) {
            $payload = $response.Content | ConvertFrom-Json
            return $payload.status -eq 'ready'
        }
        return $true
    } catch {
        return $false
    }
}

function Wait-ServiceReady {
    param(
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$HealthUri,

        [Parameter(Mandatory = $true)]
        [string]$ErrorLogPath,

        [switch]$RequireReadyStatus
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    do {
        if ($Process.HasExited) {
            throw "$Name 프로세스가 준비되기 전에 종료됐습니다. 로그: $ErrorLogPath"
        }
        if (Test-HttpReady -Uri $HealthUri -RequireReadyStatus:$RequireReadyStatus) {
            return
        }
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "$Name 준비 시간이 초과됐습니다. 로그: $ErrorLogPath"
}

function Get-LoopbackListenerProcessId {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $endpoint = [regex]::Escape("127.0.0.1:$Port")
    $pattern = "^\s*TCP\s+$endpoint\s+\S+\s+LISTENING\s+(\d+)\s*$"
    foreach ($line in (& "$env:SystemRoot\System32\netstat.exe" -ano -p tcp)) {
        if ($line -match $pattern) {
            return [int]$Matches[1]
        }
    }
    return $null
}

function Stop-ServiceProcess {
    param(
        [System.Diagnostics.Process]$Process,

        [Parameter(Mandatory = $true)]
        [int]$Port,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedProcessName
    )

    $listenerProcessId = Get-LoopbackListenerProcessId -Port $Port
    if ($null -ne $listenerProcessId) {
        $listenerProcess = Get-Process -Id $listenerProcessId -ErrorAction SilentlyContinue
        if ($null -ne $listenerProcess) {
            if ($listenerProcess.ProcessName -ne $ExpectedProcessName) {
                Write-Warning (
                    "포트 $Port 프로세스가 예상한 $ExpectedProcessName 이 아니어서 종료하지 않습니다."
                )
            } else {
                Stop-Process -Id $listenerProcessId -Force -ErrorAction SilentlyContinue
            }
        }
    }

    if ($null -ne $Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Set-ProcessEnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [object]$Value
    )

    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

Assert-RequiredFile -Path $pythonPath -Description 'Python 가상환경 실행 파일'
Assert-RequiredFile -Path $classifierModelPath -Description 'GTSRB 분류 모델'
Assert-RequiredFile -Path $detectorModelPath -Description '교통표지판 검출 모델'

if ($CheckOnly) {
    Write-Host '로컬 웹·추론 통합 실행에 필요한 파일과 명령을 확인했습니다.'
    return
}

if (Test-TcpPort -Port $WebPort) {
    throw "웹 포트 $WebPort 를 이미 다른 프로세스가 사용하고 있습니다."
}
if (Test-TcpPort -Port $MlPort) {
    throw "추론 포트 $MlPort 를 이미 다른 프로세스가 사용하고 있습니다."
}

New-Item -ItemType Directory -Path $localStateDirectory -Force | Out-Null

$mlOutputLog = Join-Path $localStateDirectory 'ml-server-stdout.log'
$mlErrorLog = Join-Path $localStateDirectory 'ml-server-stderr.log'
$webOutputLog = Join-Path $localStateDirectory 'web-server-stdout.log'
$webErrorLog = Join-Path $localStateDirectory 'web-server-stderr.log'
$mlProcess = $null
$webProcess = $null
$managedEnvironmentNames = @(
    'ROADSCANNER_LOCAL_PASSWORD',
    'ROADSCANNER_ML_HOST',
    'ROADSCANNER_ML_PORT',
    'ROADSCANNER_ALLOWED_IMAGE_HOSTS',
    'ROADSCANNER_ALLOWED_IMAGE_PORTS'
)
$originalEnvironment = @{}
foreach ($environmentName in $managedEnvironmentNames) {
    $originalEnvironment[$environmentName] = [Environment]::GetEnvironmentVariable(
        $environmentName,
        'Process'
    )
}
$plainPassword = $originalEnvironment['ROADSCANNER_LOCAL_PASSWORD']

if ([string]::IsNullOrWhiteSpace($plainPassword)) {
    $securePassword = Read-Host '로컬 테스트 비밀번호' -AsSecureString
    $credential = New-Object System.Net.NetworkCredential('', $securePassword)
    $plainPassword = $credential.Password
}

try {
    Set-ProcessEnvironmentValue -Name 'ROADSCANNER_LOCAL_PASSWORD' -Value $null
    Set-ProcessEnvironmentValue -Name 'ROADSCANNER_ML_HOST' -Value '127.0.0.1'
    Set-ProcessEnvironmentValue -Name 'ROADSCANNER_ML_PORT' -Value ([string]$MlPort)
    Set-ProcessEnvironmentValue `
        -Name 'ROADSCANNER_ALLOWED_IMAGE_HOSTS' `
        -Value '127.0.0.1,localhost,::1'
    Set-ProcessEnvironmentValue `
        -Name 'ROADSCANNER_ALLOWED_IMAGE_PORTS' `
        -Value "80,443,$WebPort"

    $mlProcess = Start-Process `
        -FilePath $pythonPath `
        -ArgumentList @('-m', 'ml_service.app') `
        -WorkingDirectory $repositoryRoot `
        -RedirectStandardOutput $mlOutputLog `
        -RedirectStandardError $mlErrorLog `
        -WindowStyle Hidden `
        -PassThru

    foreach ($environmentName in @(
        'ROADSCANNER_ML_HOST',
        'ROADSCANNER_ML_PORT',
        'ROADSCANNER_ALLOWED_IMAGE_HOSTS',
        'ROADSCANNER_ALLOWED_IMAGE_PORTS'
    )) {
        Set-ProcessEnvironmentValue `
            -Name $environmentName `
            -Value $originalEnvironment[$environmentName]
    }

    Wait-ServiceReady `
        -Process $mlProcess `
        -Name '추론 서버' `
        -HealthUri "http://127.0.0.1:$MlPort/health" `
        -ErrorLogPath $mlErrorLog `
        -RequireReadyStatus

    Set-ProcessEnvironmentValue -Name 'ROADSCANNER_LOCAL_PASSWORD' -Value $plainPassword
    try {
        $webProcess = Start-Process `
            -FilePath $mavenCommand.Source `
            -ArgumentList @(
                '-B',
                '-q',
                '-Plocal-smoke',
                '-DskipTests',
                "-Droadscanner.local.port=$WebPort",
                "-Droadscanner.local.public-base-url=http://127.0.0.1:$WebPort",
                '-Droadscanner.local.spring-profiles=local,local-ml',
                "-Droadscanner.local.ml.api-url=http://127.0.0.1:$MlPort/predict",
                'jetty:run'
            ) `
            -WorkingDirectory $repositoryRoot `
            -RedirectStandardOutput $webOutputLog `
            -RedirectStandardError $webErrorLog `
            -WindowStyle Hidden `
            -PassThru
    } finally {
        Set-ProcessEnvironmentValue `
            -Name 'ROADSCANNER_LOCAL_PASSWORD' `
            -Value $originalEnvironment['ROADSCANNER_LOCAL_PASSWORD']
    }

    Wait-ServiceReady `
        -Process $webProcess `
        -Name '웹 서버' `
        -HealthUri "http://127.0.0.1:$WebPort/login" `
        -ErrorLogPath $webErrorLog

    Write-Host "웹 서버: http://127.0.0.1:$WebPort/"
    Write-Host "추론 서버: http://127.0.0.1:$MlPort/health"
    Write-Host '두 서버가 함께 실행 중입니다. 중지하려면 Ctrl+C를 누르세요.'

    if ($SmokeTest) {
        Write-Host '스모크 검증이 완료되어 두 서버를 함께 종료합니다.'
        return
    }

    while (-not $webProcess.HasExited -and -not $mlProcess.HasExited) {
        Start-Sleep -Seconds 1
    }

    if ($webProcess.HasExited) {
        throw "웹 서버가 종료되어 추론 서버도 함께 종료합니다. 로그: $webErrorLog"
    }
    throw "추론 서버가 종료되어 웹 서버도 함께 종료합니다. 로그: $mlErrorLog"
} finally {
    $plainPassword = $null
    foreach ($environmentName in $managedEnvironmentNames) {
        Set-ProcessEnvironmentValue `
            -Name $environmentName `
            -Value $originalEnvironment[$environmentName]
    }
    Stop-ServiceProcess `
        -Process $webProcess `
        -Port $WebPort `
        -ExpectedProcessName 'java'
    Stop-ServiceProcess `
        -Process $mlProcess `
        -Port $MlPort `
        -ExpectedProcessName 'python'
}
