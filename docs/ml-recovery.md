# RoadScanner 기존 모델 복구

## 결론

`road_scanner.h5`는 손상되거나 유실되지 않았다. 현재 파일과 2023년 복구 백업본은 크기 14,133,180바이트와 SHA-256 `7F9FD2D60F907FC346185B4CE62C4C50C2879DF4469906BBD10FBD26A4ECB0CE`가 같다.

잘못된 분석 결과의 직접 원인은 로컬 프로필이 업로드 이미지와 무관하게 항상 결과 ID `1`을 반환하던 대체 구현이었다. 여기에 43-class softmax가 학습 범위 밖 이미지에도 높은 확률을 내는 문제가 겹쳤다. 저장소에는 모델 파일만 남고 원본 학습 데이터, 학습 코드, Flask 추론 서버와 운영 DB 클래스 매핑은 없었다.

기본 `local` 대체 구현은 이제 결과 ID `44`(`인식 불가`)만 반환한다. 실제 추론은 `local,local-ml`에서만 켜지며, 모델은 Git LFS 대상으로 지정하고 알려진 SHA-256을 시작 시 강제 검증한다.

## 복구된 모델 계약

이 절의 모델은 검출기가 아니라 검출된 crop의 의미를 판별하는 후단 분류기다.

- 저장 버전: TensorFlow/Keras 2.12.0
- 검증 실행 버전: TensorFlow 2.16.2, `compile=False`
- 입력: `(None, 30, 30, 3)` float32
- 출력: GTSRB class ID `0..42`의 43개 softmax 값
- 검증된 전처리: 이미지 RGB 디코드 → BGR 채널 순서 → bicubic 30×30 → float32 `/ 255.0`
- 명시적 결과 매핑: model class `0..42` → local `RESULT_IMAGE.no` `1..43`
- 결과 ID 44: 낮은 신뢰도·margin, 작은/저정보량 이미지, 가로로 넓은 장면 또는 training 특징 분포 밖 입력의 `인식 불가`
- OOD 보강: 마지막 512차원 특징과 GTSRB training class centroid의 cosine similarity를 class별 기준과 비교

사람용 클래스 이름은 GTSRB 공식 정답 필드가 아니라 UI 표시 번역이다. 공식 GTSRB 정답은 숫자 ClassId이므로 `ml_service/class_map.json`은 숫자 ID와 표시 이름을 구분한다.

## API

Java 호환 엔드포인트:

```http
POST /predict
Content-Type: application/json
Accept: text/plain

{"image_url":"http://127.0.0.1:18080/local-files/<uuid>.png"}
```

성공 응답은 DB 결과 ID 한 개다.

```text
34
```

진단용 `POST /predict/debug`은 raw class ID, 결과 ID, 영문·한국어 이름, confidence, margin, OOD similarity/threshold, top-3와 모델·class map·OOD 기준 해시를 JSON으로 반환한다. Java 호환 `POST /predict`도 `X-RoadScanner-Catalog-SHA256` 응답 헤더를 보내며 Java가 배포된 DB catalog와 같은 해시인지 확인한다. 이미지 URL은 환경변수 `ROADSCANNER_ALLOWED_IMAGE_HOSTS`의 정확한 호스트만 허용하고 redirect, 사용자정보가 포함된 URL, 5MB 초과 응답과 비이미지 응답을 거부한다. 복구 실행은 두 서비스 모두 loopback에만 바인딩한다.

## 실행 모드

- `local`: 외부 ML 서비스가 필요 없는 Java 테스트와 UI 개발용이며, 거짓 양성을 막기 위해 항상 `인식 불가`를 반환한다.
- `local,local-ml`: H2와 메모리 업로드는 유지하면서 `http://127.0.0.1:5000/predict`의 실제 모델을 호출한다.
- 운영: 기존 외부 분석 API 호출 경로를 유지한다. 먼저 `docs/db/oracle-traffic-sign-result-catalog.sql`을 적용해야 하며, 시작 시 DB 44행을 class map과 대조해 불일치하면 fail-closed 한다.

자세한 실행 명령은 루트 `README.md`에 있다.

