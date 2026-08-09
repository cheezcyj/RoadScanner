# RoadScanner 전체 장면 교통표지판 검출기

## 현재 상태와 범위

RoadScanner에는 전체 이미지에서 교통표지판 후보 위치를 찾는 1-class 검출기가 추가되어 있다. 이 검출기는 Open Images의 generic `Traffic sign`과 `Stop sign` bounding box만 학습하며 표지판의 국가나 의미를 판별하지 않는다. 검출된 영역의 의미 판별은 별도의 `road_scanner.h5`가 담당한다.

현재 파이프라인의 정확한 범위는 다음과 같다.

```text
전체 RGB 이미지
  → Open Images 기반 generic traffic-sign 검출
  → 검출이 없고 가로세로 비율 ≤ 1.25인 단일 도안만 guarded crop fallback
  → 검출 상자에 10% 여백을 둔 정사각형 crop
  → GTSRB 기반 독일 표지판 43종 분류
  → 후보가 모두 수락되고 같은 결과일 때만 결과 ID 반환
  → 그 외에는 result ID 44(인식 불가)
```

따라서 Open Images에 여러 국가의 장면이 포함된다는 사실이 RoadScanner가 세계 각국의 표지판 의미를 분류한다는 뜻은 아니다. 현재 의미 분류 범위는 GTSRB 43종에 한정된다.

## 데이터셋

`ml_service/detection_data.py`는 Open Images 공식 CSV를 streaming으로 읽고 `Traffic sign`(`/m/01mqdt`)과 `Stop sign`(`/m/02pv19`)을 하나의 `traffic_sign` 클래스로 합친다. `IsGroupOf`, `IsDepiction`, `IsInside` 상자는 제외한다. 이미지별 원 URL·라이선스·저자 정보, 원 annotation URL과 관측 byte/ETag, 이미지·라벨 SHA-256은 `manifest.json`에 보존한다.

비표지판 장면은 공식 human image-level annotation에서 `Traffic sign`, `Confidence=0`으로 사람이 부재를 확인한 이미지만 사용한다. bbox positive 이미지 ID와 겹치는 항목은 제외하고 빈 YOLO label과 `is_negative: true`로 기록한다.

학습에 사용한 manifest SHA-256은 `E110064123FEF5FA001C148A67ADADBE03A234205D30A911693FDCB8E0DAEE71`이다.

| split | positive 이미지 | hard-negative 이미지 | 전체 이미지 | positive box |
|---|---:|---:|---:|---:|
| train | 1,000 | 250 | 1,250 | 2,019 |
| validation | 38 | 36 | 74 | 59 |

원 annotation에서 train positive 후보는 3,135장, human-verified negative 후보는 bbox positive 4장을 제외한 1,461장이었다. validation은 positive 38장과 negative 36장을 모두 사용했다. 선택은 고정 seed와 이미지 ID의 SHA-256 순서로 이루어져 입력 CSV 순서와 무관하다.

annotation은 CC BY 4.0이지만 이미지 픽셀은 이미지별 라이선스를 따른다. 원본 이미지나 이를 포함한 묶음을 재배포하기 전에는 manifest의 `source_metadata.License`와 attribution을 별도로 검토해야 한다.

## 데이터 재생성

기본 Python 환경에 `ml_service/requirements.txt`를 설치한 뒤 저장소 루트에서 실행한다. 아래 명령이 이번 학습의 1,000/250 및 38/36 구성을 만든다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.detection_data `
  --output-dir data\detector\open-images-v7 `
  --train-images 1000 `
  --train-negative-images 250 `
  --validation-images 0 `
  --validation-negative-images 0 `
  --workers 4 `
  --seed roadscanner-open-images-v7-traffic-sign-v1
```

`0`은 해당 split의 모든 eligible 이미지를 뜻한다. 원 CSV의 `Content-Length`가 코드에 고정된 값과 다르거나 class/schema가 바뀌면 다운로드를 중단한다. 각 이미지도 최대 byte·pixel 크기, JPEG 디코드, 실제 수신 byte를 확인한 뒤 `.part` 파일을 원자적으로 교체한다.

