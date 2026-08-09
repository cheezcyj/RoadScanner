# RoadScanner

도로 위 장면에서 교통표지판 후보를 검출하고 독일 GTSRB 43종 범위에서 분류하는 Spring MVC 기반 웹 애플리케이션입니다. 신뢰 기준을 통과하지 못하거나 지원 범위 밖인 이미지는 인식 불가로 처리합니다. 사진 선택부터 미리보기, 분석 결과 확인과 의견 제출까지 하나의 흐름으로 제공하며 Q&A, 비공개 문의와 관리자 운영 기능을 함께 지원합니다.

## 역할별 기능

| 역할 | 제공 기능 |
| --- | --- |
| 방문자 | 메인·서비스 소개, 로그인, 회원가입, 아이디·비밀번호 찾기, Q&A 목록·검색·상세 조회 |
| 회원 | 사진 분석과 의견 제출, 마이페이지, Q&A 작성·수정·삭제, 비공개 문의 작성·수정·삭제와 답변 확인 |
| 관리자 | 계정 상태 관리, 문의 답변, 공지 관리, 이미지 데이터 관리, 분석 피드백 통계 |

## 주요 이용 흐름

- 사진 분석: 이미지 선택 → 미리보기 → 분석 실행 → 결과 확인 → 의견 제출
- 비공개 문의: 문의 작성 → 내 문의글 목록 → 문의 상세 → 관리자 답변 확인
- 관리자 운영: 계정·문의·공지·이미지 데이터 관리 → 분석 피드백 통계 확인

## 기능 시연

아래 화면은 일반화된 로컬 테스트 데이터만 사용합니다. 문서에 이름, 개인 계정, 외부 협업 문서와 개인 저장소 링크를 포함하지 않습니다.

### 서비스 및 계정

#### 메인페이지와 서비스 소개

첫 화면의 주행 애니메이션, 독일 GTSRB 43종 지원 범위와 인식 불가 정책, 이용 순서, 분류 모델 검증 요약과 사진 분석 진입 흐름을 보여 줍니다. 첫 화면은 최적화된 애니메이션과 포스터 대체 이미지를 사용하고, 아래쪽 영상은 해당 영역에 가까워졌을 때 불러옵니다.

![메인페이지와 서비스 소개](docs/demo/landing-overview.gif)

#### 로그인

로그인 양식과 인증 후 사진 분석 화면으로 이어지는 흐름을 보여 줍니다. 인증이 필요한 기능은 로그인 상태와 권한에 따라 접근을 제한합니다.

![로그인](docs/demo/account-login.gif)

#### 회원가입

아이디, 비밀번호와 이메일 입력 양식을 제공하고 입력값 검증과 이메일 인증 흐름을 거쳐 계정을 생성합니다. 로컬 실행에서는 인증 메일을 외부로 보내지 않고 메모리 기반 메일함에서 확인합니다.

![회원가입](docs/demo/account-registration.gif)

#### 아이디·비밀번호 찾기

이메일로 아이디를 찾거나 아이디와 이메일을 확인해 비밀번호 재설정 절차를 시작할 수 있습니다.

![아이디와 비밀번호 찾기](docs/demo/account-recovery.gif)

### 회원 기능

#### 마이페이지

아이디와 이메일을 읽기 전용으로 확인하고 비밀번호를 변경할 수 있습니다. 계정 정보와 비공개 문의 진입 동선을 한 화면에 제공합니다.

![마이페이지](docs/demo/profile-management.gif)

#### 비공개 문의

공개 Q&A 데이터와 분리된 문의를 작성·수정·삭제하고 내 문의글 목록, 상세 내용, 답변 상태와 관리자 답변을 확인할 수 있습니다.

![비공개 문의](docs/demo/private-inquiry-list.gif)

#### 사진 분석

드래그 앤 드롭 또는 파일 선택으로 이미지를 올리고 미리보기, 분석 결과와 의견 제출까지 이어집니다. 결과 화면을 닫으면 초기 업로드 화면으로 돌아가며, 의견을 보낸 뒤에는 결과를 계속 확인할 수 있습니다.

![사진 분석](docs/demo/image-analysis-flow.gif)

