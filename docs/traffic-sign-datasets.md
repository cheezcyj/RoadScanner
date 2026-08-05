# 교통표지판 데이터셋 카탈로그

2026-08-03 기준 공식 또는 1차 배포처와 이용조건을 확인한 우선 후보다. Kaggle·Roboflow 미러의 라이선스 표시는 원 배포처의 제한을 대체하지 않는다.

| 우선 | 데이터셋 | 적용 목적 | 이용조건과 상태 |
|---:|---|---|---|
| 1 | [AI Hub 수도권](https://aihub.or.kr/aihubdata/data/view.do?dataSetSn=188) · [수도권 외](https://www.aihub.or.kr/aihubdata/data/view.do?dataSetSn=187) | 한국 full-scene 표지판 검출. 두 세트 합산 이미지 약 191만 장, 표지판 객체 약 294만 개 | 본인인증·신청 필요. R&D 이용 가능, 원데이터 재배포·판매 금지. 아직 미다운로드 |
| 2 | [Zenseact Open Dataset](https://zod.zenseact.com/) | 유럽 14개국 검출·분류, 표지판 약 44.6만 개와 156클래스 | CC BY-SA 4.0, 접근 신청 필요. 아직 미다운로드 |
| 3 | [GTSRB](https://benchmark.ini.rub.de/gtsrb_dataset.html) · [공식 ERDA](https://sid.erda.dk/public/archives/daaeac0d7ce1152aea9b61d9f1e19370/published-archive.html) | 기존 43클래스 crop 분류기 재현과 회귀 검증 | 공식 training 39,209장과 test 12,630장·정답 다운로드 및 고정 SHA-256 검증 완료 |
| 4 | [Synset Signset Germany](https://synset.de/datasets/synset-signset-ger/) · [GTSRB 전용 HF subset](https://huggingface.co/datasets/FraunhoferIOSB/Synset-Signset-Germany-GTSRB-Subset) | GTSRB 43종의 고해상도 도안 회귀와 synthetic domain 보강 | CC BY 4.0. 첫 43개 ID가 GTSRB `0..42`와 직접 일치한다. 43개 도안·출처·해시·저작자 표기 수집 완료. synthetic 학습 subset은 조사만 완료하고 아직 미다운로드 |
| 5 | [Open Images V7](https://storage.googleapis.com/openimages/web/download_v7.html) | 글로벌 generic `Traffic sign` detector 사전학습 | annotation CC BY 4.0. positive 1,038장과 human-verified hard-negative 286장 다운로드·초기 학습 완료. 이미지별 원 라이선스·출처 보존 필요 |
| 6 | [GLARE](https://github.com/NicholasCG/GLARE_Dataset) | 미국 역광 환경 검출·분류 보강 | CC BY 4.0, 41종·2,157장 |
| 7 | [Mapillary MTSD](https://www.mapillary.com/dataset/trafficsign) | 전 세계 약 10만 장과 세부 클래스 벤치마크 | [Research Use License](https://www.mapillary.com/dataset/assets/mapillary-object-dataset-research-use-license-2019.pdf). 제품 사용은 별도 계약 전 금지 |
| 8 | [TT100K](https://cg.cs.tsinghua.edu.cn/traffic-sign/) | 중국 10만 장면, bbox·mask·세부분류 | CC-BY-NC. 상업 적용은 별도 허가 필요 |
| 9 | [사우디 ArTS](https://data.mendeley.com/datasets/4tznkn45mx/1) | 중동권 24클래스 crop 분류 보강 | CC BY 4.0 |
| 10 | [일본 GeoTechnologies 표지판 데이터](https://kyodonewsprwire.jp/index.php/release/202510086746) | 일본 전국 78개 의미 클래스 | 연구·교육용 공개. 실제 패키지 README와 상업권 확인 후 사용 |

## 수집·학습 순서

1. 완료: 기존 GTSRB 모델과 공식 training/test로 전처리·클래스 순서·정확도를 확정하고, training track 단위 분할로 임시 OOD 기준을 보정한다.
2. 초기 기준선 완료: Open Images subset으로 generic 1-class detector를 학습하고 ONNX로 내보낸다. 이 단계는 표지판의 위치만 다루며 국가별 의미 분류를 확장하지 않는다.
3. 한국 detector: AI Hub 187·188을 신청해 bbox를 공통 COCO/YOLO 형식으로 변환하고 별도 test split을 고정한다.
4. 글로벌 detector 보강: ZOD와 GLARE를 라이선스별로 분리하고 Open Images와 중복·지역 편향을 확인한 뒤 결합한다.
5. 연구 전용 트랙: MTSD·TT100K는 제품 학습 데이터와 물리적으로 분리해 비교 평가한다.
6. 국가별 classifier: 일본·중국·중동 등의 crop 데이터는 공통 상위 분류와 국가별 세부 ID를 함께 가진 ontology로 변환한다.

각 파일은 최소한 `origin_url`, 데이터셋 버전, 검색·다운로드 일자, SHA-256, 원 라이선스, 수락한 약관 사본, 국가, 원 class ID, 변환 이력을 manifest에 보존한다. 영상 연속 프레임이나 같은 물리 표지판이 train과 validation에 동시에 들어가지 않도록 track 단위로 분리한다.

## GTSRB 43종 고해상도 도안

Fraunhofer IOSB·IPA와 KIT가 공개한 Synset Signset Germany는 전체 211종 중 첫 43개 class ID가 GTSRB와 직접 일치한다고 명시한다. 공식 클래스 표에 쓰인 도안 PNG `0.png`부터 `42.png`까지를 `ml_service/class_map.json`의 key로 이름 붙여 수동 회귀 자료로 수집했다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.download_gtsrb_design_icons
```

결과는 Git 제외 경로 `data/manual-validation/positive/gtsrb-design`에 생성된다.

- `original-transparent`: 원격 PNG byte를 변경 없이 보존한 43장. 모두 RGBA이며 27장은 1024×1024, 삼각형 계열을 포함한 16장은 1024×899다.
- `upload-ready-transparent`: 사용자 업로드와 회귀 검증에 권장하는 43장이다. 원본 종횡비를 유지하고 필요한 경우 RGBA 전체에 LANCZOS 축소만 적용한 뒤 alpha 합성 없이 1024×1024 투명 RGBA canvas 중앙에 복사한다. 축소하지 않은 foreground RGBA는 원본과 byte 단위로 같고, 축소 시에는 LANCZOS만 foreground/alpha에 영향을 준다. 완전 투명 픽셀의 hidden RGB는 `(255,255,255)`로 정규화되어 RGB-only 변환에서도 검은 배경이 생기지 않는다.
- `upload-ready-white`: 위 투명 정규화본을 흰색에 alpha 합성한 RGB 호환 파생본 43장이다.
- `manifest.json`: class/result ID, 원본 URL, 원본·파생 SHA-256, 크기, 모드와 변환 이력을 기록한다.
- `ATTRIBUTION.md`: 데이터셋명, 제작자 7명, 저작권, 원문·CC BY 4.0 링크, 변경 내용과 면책을 기록한다.

검증 결과 세 variant는 각각 43개다. `upload-ready-transparent`는 전부 PNG·RGBA·1024×1024이고 alpha, 투명 픽셀, hidden RGB, 파일명·해시·class ID가 manifest와 일치한다. `upload-ready-white`는 전부 PNG·RGB·1024×1024다. 이 43장은 회귀 test 기준이며 같은 파일을 임계값 보정이나 학습에 다시 사용해 성능을 주장해서는 안 된다. synthetic 학습 보강은 별도의 train/validation split을 내려받아 공식 GTSRB test와 이 도안 회귀 세트에 독립적으로 평가한다.

### 별도 synthetic 학습 subset

대용량 RADAR 원본 대신 Fraunhofer IOSB가 공개한 Hugging Face 전용 저장소 `FraunhoferIOSB/Synset-Signset-Germany-GTSRB-Subset`를 우선 후보로 확인했다. 조사한 고정 revision은 `9d47eb87b975737551f7b16c401e118650123b47`이며 `OGRE`와 `Cycles` 렌더러가 각각 43 class × 500장을 제공한다. 렌더러별 split은 class당 train 400장·validation 100장, 전체 train 17,200장·validation 4,300장이다.

전체 Parquet에는 image 외에 mask·segmentation도 들어 있어 OGRE만 약 1.69GB다. Hugging Face dataset viewer의 `/rows` API에서 `row.image.src`만 받으면 OGRE train+validation은 약 125MB로 추정된다. 1차 학습 보강은 OGRE image-only를 권장하며 다음 계약을 지키는 downloader를 먼저 구현한다.

- 현재 Hub revision이 위 고정 SHA와 다르면 중단한다.
- `label`, `class_name`, `sample_idx`, 원 row index, 이미지 크기·byte·SHA-256을 manifest에 기록한다.
- class별 train 400장·validation 100장과 `(label, sample_idx)` split 교집합 0건을 검증한다.
- OGRE와 Cycles를 나중에 합칠 때도 같은 `(label, sample_idx)`를 같은 split에 둔다.
- mask·segmentation은 이 분류 학습 단계에서 내려받지 않는다.
- 현재 43개 고해상도 도안은 학습·OOD centroid·threshold 보정·augmentation seed에 넣지 않고 최종 도안 호환성 회귀로만 유지한다.

viewer가 주는 이미지는 캐시된 JPEG와 만료 가능한 서명 URL이므로 source revision과 변환 사실을 attribution에 기록해야 한다. 실제 다운로드와 재학습은 아직 실행하지 않았으며, 기존 GTSRB real-domain 성능을 공식 test에서 비교한 뒤에만 새 모델을 적용한다.

## 현재 Open Images detector subset

`ml_service/detection_data.py`는 공식 class description, bbox annotation, image metadata와 human image-level annotation을 직접 읽는다. `Traffic sign`(`/m/01mqdt`)과 `Stop sign`(`/m/02pv19`) bbox를 generic `traffic_sign` 한 클래스로 합치며 `IsGroupOf`, `IsDepiction`, `IsInside`는 제외한다. negative는 `Traffic sign`, `Confidence=0`으로 사람이 부재를 확인한 이미지 중 bbox positive와 겹치지 않는 항목만 사용한다.

| split | eligible positive | 선택 positive | eligible negative | 선택 negative | box |
|---|---:|---:|---:|---:|---:|
| train | 3,135 | 1,000 | 1,461 | 250 | 2,019 |
| validation | 38 | 38 | 36 | 36 | 59 |

train negative 후보를 만들 때 bbox positive와 겹친 4개 ID를 제외했다. 선택 이미지마다 원 URL, 이미지별 라이선스·attribution, byte, SHA-256, 크기와 변환 라벨을 manifest에 기록한다. 이번 학습 manifest SHA-256은 `E110064123FEF5FA001C148A67ADADBE03A234205D30A911693FDCB8E0DAEE71`이다.

```powershell
.\.venv\Scripts\python.exe -m ml_service.detection_data `
  --output-dir data\detector\open-images-v7 `
  --train-images 1000 --train-negative-images 250 `
  --validation-images 0 --validation-negative-images 0 `
  --seed roadscanner-open-images-v7-traffic-sign-v1
```

이 subset으로 학습한 SSDLite320 기준선은 validation AP50 `0.377702`였고 ONNX artifact SHA-256은 `D1EC740200141D1BB6D96935ACC947216CFA28911A6D8F4F56F2832E7B30CF03`이다. validation positive가 38장뿐이고 같은 split으로 best epoch를 선택했으므로 독립 성능이나 전 세계 일반화 결과가 아니다. 데이터·학습·추론 계약과 재현 명령은 `docs/ml-detector.md`에 정리되어 있다.

Open Images detector가 찾은 crop의 현재 후단은 독일 GTSRB 43종 분류기다. 그러므로 한국·중국·일본 표지판도 위치는 찾을 수 있을 가능성이 있지만 의미를 올바르게 분류한다고 간주해서는 안 된다. 국가별 classifier와 ontology가 준비될 때까지 지원 범위는 `generic 검출 + GTSRB 43종 분류`다.