같은 seed는 같은 이미지 ID 집합을 선택하지만 `generated_at`, 기존 파일의 다운로드 상태 등 실행 관측값도 manifest에 기록되므로 새로 생성한 manifest의 byte 단위 SHA-256까지 같다고 가정하면 안 된다. 정확한 실행을 감사하거나 재개할 때는 `runs/traffic-sign-detector-v1/dataset-manifest.json` snapshot과 그 SHA-256을 기준으로 삼는다.

## 학습 환경과 명령

검출기는 torchvision `ssdlite320_mobilenet_v3_large`이며 background를 제외한 foreground는 하나다. MobileNetV3 backbone만 ImageNet `IMAGENET1K_V1`로 초기화하고 detection head는 새로 학습한다. 입력은 RGB `[0,1]`, `1×3×320×320`이고 종횡비를 유지한 letterbox의 빈 영역은 RGB `(114,114,114)`로 채운다.

기록된 학습 환경은 Python 3.12.13, PyTorch `2.12.1+cu126`, torchvision `0.27.1+cu126`, ONNX `1.22.0`, ONNX Runtime `1.27.0`이다. 별도 환경을 만들 때는 다음처럼 버전을 고정한다.

```powershell
python -m venv .venv-detector
.\.venv-detector\Scripts\python.exe -m pip install `
  torch==2.12.1+cu126 torchvision==0.27.1+cu126 `
  --index-url https://download.pytorch.org/whl/cu126
.\.venv-detector\Scripts\python.exe -m pip install `
  -r ml_service\requirements-detector-training.txt
```

실제 학습 명령은 다음과 같다.

```powershell
.\.venv-detector\Scripts\python.exe -m ml_service.train_detector `
  --manifest data\detector\open-images-v7\manifest.json `
  --output-dir runs\traffic-sign-detector-v1 `
  --epochs 20 `
  --batch-size 16 `
  --learning-rate 0.01 `
  --momentum 0.9 `
  --weight-decay 0.0005 `
  --seed 20260803 `
  --device cuda `
  --amp `
  --pretrained-backbone `
  --score-threshold 0.5 `
  --iou-threshold 0.5 `
  --max-detections 100 `
  --export-onnx `
  --onnx-path traffic_sign_detector.onnx