#### Q&A 게시판

공지와 일반 글을 함께 조회하고 분류·제목·내용 조건으로 검색할 수 있습니다. 페이징, 상세 조회와 서식 도구가 포함된 글쓰기·수정·삭제 흐름을 제공합니다.

![Q&A 게시판](docs/demo/qna-post-crud.gif)

### 관리자 기능

#### 계정 관리

계정 현황과 일반 회원, 관리자, 이용 제한 계정 목록을 구분해 조회하고 검색할 수 있습니다.

![관리자 계정 관리](docs/demo/admin-account-management.gif)

#### 이미지 데이터 관리

분석 이미지 목록을 상태별로 조회하고 선택한 이미지와 피드백 상세 정보를 한 화면에서 확인합니다.

![관리자 이미지 데이터 관리](docs/demo/admin-image-data-management.gif)

#### 분석 피드백 통계

싫어요 의견의 오류 유형별 누적 수치와 월별 추이를 차트로 확인합니다.

![관리자 분석 피드백 통계](docs/demo/admin-analysis-statistics.gif)

#### 문의 답변 관리

문의 상세에서 관리자 답변을 작성하고 저장된 답변을 확인하거나 수정·삭제할 수 있습니다.

![관리자 문의 답변 관리](docs/demo/qna-answer-management.gif)

#### 공지 관리

공지 작성, Q&A 목록 상단 노출, 상세 조회와 수정·삭제 흐름을 제공합니다.

![관리자 공지 관리](docs/demo/qna-notice-crud.gif)

#### 문의 관리

전체 문의를 답변 상태별로 조회하고 상세 내용과 답변 영역을 함께 관리합니다.

![관리자 문의 관리](docs/demo/admin-qna-management.gif)

## 기술 구성

- Java 8, Spring MVC, MyBatis
- Oracle 운영 DB, H2(Oracle 모드) 기반 로컬·통합 테스트
- AWS S3 객체 스토리지
- Python 기반 교통표지판 검출·분류 서비스
- Maven, JUnit 4, JaCoCo

## 로컬 실행

아래 PowerShell 명령은 저장소 루트에서 실행합니다. 웹 애플리케이션에는 JDK 8과 Maven, 분석 서비스에는 64비트 Python 3.12가 필요합니다. 모델과 첫 화면 애니메이션은 Git LFS로 내려받습니다.

실제 연결 정보와 자격증명은 환경변수 또는 배포 비밀 저장소로 주입합니다. 이 프로젝트는 `.env`를 자동으로 읽지 않으며 `.env.example`에는 변수 이름과 예시만 제공합니다.

### 외부 서비스 없이 실행

`local-smoke` 프로필은 프로세스 내부 H2 DB와 로컬 업로드·메일·분석 대체 구현을 사용합니다. 외부 DB, 객체 스토리지, SMTP 또는 분석 API에는 연결하지 않으며 서버는 `127.0.0.1:18080`에만 바인딩됩니다. 이 모드의 분석 대체 구현은 거짓 표지판 결과를 만들지 않고 항상 인식 불가를 반환합니다.

`local`과 `local-ml`은 개발 전용 프로필입니다. 아래 Maven/Jetty 실행 방식에서만 루프백 바인딩이 강제되므로 이 프로필을 활성화한 WAR를 다른 WAS에 배포하거나 외부에 공개하지 마세요. 임시 관리자 계정과 로컬 메일함이 함께 활성화됩니다.

```powershell
$env:ROADSCANNER_LOCAL_PASSWORD = [System.Net.NetworkCredential]::new("", (Read-Host "로컬 테스트 비밀번호" -AsSecureString)).Password
mvn -B -Plocal-smoke -DskipTests jetty:run
```

실행 후 `http://127.0.0.1:18080/`에 접속합니다. 포트를 바꾸려면 다음처럼 Maven 속성 전체를 따옴표로 감쌉니다.

```powershell
mvn -B -Plocal-smoke -DskipTests "-Droadscanner.local.port=18081" jetty:run
```

