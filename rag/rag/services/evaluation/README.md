# RAGAS 평가 파이프라인

RAG 시스템의 성능을 RAGAS 프레임워크로 체계적으로 평가하는 자동화 파이프라인입니다.

## 📋 개요

이 파이프라인은 다음 작업을 자동으로 수행합니다:

1. **데이터 수집**: RAG 시스템에 282개 환자 케이스 입력 및 응답 수집
2. **RAGAS 평가**: 5개 메트릭(충실성, 관련성, 정밀도, 재현율, 정확도) 계산
3. **결과 분석**: 응급도별, 난이도별, 카테고리별 성능 분석
4. **시각화**: 4종 차트 자동 생성 (레이더, 응급도, 카테고리, 난이도)
5. **리포트 생성**: 발표용 마크다운 리포트 자동 작성

## 🏗️ 디렉토리 구조

```
evaluation/
├── __init__.py                # 패키지 초기화
├── config.py                  # 설정 파일
├── data_collector.py          # RAG 호출 및 데이터 수집
├── ragas_evaluator.py         # RAGAS 메트릭 계산
├── analyzer.py                # 결과 분석
├── visualizer.py              # 차트 생성
├── report_generator.py        # 리포트 생성
├── run_evaluation.py          # 전체 파이프라인 실행 (메인)
├── README.md                  # 이 파일
└── results/                   # 평가 결과 저장
    ├── raw/                   # JSON 결과
    ├── charts/                # 생성된 차트
    └── reports/               # 발표 리포트
```

## 🚀 빠른 시작

### 1. 환경 설정

```bash
# 가상환경 활성화
cd rag
source venv/bin/activate  # Windows: venv\Scripts\activate

# RAGAS 패키지 설치
pip install ragas datasets matplotlib seaborn tqdm

# 또는 requirements.txt 전체 재설치
pip install -r requirements.txt
```

### 2. 환경변수 설정

```bash
# CLOVA Studio API 키 설정 (필수)
export CLOVA_STUDIO_API_KEY="your-clova-api-key"

# Windows
set CLOVA_STUDIO_API_KEY=your-clova-api-key

# OpenAI API 키 설정 (선택사항 - 폴백용)
# CLOVA Studio를 우선 사용하며, CLOVA 사용 불가 시에만 필요
export OPENAI_API_KEY="your-openai-api-key"
```

### 3. RAG 서비스 시작

```bash
# 터미널 1: RAG 서비스 실행
cd rag
python main.py
```

### 4. 평가 실행

```bash
# 터미널 2: 평가 파이프라인 실행

# 샘플 데이터로 빠른 테스트 (10개 케이스, ~8분)
python evaluation/run_evaluation.py --sample

# 전체 데이터로 평가 (282개 케이스, ~2시간)
python evaluation/run_evaluation.py
```

## 📊 출력 결과

### 1. 원본 데이터 (`results/raw/`)
- `dataset_complete_YYYYMMDD_HHMMSS.json`: RAG 응답이 포함된 완성 데이터셋
- `ragas_results_YYYYMMDD_HHMMSS.json`: RAGAS 평가 결과 (케이스별 점수)
- `errors_YYYYMMDD_HHMMSS.json`: 에러 로그 (있는 경우)

### 2. 차트 (`results/charts/`)
- `YYYYMMDD_HHMMSS_radar_chart.png`: 5개 메트릭 레이더 차트
- `YYYYMMDD_HHMMSS_ktas_performance.png`: 응급도별 성능 바 차트
- `YYYYMMDD_HHMMSS_category_heatmap.png`: 신체계통별 히트맵
- `YYYYMMDD_HHMMSS_difficulty_performance.png`: 난이도별 성능 차트

### 3. 리포트 (`results/reports/`)
- `report_YYYYMMDD_HHMMSS.md`: 발표용 마크다운 리포트
  - 전체 평가 결과
  - 응급도/난이도별 분석
  - 우수/약점 케이스
  - 발표 포인트 제안

## ⚠️ 사전 준비 사항

### 필수: RAG 서비스 수정

**현재 문제**: RAG 서비스가 검색된 문서(contexts)를 반환하지 않음

**해결 방법**: `rag/services/rag_retrieval_service.py` 수정 필요

자세한 내용은 `RAG_SERVICE_MODIFICATION_GUIDE.md` 참조

## 📖 상세 사용법

### 옵션 1: 샘플 데이터로 테스트

```bash
python evaluation/run_evaluation.py --sample
```

- 10개 케이스만 평가
- 소요 시간: 약 8분
- 용도: 파이프라인 테스트, 빠른 검증

### 옵션 2: 전체 데이터로 평가

