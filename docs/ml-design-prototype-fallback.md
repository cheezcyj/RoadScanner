# 표지 도안 프로토타입 fallback

## 목적

복구된 `road_scanner.h5`는 작은 실사 GTSRB 사진으로 학습된 43종 폐쇄형
분류기다. 의미가 같은 고해상도 벡터 도안도 실사 학습 분포와 다르면 OOD로
거부될 수 있다. RoadScanner는 이 간극을 좁히기 위해 43종 정식 도안의
512차원 특징을 고정 프로토타입으로 보관한다.

이 기능은 기존 분류기를 대체하지 않는다. confidence, raw margin, 시각 정보,
OOD를 포함한 기존 정책이 먼저 실행되며 기존 정책이 거부한 입력에만
프로토타입 fallback을 시도한다. 기존에 수락된 결과는 변경하지 않는다.
`image_too_small`, `sign_crop_required`, `low_visual_information`,
`high_frequency_noise` 같은 구조적 안전 거부도 우회하지 않는다.

## 판정 조건

프로토타입은 `road_scanner.h5`의 `batch_normalization_2` 출력 512차원을
L2 정규화한 뒤 cosine similarity로 비교한다.

- 일반 클래스: top-1 similarity `0.80` 이상
- top-1과 top-2 similarity 차이: `0.15` 이상
- class 33 `turn_right_ahead`: similarity `0.50` 이상이며 기존 CNN top-1도
  class 33일 때만 허용
- 모든 조건을 통과한 경우에만 기존 `인식 불가`를 해당 43종 결과로 바꾼다.

class 33의 별도 조건은 수동 우회전 도안 similarity `0.510028`과 검증 음성의
class 33 최대 similarity `0.266754` 사이의 관측 간극을 이용한다. 이 수치는
일반적인 open-set 안전성을 증명하지 않으므로 다른 클래스나 문턱에 그대로
확대 적용하면 안 된다.

## 출처와 재현

프로토타입 입력은 Synset Signset Germany의 GTSRB ID `0..42` 도안이다.
원본 URL과 SHA-256은
`ml_service/gtsrb_design_icon_sources.lock.json`에 고정되어 있다. 권장 입력은
`upload-ready-transparent` 1024×1024 RGBA이며 완전 투명 픽셀의 숨은 RGB도
흰색으로 정규화되어 있다. 생성기는 이 이미지를 RGB로 변환한 픽셀이
`upload-ready-white` 호환본과 완전히 같은지 43장 모두 검증한다.