```

학습은 SGD와 cosine annealing, CUDA AMP를 사용했다. 밝기·대비·채도 0.8~1.2와 수평 반전을 train에만 적용했다. Windows에서 순서를 고정하기 위해 loader worker는 0이며 Python·NumPy·PyTorch·CUDA seed와 deterministic 설정을 적용한다. 다만 다른 GPU, 드라이버, PyTorch/CUDA build에서 학습 결과와 ONNX byte hash가 완전히 같다고 보장할 수는 없다.

재개할 때는 동일 manifest SHA를 가진 checkpoint만 허용한다. 예를 들어 20 epoch의 `last.pt`에서 총 30 epoch까지 계속하려면 `--resume runs\traffic-sign-detector-v1\last.pt --epochs 30`을 추가한다.

## 학습 및 내보내기 결과

20 epoch 학습 중 validation AP50가 가장 높았던 epoch index 5, 즉 6번째 epoch의 checkpoint를 ONNX로 내보냈다. AP50은 이 코드가 계산하는 1-class, IoU 0.5, 101-point 보간 지표이며 공식 COCO 전체 지표 묶음은 아니다.

| 항목 | 결과 |
|---|---:|
| best validation AP50 | 0.377702 |
| score ≥ 0.5, IoU ≥ 0.5 precision | 30 / 64 = 46.8750% |
| score ≥ 0.5, IoU ≥ 0.5 recall | 30 / 59 = 50.8475% |
| false positives at score ≥ 0.5 | 34 |
| 전체 20 epoch 시간 | 212.801초 |

validation 74장을 매 epoch 평가해 best checkpoint를 선택했으므로 이 수치는 독립 test 성능이 아니다. 특히 positive validation이 38장뿐이어서 국가·날씨·카메라별 일반화 근거로 사용하면 안 된다.

배포 artifact는 `traffic_sign_detector.onnx`, 15,022,923바이트이며 SHA-256은 다음과 같다.

```text
D1EC740200141D1BB6D96935ACC947216CFA28911A6D8F4F56F2832E7B30CF03
```

ONNX checker를 통과했고 같은 validation sample에서 PyTorch 대비 ONNX Runtime 최대 절대 오차는 box `6.103515625e-05`, score `1.415610313e-07`, label `0`이었다. 이는 내보내기 parity 검사이지 실제 검출 정확도 검사가 아니다.

```powershell
Get-FileHash .\traffic_sign_detector.onnx -Algorithm SHA256
Get-FileHash .\data\detector\open-images-v7\manifest.json -Algorithm SHA256
```

## 추론 적용

일반 추론 환경에는 `ml_service/requirements.txt`의 ONNX Runtime이 포함된다. detector 모드는 모델 hash가 명시되지 않으면 시작하지 않는다.

```powershell
$env:ROADSCANNER_PIPELINE_MODE = "detect"
$env:ROADSCANNER_DETECTOR_MODEL_PATH = "traffic_sign_detector.onnx"
$env:ROADSCANNER_EXPECTED_DETECTOR_MODEL_SHA256 = "D1EC740200141D1BB6D96935ACC947216CFA28911A6D8F4F56F2832E7B30CF03"
$env:ROADSCANNER_DETECTOR_MIN_SCORE = "0.7"
$env:ROADSCANNER_DETECTOR_NMS_IOU_THRESHOLD = "0.45"
$env:ROADSCANNER_DETECTOR_MAX_CANDIDATES = "10"
$env:ROADSCANNER_ALLOW_CROP_FALLBACK = "true"
$env:ROADSCANNER_CROP_FALLBACK_MAX_ASPECT = "1.25"
$env:ROADSCANNER_OOD_THRESHOLD_OVERRIDES = "12:0.77"
.\.venv\Scripts\python.exe -m ml_service.app
```

이 값들은 현재 코드와 `.env.example`의 기본값이다. 분류기 OOD class threshold에는 기본적으로 `ROADSCANNER_OOD_SAFETY_MARGIN=0.07`을 더해, 낯선 표지가 GTSRB 43종으로 강제 확정되는 경우를 더 보수적으로 거부한다. 다만 GTSRB class 12 `priority_road`는 깨끗한 공식 도안도 raw class confidence 1.0으로 맞게 분류하면서 전역 safety margin 때문에 거부되는 회귀가 확인되어 effective threshold를 `0.77`로 고정했다. `ROADSCANNER_OOD_THRESHOLD_OVERRIDES`는 쉼표로 구분한 `class_id:threshold` 형식이며, 지정되지 않은 클래스는 계속 전역 margin을 사용한다. 이 운영 정책은 `/health`와 평가 보고서의 `ood_threshold_overrides`에 노출되며 기존 OOD reference 파일과 hash는 바꾸지 않는다.

runtime은 ONNX 입출력 shape·이름·값 범위와 모든 label이 foreground `1`인지 검사하고, 원본 좌표로 복원한 뒤 score filter와 NMS를 적용한다. artifact가 없거나 hash가 다르거나 계약이 맞지 않으면 fail-closed 한다. `/health`는 pipeline mode, detector hash와 threshold를 반환하고 `/predict/debug`은 검출·crop·분류 후보와 최종 거부 사유를 반환한다.

단일 정수 결과 ID만 받는 기존 Java 계약 때문에 여러 후보를 임의로 하나 고르지 않는다. 검출이 없으면 원칙적으로 `no_sign_detected`를 반환한다. 단, 사용자가 올리는 정사각형에 가까운 표지 도안을 지원하기 위해 대칭 종횡비가 1.25 이내인 입력만 전체 이미지를 분류기에 한 번 전달하고 기존 confidence·margin·OOD 기준을 모두 통과할 때만 crop fallback을 수락한다. `/predict/debug`의 `crop_fallback_attempted`와 `crop_fallback_used`로 이 경로를 구분할 수 있다. 모든 검출 crop이 거부되면 `all_candidates_rejected`, 일부만 수락되거나 수락된 class가 서로 다르면 `ambiguous`로 result ID 44를 반환한다. 모든 후보가 같은 GTSRB 결과로 수락될 때만 그 ID를 반환한다.

이미 표지판만 잘라낸 입력을 기존 방식으로 검사하려면 `ROADSCANNER_PIPELINE_MODE=crop`을 명시한다.

## 최종 runtime 평가

배포와 같은 score `0.7`, NMS IoU `0.45`, 최대 후보 `10`, 기본 OOD safety margin `0.07`과 class 12 effective threshold `0.77`로 manifest의 validation 74장을 다시 평가했다. runtime NMS까지 적용한 검출기 AP50은 `0.367637`이며, threshold 지점에서 precision은 27/50 = `54.0000%`, recall은 27/59 = `45.7627%`였다. 검출기만 보면 사람이 `Traffic sign` 부재를 확인한 장면 36장 중 20장에도 후보가 남았다.

후단 GTSRB 분류기, guarded crop fallback과 strict consensus까지 포함한 실제 서비스 판정에서는 positive 38장 중 7장만 수락했고 negative 36장은 모두 result ID 44로 거부했다. 수락된 7장은 모두 Open Images `Stop sign`이 포함된 13장 중 일부였고 RoadScanner의 stop result ID `15`로 일치했다. 이 평가는 generic Open Images manifest에 GTSRB 의미 class 정답이 없으므로 전체 semantic accuracy를 뜻하지 않는다.

수동 회귀 입력도 별도로 확인했다. `data/manual-validation/positive`의 GTSRB 지원 도안 7장은 rejected-only 도안 프로토타입까지 포함한 실제 서비스 경로에서 7/7 올바르게 수락했다. `data/manual-validation/unknown`으로 분리한 GTSRB 미지원 도안 `13.jpg`, `26.jpg`는 2/2 ID 44를 유지했다. 이 9장은 OOD reference나 프로토타입 생성 입력에는 넣지 않았다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.evaluate_detector `
  --manifest data\detector\open-images-v7\manifest.json `
  --detector traffic_sign_detector.onnx `
  --expected-detector-sha256 D1EC740200141D1BB6D96935ACC947216CFA28911A6D8F4F56F2832E7B30CF03 `
  --score-threshold 0.7 --nms-iou-threshold 0.45 --max-detections 10 `
  --provider CPUExecutionProvider `
  --classifier-model road_scanner.h5 `
  --class-map ml_service\class_map.json `
  --ood-reference ml_service\ood_reference.npz `
  --expected-classifier-sha256 7F9FD2D60F907FC346185B4CE62C4C50C2879DF4469906BBD10FBD26A4ECB0CE `
  --expected-class-map-sha256 A5001A6CFAC4CF1C1135ABA53312226A59617FA635DF09B1C9B7B9F037A01F8E `
  --expected-ood-reference-sha256 B291EB750E33EFD59E27E09E5EA2236DE05E38624E2355EC0B42E653A3E43F4D `
  --classifier-ood-safety-margin 0.07 `
  --output runs\traffic-sign-detector-v1\evaluation-final.json