비밀번호가 공백 없이 문자·숫자·특수문자를 각각 하나 이상 포함하고 유니코드 문자 8~20개, UTF-8 72바이트 이하 조건을 만족하면 `localuser`와 `localadmin` 계정이 만들어집니다. 두 계정은 입력한 동일한 비밀번호를 사용합니다. 입력 과정은 화면에 표시되지 않고 비밀번호는 파일이나 로그에 기록되지 않으며, 서버를 종료하면 로컬 DB, 업로드와 메일 데이터가 함께 사라집니다. 로컬 메일함은 관리자 권한에서만 보이고 외부로 메일을 전송하지 않습니다.

다른 곳에서 사용하지 않는 로컬 전용 비밀번호를 사용하고, 서버를 종료한 뒤 현재 PowerShell 프로세스에 남은 평문 환경변수를 제거합니다.

```powershell
Remove-Item Env:ROADSCANNER_LOCAL_PASSWORD -ErrorAction SilentlyContinue
```

### 실제 분석 모델 연결

새 체크아웃에서는 Git LFS 모델 파일을 먼저 내려받습니다.

```powershell
git lfs install
git lfs pull
```

새 체크아웃에서는 가상환경과 고정 의존성을 최초 한 번 준비합니다.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r ml_service\requirements.txt
```

웹과 Python 추론 서비스를 항상 함께 실행하려면 통합 실행 스크립트를 사용합니다. 비밀번호는 화면에 표시되지 않으며 파일이나 명령행 인수에 저장되지 않습니다. 스크립트는 추론 서버가 준비된 뒤 `local,local-ml` 웹 서버를 시작하고, 어느 한쪽이 실패하거나 종료되면 나머지 프로세스도 함께 정리합니다.

```powershell
.\scripts\start-local-stack.cmd
```

웹은 `127.0.0.1:18080`, Python 추론 서비스는 `127.0.0.1:5000`에만 바인딩됩니다. 웹은 `/login`, 추론 서비스는 `/health` 응답이 준비될 때까지 스크립트가 확인합니다. 두 서버를 함께 중지하려면 실행한 터미널에서 `Ctrl+C`를 누릅니다. 포트를 바꿔야 한다면 `-WebPort`와 `-MlPort`를 사용합니다.

```powershell
.\scripts\start-local-stack.cmd -WebPort 18081 -MlPort 5001
```

환경을 점검만 하고 서버를 시작하지 않으려면 다음 명령을 사용합니다.

```powershell
.\scripts\start-local-stack.cmd -CheckOnly
```

두 프로세스를 개별적으로 실행해야 하는 경우에는 먼저 Python 추론 서비스를 시작합니다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.app
```

다른 터미널에서 실제 모델 연동 프로필을 실행합니다.

```powershell
$env:ROADSCANNER_LOCAL_PASSWORD = [System.Net.NetworkCredential]::new("", (Read-Host "로컬 테스트 비밀번호" -AsSecureString)).Password
mvn -B -Plocal-smoke -DskipTests "-Droadscanner.local.spring-profiles=local,local-ml" jetty:run
```

전체 사진에서 표지판 후보를 검출한 뒤 GTSRB 43종 분류기로 결과를 판정합니다. 검출 또는 신뢰도·OOD 안전 기준을 통과하지 못한 결과는 인식 불가로 처리합니다. 모델 구성과 데이터셋 정보는 다음 문서를 참고합니다.

- [교통표지판 검출기](docs/ml-detector.md)
- [교통표지판 데이터셋](docs/traffic-sign-datasets.md)

## 운영 배포

- Oracle 배포 전 [보안 스키마 migration](src/main/resources/db/oracle-security-migration.sql)을 적용합니다.
- ML 기능 활성화 전 [교통표지판 결과 카탈로그 SQL](docs/db/oracle-traffic-sign-result-catalog.sql)을 검토·적용합니다.

## 검증

Java 단위 테스트, H2 DAO 통합 테스트와 JaCoCo 커버리지 기준을 검증합니다.

```powershell
mvn -B clean -Pintegration-tests verify
```

Python 분석 서비스의 단위·회귀 테스트와 설치된 의존성의 호환성을 검증합니다.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r ml_service\requirements-dev.txt
.\.venv\Scripts\python.exe -m pytest -q ml_service\tests
.\.venv\Scripts\python.exe -m pip check
```