## 추가된 전체 장면 검출 파이프라인

전체 도로 사진을 바로 30×30 분류기에 넣어 엉뚱한 GTSRB class가 나오던 경로를 막기 위해 Open Images 기반 1-class 검출기를 분류기 앞에 추가했다. 검출기는 generic `Traffic sign`과 `Stop sign` 위치만 찾고, 각 상자를 10% 여백의 정사각형으로 잘라 기존 GTSRB 43종 분류기에 전달한다. 검출이 없으면 원칙적으로 ID 44를 반환하되, 종횡비 1.25 이내의 단일 표지 도안만 동일한 OOD 기준으로 guarded crop fallback을 시도한다. crop 분류가 하나라도 거부되거나 여러 후보의 분류 결과가 서로 다르면 기존 단일 결과 ID API는 `44`(`인식 불가`)를 반환한다.

사용한 detector 데이터는 train positive 1,000장·hard-negative 250장, validation positive 38장·hard-negative 36장이다. positive box는 각각 2,019개와 59개다. hard-negative는 Open Images human image-label에서 `Traffic sign`, `Confidence=0`인 항목만 선택하고 bbox positive 이미지와 겹치는 ID를 제외했다. 학습 manifest SHA-256은 `E110064123FEF5FA001C148A67ADADBE03A234205D30A911693FDCB8E0DAEE71`이다.

torchvision SSDLite320 MobileNetV3 Large를 20 epoch 학습한 초기 기준선의 best validation AP50는 `0.377702`였다. score 0.5와 IoU 0.5에서 precision은 30/64 = 46.8750%, recall은 30/59 = 50.8475%였다. 이 validation으로 best epoch도 선택했으므로 독립 test 결과가 아니며 운영 성능으로 해석하면 안 된다.

내보낸 `traffic_sign_detector.onnx`는 15,022,923바이트이고 SHA-256은 `D1EC740200141D1BB6D96935ACC947216CFA28911A6D8F4F56F2832E7B30CF03`이다. detector mode는 이 기대 hash가 없거나 실제 파일과 다르면 시작하지 않는다. ONNX checker와 PyTorch/ONNX Runtime 출력 parity 검사는 통과했다.

데이터와 학습은 다음 구성으로 다시 실행할 수 있다. 정확한 옵션·환경 버전·전체 결과와 라이선스 주의사항은 `docs/ml-detector.md`에 있다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.detection_data `
  --train-images 1000 --train-negative-images 250 `
  --validation-images 0 --validation-negative-images 0

.\.venv-detector\Scripts\python.exe -m ml_service.train_detector `
  --manifest data\detector\open-images-v7\manifest.json `
  --output-dir runs\traffic-sign-detector-v1 `
  --epochs 20 --batch-size 16 --learning-rate 0.01 `
  --device cuda --amp --seed 20260803 `
  --onnx-path traffic_sign_detector.onnx
```

적용할 때는 `ROADSCANNER_PIPELINE_MODE=detect`, `ROADSCANNER_DETECTOR_MODEL_PATH=traffic_sign_detector.onnx`, `ROADSCANNER_EXPECTED_DETECTOR_MODEL_SHA256=D1EC...CF03`을 명시한다. 기존 crop-only 회귀 검사는 `ROADSCANNER_PIPELINE_MODE=crop`으로 유지할 수 있다.

## 검증 결과

2026-08-02에 공식 GTSRB ERDA training 39,209장과 test 12,630장·정답을 내려받았다. training은 같은 물리 표지판의 연속 프레임이 섞이지 않게 track ID modulo 5로 reference 30,869장과 calibration 8,340장을 분리했다. class별 하위 0.5% similarity를 거부 기준으로 삼았고 calibration 수락률은 99.1966%였다. test는 기준 생성에 넣지 않고 서비스와 같은 판정 경로로 전체 평가했다.