```

## 남은 한계와 다음 검증

- Open Images 검출기는 generic 표지판 위치만 찾는다. 속도제한 값, 문자, 국가별 의미는 구분하지 않는다.
- 후단 분류기는 독일 GTSRB 43종이다. 한국·중국·일본 등 다른 체계의 표지판은 올바른 의미를 낼 보장이 없으며 GTSRB class로 오분류되거나 `인식 불가`가 될 수 있다.
- validation이 작고 같은 validation으로 best epoch를 선택했다. 별도 Open Images test, 실제 RoadScanner 업로드 장면, 국가·주야·악천후·소형 표지판별 독립 평가가 필요하다.
- AP50 0.377702와 score 0.5에서 precision 46.8750%, recall 50.8475%는 초기 기준선이다. 현재 artifact를 안전 관련 판단이나 무감독 운영에 바로 사용해서는 안 된다.
- hard-negative는 `Traffic sign` 부재를 사람이 확인한 Open Images 표본 286장뿐이다. 도로 시설물, 광고, 로고, 원거리 표지판 등 실제 오탐 유형을 충분히 대표하지 않는다.
- runtime score 0.7은 이 작은 validation에서 오탐 억제를 우선해 선택한 초기 운영값이다. 같은 validation으로 epoch와 threshold를 정했으므로 독립 calibration 결과가 아니며 새 test set에서 다시 고정해야 한다.
- 이미지별 라이선스와 attribution을 보존해야 하며, 원 이미지 또는 파생 데이터의 제품 배포 가능성은 별도 법적 검토 대상이다.
- 다음 단계는 독립 full-scene test set을 먼저 고정하고 AI Hub·ZOD·GLARE 등으로 국가·환경 coverage를 늘린 뒤, 국가별 classifier와 공통 ontology를 분리 학습하는 것이다.