```bash
python evaluation/run_evaluation.py
```

- 282개 전체 케이스 평가
- 소요 시간: 약 2시간
- 용도: 최종 평가, 발표 자료

### 옵션 3: 특정 카테고리만 평가

```bash
python evaluation/run_evaluation.py --dataset ../data_ver2/ragas_by_category/15세이상_심혈관_의미문장추가.json
```

- 특정 카테고리 10개 케이스만 평가
- 용도: 카테고리별 성능 분석

### 옵션 4: 데이터 수집 스킵

```bash
# 이미 수집된 데이터로 평가만 재실행
python evaluation/run_evaluation.py --skip-collection --dataset results/raw/dataset_complete_20251110_160000.json
```

## 🔧 설정 변경

`config.py` 파일에서 다음 항목을 변경할 수 있습니다:

```python
# RAG API 엔드포인트
RAG_API_URL = "http://localhost:8080/api/clovax/chat/completions/HCX-005"

# RAG 설정
RAG_CONFIG = {
    "useRAG": True,
    "ragTopK": 3,           # 검색 문서 수
    "ragThreshold": 0.1,    # 유사도 임계값
    "temperature": 0.7
}

# 평가 실행 설정
EVALUATION_CONFIG = {
    "batch_size": 10,       # 배치 크기
    "timeout": 60,          # 타임아웃 (초)
    "retry_attempts": 3     # 재시도 횟수
}
```

## 📈 RAGAS 메트릭 설명

| 메트릭 | 설명 | 필요 데이터 |
|--------|------|-------------|
| **Faithfulness** | 답변이 검색된 문서에 근거하는가? (환각 검증) | answer + contexts |
| **Answer Relevancy** | 답변이 질문과 얼마나 관련있는가? | question + answer |
| **Context Precision** | 검색된 문서가 얼마나 정확한가? | question + contexts + ground_truth |
| **Context Recall** | 필요한 문서를 모두 찾았는가? | contexts + ground_truth |
| **Answer Correctness** | 답변이 정답과 얼마나 일치하는가? | answer + ground_truth |

## 🐛 문제 해결

### 에러: "CLOVA_STUDIO_API_KEY not found"

**원인**: RAGAS가 메트릭 계산에 사용하는 CLOVA Studio API 키가 없음

**해결**:
```bash
# CLOVA Studio API 키 설정 (필수)
export CLOVA_STUDIO_API_KEY="your-api-key"

# 또는 폴백으로 OpenAI 사용
export OPENAI_API_KEY="sk-..."
```

### 에러: "contexts is empty"

**원인**: RAG 서비스가 contexts를 반환하지 않음

**해결**: `RAG_SERVICE_MODIFICATION_GUIDE.md` 참조하여 RAG 서비스 수정

### 에러: "Connection refused"

**원인**: RAG 서비스가 실행 중이지 않음

**해결**:
```bash
# RAG 서비스 시작
cd rag
python main.py
```

### 한글 폰트 깨짐 (차트)

**Windows**: `config.py`에서 `Malgun Gothic` 확인
**macOS**: `AppleGothic` 또는 다른 한글 폰트 설정
**Linux**: 한글 폰트 설치 필요

## 📝 개별 모듈 실행

각 모듈은 독립적으로 실행 가능합니다:

```bash
# 데이터 수집만
python evaluation/data_collector.py

# RAGAS 평가만
python evaluation/ragas_evaluator.py path/to/dataset_complete.json

# 결과 분석만
python evaluation/analyzer.py path/to/ragas_results.json path/to/dataset.json

# 시각화만
python evaluation/visualizer.py path/to/ragas_results.json path/to/dataset.json

# 리포트 생성만
python evaluation/report_generator.py path/to/ragas_results.json path/to/dataset.json
```

## 💡 팁

1. **처음 실행**: 반드시 `--sample` 옵션으로 테스트 먼저
2. **API 비용**: RAGAS는 많은 LLM 호출이 필요하므로 비용 주의
3. **시간 계획**: 전체 평가(282개)는 2시간 이상 소요
4. **결과 백업**: 평가 결과는 자동으로 타임스탬프와 함께 저장됨

## 🔗 관련 문서

- `RAGAS_PIPELINE_DESIGN.md`: 전체 설계 개요
- `RAG_SERVICE_MODIFICATION_GUIDE.md`: RAG 서비스 수정 가이드
- `../data_ver2/RAGAS_README.md`: 평가 데이터셋 가이드
- RAGAS 공식 문서: https://docs.ragas.io/

## 📧 문의

프로젝트 팀에 문의하세요.

---

**버전**: v1.0
**작성일**: 2025-11-10