다음 순서로 원본 다운로드부터 아티팩트까지 재생성한다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.download_gtsrb_design_icons
.\.venv\Scripts\python.exe -m ml_service.generate_design_prototypes
```

추적되는 산출물은 다음과 같다.

- `ml_service/design_prototypes.npz`: `(43, 512)` float32 L2 정규화 행렬과
  순서가 고정된 class ID
- `ml_service/design_prototypes.json`: 모델·class map·source lock·입력 도안
  43장의 SHA-256, 선택 variant, 전처리 및 shape
- 아티팩트 SHA-256:
  `5AA43019CF6A15C84A04AF58C962736DCDDB4C5D0BC6FE557EEEB25197538953`
- 메타데이터 SHA-256:
  `8610646A04986AD84D7DAA9FB985660D507BA216EC53EF4F00186C0CB48A0F06`

NPZ ZIP 항목의 시간과 권한도 고정하고 메타데이터에 TensorFlow·NumPy·Pillow
생성 버전을 남긴다. 같은 잠긴 입력과 `requirements.txt` 환경에서는 바이트가
동일하게 생성된다. 서비스는 시작할 때 아티팩트와 메타데이터 해시, shape,
class 순서, L2 norm, 모델·class map 해시 및 source-lock provenance를 모두
검증하고 불일치하면 시작하지 않는다.

## 환경설정과 진단 API

기본 설정은 `.env.example`에 있다.

- `ROADSCANNER_DESIGN_PROTOTYPE_ENABLED=true`
- `ROADSCANNER_DESIGN_PROTOTYPE_PATH`
- `ROADSCANNER_DESIGN_PROTOTYPE_METADATA_PATH`
- `ROADSCANNER_EXPECTED_DESIGN_PROTOTYPE_SHA256`
- `ROADSCANNER_EXPECTED_DESIGN_PROTOTYPE_METADATA_SHA256`
- `ROADSCANNER_DESIGN_PROTOTYPE_MIN_SIMILARITY=0.80`
- `ROADSCANNER_DESIGN_PROTOTYPE_MIN_MARGIN=0.15`
- `ROADSCANNER_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES=33:0.50`
- `ROADSCANNER_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES=33`

`GET /health`는 활성화 여부, 두 아티팩트의 실제 SHA-256과 검증 상태,
source-lock SHA-256, 선택 variant, shape, feature layer와 모든 문턱을 노출한다.

`POST /predict`의 기존 plain-text result ID 계약은 바뀌지 않는다.
`POST /predict/debug`의 기존 필드도 유지하며 다음 진단 필드만 추가한다.

- `prediction_source`: `recovered_cnn`, `canonical_design_prototype`, `rejected`
- `raw_class_id`: 기존 CNN top-1
- `base_reason`: fallback 전 기존 정책의 결과
- `prototype_class_id`, `prototype_similarity`, `prototype_margin`,
  `prototype_threshold`

fallback 결과에서도 기존 `confidence`, `margin`, `top3`, OOD 필드는 원래 CNN
판정의 진단값으로 유지한다. 최종 결과를 선택한 근거는 새 prototype 필드로
구분한다.

## 회귀 결과와 한계

2026-08-03 고정 아티팩트와 운영 판정 경로에서 확인한 결과다.

- 정식 도안 43장: 기존 18장 수락, fallback 신규 25/25 정답, 최종 43/43
- `manual-validation/positive` 지원 7장: 기존 5장 수락, fallback 신규 2/2
  정답, 최종 7/7
- 미지원 도안 `13.jpg`, `26.jpg`: 신규 수락 0
- Open Images human-verified 음성 286장: prototype 신규 후보 수락 0
- 공식 GTSRB test 12,630장: prototype 신규 수락 6/6 정답, 신규 오답 0
- 회전·JPEG·크기·여백 변형 430장: 신규 수락 96/96 정답, 신규 오답 0

### 릴리스 필수 회귀 게이트

위 수동 도안과 Open Images 286장은 `data/` 아래의 Git 비추적 데이터이므로 일반
체크아웃에서는 회귀 테스트가 `skip`될 수 있다. 프로토타입 아티팩트·문턱·전처리·
모델을 바꾸는 릴리스에서는 해당 corpus를 내려받거나 재생성한 뒤 아래 테스트가
반드시 실제로 실행되어 통과해야 한다. `skipped` 결과는 통과로 인정하지 않는다.

```powershell
.\.venv\Scripts\python.exe -m pytest -q `
  ml_service/tests/test_model_regression.py::test_rejected_only_design_prototype_regression_corpus
```

릴리스 기대값은 정식 도안 43/43, 수동 지원 도안 7/7, 미지원 도안 신규 수락 0/2,
Open Images human-verified 음성의 프로토타입 신규 수락 0/286이다. 공식 GTSRB도
`evaluate_gtsrb`를 프로토타입 ON/OFF로 각각 실행해 신규 구조가 6/6 정답이고 신규
오답이 0인지 확인한다. OFF 비교는 `--no-design-prototype`으로 실행한다.

43개 원 도안은 프로토타입 자체의 입력이며 이 보조 경로의 조건을 정할 때도
사용했다. 따라서 `43/43`은 독립 정확도나 일반화 성능이 아니라, 고정한 도안
43개가 서비스의 실제 detector/crop 경로에서도 깨지지 않는다는 호환성 회귀
결과다. 일반화 근거는 별도로 분리한 공식 GTSRB test와 Open Images 음성,
향후 Synset synthetic validation에서 평가해야 한다.

Open Images train 음성에는 기존 CNN/OOD 정책 자체의 오수락 1장이 남아 있다.
프로토타입이 새로 만든 오수락은 아니며 별도 hard-negative 보강 대상이다.

도안이 화면의 40~55%로 작아져 여백이 매우 커지면 단일 프로토타입의
coverage가 낮다. 실제 장면에서는 detector가 표지판을 먼저 정사각형으로
잘라 주어야 하며, 추가 확대가 필요하면 문턱을 낮추기보다 검증된 다중-scale
프로토타입이나 검출 학습 데이터를 추가해야 한다.
