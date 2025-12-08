from pydantic import BaseModel
from typing import Optional, List
import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()


class Settings(BaseModel):
    # CLOVA Studio API 키 및 기본 URL
    CLOVA_STUDIO_API_KEY: str = os.getenv("CLOVA_STUDIO_API_KEY", "")
    CLOVA_STUDIO_BASE_URL: str = os.getenv("CLOVA_STUDIO_BASE_URL", "https://clovastudio.stream.ntruss.com")

    # OpenAI API 키 (RAGAS 평가용 - 폴백 옵션)
    # RAGAS 평가시 CLOVA Studio를 우선 사용하며, CLOVA 사용 불가 시에만 OpenAI 사용
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")

    # 서버 설정
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    RELOAD: bool = os.getenv("RELOAD", "false").lower() == "true"
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"

    # CORS 설정 (테스트용 - 모든 IP 허용)
    CORS_ORIGINS: List[str] = ["*"]

    # 로깅 설정
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    # API 버전 및 프로젝트 설정
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "CLOVAX API"
    VERSION: str = "1.0.0"

    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()

# ============================================
# RAGAS 평가 시스템 설정
# ============================================

from ragas.metrics import (
    faithfulness,
    answer_relevancy,
    context_precision,
    context_recall,
    answer_correctness
)

# 프로젝트 루트 경로
PROJECT_ROOT = Path(__file__).parent.parent  # rag/
INPUT_DATA_DIR = PROJECT_ROOT.parent / "data_ver2"  # triage/data_ver2 (입력용)
OUTPUT_DATA_DIR = PROJECT_ROOT / "data"  # rag/data (출력용 - RAGAS 결과)

# 평가 대상 데이터셋 (입력)
DATASET_PATH = str(INPUT_DATA_DIR / "ragas_evaluation_dataset_all.json")
SAMPLE_DATASET_PATH = str(INPUT_DATA_DIR / "ragas_evaluation_dataset_sample.json")
CATEGORY_DIR = str(INPUT_DATA_DIR / "ragas_by_category")

# RAG 서비스 엔드포인트
# Triage Server를 통한 CLOVAX 호출
RAG_API_URL = os.getenv("RAG_API_URL", "http://localhost:8080/api/clovax/chat/completions/HCX-005")

# RAG 설정
RAG_CONFIG = {
    "useRAG": True,
    "ragTopK": 3,
    "ragThreshold": 0.1,
    "temperature": 0.7,
    "maxTokens": 1000
}

# RAGAS 메트릭 설정
METRICS = [
    faithfulness,        # 답변이 contexts에 얼마나 충실한가 (환각 검증)
    answer_relevancy,    # 답변이 질문과 얼마나 관련있는가
    context_precision,   # 제공된 contexts가 얼마나 정확한가
    context_recall,      # ground truth 답변에 필요한 contexts가 모두 포함되었는가
    answer_correctness   # 답변이 ground truth와 얼마나 일치하는가
]

# 결과 저장 경로 (파이프라인 단계별로 분리)
COLLECT_DIR = OUTPUT_DATA_DIR / "collect"      # 1단계: 데이터 수집 (dataset_complete, errors)
EVALUATE_DIR = OUTPUT_DATA_DIR / "evaluate"    # 2단계: RAGAS 평가 (ragas_results)
ANALYZE_DIR = OUTPUT_DATA_DIR / "analyze"      # 3단계: 결과 분석 (analysis_results)
CHARTS_DIR = OUTPUT_DATA_DIR / "charts"        # 4단계: 차트 생성 (PNG 파일들)
REPORTS_DIR = OUTPUT_DATA_DIR / "reports"      # 5단계: 리포트 생성 (Markdown 파일들)

# 하위 호환성을 위한 별칭 (기존 코드에서 RAW_RESULTS_DIR 사용하는 경우)
RAW_RESULTS_DIR = EVALUATE_DIR

# 디렉토리 생성
for dir_path in [COLLECT_DIR, EVALUATE_DIR, ANALYZE_DIR, CHARTS_DIR, REPORTS_DIR]:
    dir_path.mkdir(parents=True, exist_ok=True)

# RAGAS LLM 설정 (메트릭 계산용)
# CLOVA Studio를 우선 사용하며, 실패 시 OpenAI로 폴백
RAGAS_LLM_CONFIG = {
    "provider": "clova-studio",  # "clova-studio" (우선) or "openai" (폴백)
    "model": "HCX-DASH-002",  # CLOVA: HCX-DASH-002 (빠름) 또는 HCX-005 (고성능)
    "fallback_model": "gpt-3.5-turbo",  # OpenAI 폴백 모델
    "api_key": settings.CLOVA_STUDIO_API_KEY,  # CLOVA Studio API 키
    "fallback_api_key": settings.OPENAI_API_KEY,  # OpenAI 폴백 API 키
    "temperature": 0.0  # 평가의 일관성을 위해 0
}

# 평가 실행 설정
EVALUATION_CONFIG = {
    "batch_size": 10,  # 한 번에 처리할 케이스 수
    "timeout": 90,  # 각 RAG 호출 타임아웃 (초) - RAG(25초) + Chat(40초) = 65초 정도 소요
    "retry_attempts": 1,  # 재시도 비활성화 (1 = 재시도 없이 1번만 시도)
    "retry_delay": 5  # 재시도 간격 (초)
}

# 로깅 설정
LOGGING_CONFIG = {
    "level": "INFO",
    "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    "file": str(OUTPUT_DATA_DIR / "evaluation.log")
}
