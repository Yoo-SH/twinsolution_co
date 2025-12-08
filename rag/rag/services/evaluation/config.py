"""RAGAS 평가 설정

이 파일은 core.config에서 모든 RAGAS 관련 설정을 re-export합니다.
실제 설정은 rag/core/config.py에 정의되어 있습니다.
"""

# core.config에서 모든 RAGAS 설정 가져오기
from core.config import (
    # OpenAI API 키
    settings,

    # 경로 설정
    PROJECT_ROOT,
    INPUT_DATA_DIR,
    OUTPUT_DATA_DIR,

    # 데이터셋 경로
    DATASET_PATH,
    SAMPLE_DATASET_PATH,
    CATEGORY_DIR,

    # RAG 설정
    RAG_API_URL,
    RAG_CONFIG,

    # RAGAS 메트릭
    METRICS,

    # 결과 저장 경로
    COLLECT_DIR,
    EVALUATE_DIR,
    ANALYZE_DIR,
    CHARTS_DIR,
    REPORTS_DIR,
    RAW_RESULTS_DIR,

    # LLM 설정
    RAGAS_LLM_CONFIG,

    # 평가 설정
    EVALUATION_CONFIG,

    # 로깅 설정
    LOGGING_CONFIG
)

# 하위 호환성을 위한 추가 export
__all__ = [
    'settings',
    'PROJECT_ROOT',
    'INPUT_DATA_DIR',
    'OUTPUT_DATA_DIR',
    'DATASET_PATH',
    'SAMPLE_DATASET_PATH',
    'CATEGORY_DIR',
    'RAG_API_URL',
    'RAG_CONFIG',
    'METRICS',
    'COLLECT_DIR',
    'EVALUATE_DIR',
    'ANALYZE_DIR',
    'CHARTS_DIR',
    'REPORTS_DIR',
    'RAW_RESULTS_DIR',
    'RAGAS_LLM_CONFIG',
    'EVALUATION_CONFIG',
    'LOGGING_CONFIG'
]