- 전체 정확도: 12,505 / 12,630 = 99.0103%
- 클래스별 macro accuracy: 98.6763%
- confidence 0.85, top-1 margin 0.15, 최대 가로 종횡비 1.45, 저정보량·이웃 픽셀 상관 0.4, OOD threshold + 기본 safety margin 0.07 및 class 12 effective threshold 0.77만 적용한 프로토타입 OFF 기준: 11,363 / 12,630 수락(89.9683%), 11,343장 정답, 20장 오답, 1,267장 거부
- rejected-only 도안 프로토타입 ON 기준: 11,369 / 12,630 수락(90.0158%), 11,349장 정답, 20장 오답, 1,261장 거부, 신규 구조 6/6 정답
- 프로토타입 ON 수락 결과 정확도: 11,349 / 11,369 = 99.8241%; 프로토타입이 새로 만든 오답은 0장
- 저장소의 실제 비표지판 회귀 입력: 배경 2장은 crop 요구, 정사각형 아이콘은 OOD로 거부
- 고정 시드 stress 입력: 단색 256장과 RGB 노이즈 256장 모두 `인식 불가`
- 과거 시연 GIF의 우회전 입력: raw class 33, result ID 34, confidence 1.0
- 실제 로그인 → 업로드 → Java → Python → DB → JSP 종단간 검사: `우회전 지시`, result ID 34 확인
- 깨끗한 우선도로 도안 `data/manual-validation/positive/15.jpg`: class 12, result ID 13으로 수락
- 수동 GTSRB 지원 도안 7장: 7/7 정답 수락; 미지원 도안 `13.jpg`, `26.jpg`: 2/2 `인식 불가`
- Open Images human-verified 음성 286장: 프로토타입 신규 수락 0장

공식 ZIP은 `data/gtsrb/`에 보관하며 Git에서는 제외한다. downloader는 test images `48BA...37FA`, test GT `F94E...6D6D`, training images `D32A...290C`의 전체 SHA-256을 고정 검증하고, `download-manifest.json`에 출처 URL·다운로드 시각·바이트·해시를 함께 기록한다. OOD 기준 파일 SHA-256은 `B291EB750E33EFD59E27E09E5EA2236DE05E38624E2355EC0B42E653A3E43F4D`다.

전역 OOD safety margin은 계속 `0.07`이다. GTSRB class 12 `priority_road`만 `ROADSCANNER_OOD_THRESHOLD_OVERRIDES=12:0.77`로 effective threshold를 고정하며, 지정되지 않은 42개 클래스는 기존 `class threshold + 0.07`을 사용한다. 설정은 시작 시 class ID, 숫자 범위, 중복과 calibrated threshold 하한을 검증하고 `/health` 및 평가 결과에 노출한다. 이 정책 변경은 `ood_reference.npz`를 수정하지 않으므로 위 SHA-256도 유지된다.

## 남은 한계

- 기존 `road_scanner.h5` 자체는 여전히 이미 잘린 독일 표지판 43종 분류기다. 새 검출기는 전체 장면의 후보 위치를 찾을 뿐 국가별 의미를 확장하지 않는다.
- Open Images 검출기의 best validation AP50는 0.377702이고 독립 test가 없다. 작은 validation 74장, 특히 positive 38장만으로 세계 각국·주야·날씨·원거리 장면의 성능을 주장할 수 없다.
- 한국·중국·일본 등 다른 국가의 표지판 의미 체계를 학습하지 않았다. 해당 표지판이 검출되더라도 독일 GTSRB class로 오분류되거나 `인식 불가`가 될 수 있다.
- softmax·특징 centroid·저정보량 기준과 286장의 detector hard-negative는 오매칭을 줄이는 안전망이지만 일반적인 open-set 판별을 보장하지 않는다. 국가·환경별 독립 negative corpus 평가가 더 필요하다.
- 운영 DB의 과거 결과 번호와 원래 클래스 순서는 복구되지 않았다. 제공한 migration은 새 명시적 매핑으로 이름·설명을 맞추고 오매칭 위험이 있는 기존 참고 이미지 URL을 `none`으로 초기화하므로, 적용 전에 기존 업로드 이력과 자산 변경 영향을 검토해야 한다.
- 다음 단계는 독립 full-scene test set으로 detector threshold를 보정하고, AI Hub·ZOD·GLARE 등으로 coverage를 늘린 뒤 검출 crop을 국가별 분류기로 보내는 구조다.
