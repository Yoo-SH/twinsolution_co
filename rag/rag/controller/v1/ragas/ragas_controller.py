"""RAGAS 평가 시스템 API 컨트롤러"""

from fastapi import APIRouter, HTTPException, UploadFile, File, Form, Query
from fastapi.responses import JSONResponse, FileResponse
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
import logging
import os
import json
import tempfile
from datetime import datetime
from pathlib import Path
from io import BytesIO

# RAGAS Evaluation Services
import sys
sys.path.append(str(Path(__file__).parent.parent.parent.parent / "services" / "evaluation"))

from services.evaluation.data_collector import DataCollector
from services.evaluation.ragas_evaluator import RagasEvaluator
from services.evaluation.analyzer import ResultAnalyzer
from services.evaluation.visualizer import Visualizer
from services.evaluation.report_generator import ReportGenerator
from services.evaluation.config import (
    RAG_API_URL,
    RAG_CONFIG,
    EVALUATION_CONFIG,
    RAGAS_LLM_CONFIG,
    METRICS,
    COLLECT_DIR,
    EVALUATE_DIR,
    ANALYZE_DIR,
    CHARTS_DIR,
    REPORTS_DIR,
    RAW_RESULTS_DIR  # Backward compatibility alias for EVALUATE_DIR
)

logger = logging.getLogger(__name__)
router = APIRouter()

# =============================================================================
# Pydantic 모델 (Request/Response DTOs)
# =============================================================================

class RagasCollectRequest(BaseModel):
    """데이터 수집 요청"""
    rag_api_url: Optional[str] = Field(None, description="RAG API URL (기본값: config에서 사용)")
    use_rag: bool = Field(True, description="RAG 사용 여부")
    rag_top_k: int = Field(3, description="RAG 검색 문서 개수", ge=1, le=10)
    rag_threshold: float = Field(0.1, description="RAG 유사도 임계값", ge=0.0, le=1.0)
    temperature: float = Field(0.7, description="LLM Temperature", ge=0.0, le=2.0)
    max_tokens: int = Field(1000, description="최대 토큰 수", ge=100, le=4096)
    retry_attempts: int = Field(3, description="재시도 횟수", ge=1, le=10)
    retry_delay: int = Field(5, description="재시도 대기 시간 (초)", ge=1, le=60)
    timeout: int = Field(60, description="요청 타임아웃 (초)", ge=10, le=300)

    class Config:
        json_schema_extra = {
            "example": {
                "rag_api_url": "http://localhost:8080/api/clovax/chat/completions/HCX-005",
                "use_rag": True,
                "rag_top_k": 3,
                "rag_threshold": 0.1,
                "temperature": 0.7,
                "max_tokens": 1000,
                "retry_attempts": 3,
                "retry_delay": 5,
                "timeout": 60
            }
        }

class RagasCollectResponse(BaseModel):
    """데이터 수집 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    output_path: str = Field(..., description="완성된 데이터셋 저장 경로")
    total_cases: int = Field(..., description="총 케이스 수")
    success_count: int = Field(..., description="성공한 케이스 수")
    error_count: int = Field(..., description="실패한 케이스 수")
    error_path: Optional[str] = Field(None, description="에러 로그 저장 경로")

class RagasEvaluateRequest(BaseModel):
    """RAGAS 평가 요청"""
    dataset_path: str = Field("", description="평가할 데이터셋 경로 (full-pipeline에서는 자동 설정)")
    llm_provider: str = Field("openai", description="LLM 제공자 (openai/azure/anthropic)")
    llm_model: str = Field("gpt-3.5-turbo", description="LLM 모델명")
    llm_temperature: float = Field(0.0, description="LLM Temperature (평가용)", ge=0.0, le=1.0)

    class Config:
        json_schema_extra = {
            "example": {
                "dataset_path": "C:/path/to/dataset_complete_20251111_133647.json",
                "llm_provider": "openai",
                "llm_model": "gpt-3.5-turbo",
                "llm_temperature": 0.0
            }
        }

class RagasEvaluateResponse(BaseModel):
    """RAGAS 평가 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    results_path: str = Field(..., description="평가 결과 저장 경로")
    metrics: Dict[str, float] = Field(..., description="평가 메트릭 점수")
    average_score: float = Field(..., description="평균 점수")
    total_cases: int = Field(..., description="총 케이스 수")

class RagasAnalyzeRequest(BaseModel):
    """결과 분석 요청"""
    results_path: str = Field(..., description="RAGAS 평가 결과 파일 경로")
    dataset_path: str = Field(..., description="원본 데이터셋 경로")
    weak_case_threshold: float = Field(0.7, description="약점 케이스 임계값", ge=0.0, le=1.0)

class RagasAnalyzeResponse(BaseModel):
    """결과 분석 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    analysis_path: str = Field(..., description="분석 결과 저장 경로")
    ktas_performance: Dict[str, Dict[str, Optional[float]]] = Field(..., description="응급도별 성능 (null은 데이터 없음)")
    difficulty_performance: Dict[str, Dict[str, Optional[float]]] = Field(..., description="난이도별 성능 (null은 데이터 없음)")
    category_performance: Dict[str, Dict[str, Optional[float]]] = Field(..., description="카테고리별 성능 (null은 데이터 없음)")
    weak_cases_count: int = Field(..., description="개선 필요 케이스 수")
    weak_cases: List[Dict[str, Any]] = Field(..., description="개선 필요 케이스 목록")

class RagasVisualizeRequest(BaseModel):
    """차트 생성 요청"""
    results_path: str = Field(..., description="RAGAS 평가 결과 파일 경로")
    dataset_path: str = Field(..., description="원본 데이터셋 경로")
    output_prefix: Optional[str] = Field(None, description="출력 파일 접두사")

class RagasVisualizeResponse(BaseModel):
    """차트 생성 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    chart_paths: Dict[str, str] = Field(..., description="생성된 차트 파일 경로")

class RagasReportRequest(BaseModel):
    """리포트 생성 요청"""
    results_path: str = Field(..., description="RAGAS 평가 결과 파일 경로")
    dataset_path: str = Field(..., description="원본 데이터셋 경로")
    chart_paths: Dict[str, str] = Field(..., description="차트 파일 경로")
    output_filename: Optional[str] = Field(None, description="출력 파일명")

class RagasReportResponse(BaseModel):
    """리포트 생성 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    report_path: str = Field(..., description="리포트 파일 경로")

class RagasFullPipelineRequest(BaseModel):
    """전체 파이프라인 요청"""
    collect_config: RagasCollectRequest = Field(default_factory=RagasCollectRequest, description="데이터 수집 설정")
    evaluate_config: RagasEvaluateRequest = Field(default_factory=RagasEvaluateRequest, description="평가 설정")
    weak_case_threshold: float = Field(0.7, description="약점 케이스 임계값", ge=0.0, le=1.0)

class RagasFullPipelineResponse(BaseModel):
    """전체 파이프라인 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    collect_result: RagasCollectResponse = Field(..., description="데이터 수집 결과")
    evaluate_result: RagasEvaluateResponse = Field(..., description="평가 결과")
    analyze_result: RagasAnalyzeResponse = Field(..., description="분석 결과")
    visualize_result: RagasVisualizeResponse = Field(..., description="차트 생성 결과")
    report_result: RagasReportResponse = Field(..., description="리포트 생성 결과")

class ResultListResponse(BaseModel):
    """결과 목록 응답"""
    success: bool = Field(..., description="성공 여부")
    results: List[Dict[str, Any]] = Field(..., description="결과 목록")
    total_count: int = Field(..., description="총 결과 수")

# =============================================================================
# API 엔드포인트
# =============================================================================

@router.post("/collect",
             response_model=RagasCollectResponse,
             tags=["RAGAS"],
             summary="1단계: RAG 시스템 호출 및 데이터 수집")
async def collect_ragas_data(
    file: UploadFile = File(..., description="평가 데이터셋 JSON 파일"),
    config: Optional[str] = Form(
        None,
        description="데이터 수집 설정 (JSON 형식, 생략 시 기본값 사용)",
        example='{"use_rag": true, "rag_top_k": 3, "rag_threshold": 0.1, "temperature": 0.7, "max_tokens": 1000, "retry_attempts": 3}'
    )
):
    """
    평가 데이터셋의 각 케이스에 대해 RAG 시스템을 호출하고 answer와 contexts를 수집합니다.

    **처리 과정:**
    1. 데이터셋 JSON 파일 업로드
    2. 각 케이스의 question으로 RAG API 호출
    3. answer와 contexts 추가
    4. 완성된 데이터셋 저장

    **입력 데이터셋 형식:**
    ```json
    [
        {
            "id": "case_1",
            "question": "환자 질문",
            "ground_truth": "정답",
            "ktas_level": 3,
            "difficulty": "medium"
        }
    ]
    ```

    **출력 데이터셋 형식:**
    ```json
    [
        {
            "id": "case_1",
            "question": "환자 질문",
            "ground_truth": "정답",
            "answer": "RAG 시스템 답변",
            "contexts": ["검색된 문서1", "검색된 문서2"],
            "ktas_level": 3,
            "difficulty": "medium"
        }
    ]
    ```

    **에러 처리:**
    - 개별 케이스 실패 시 에러 로그 기록 후 계속 진행
    - 재시도 로직 포함 (설정 가능)
    """
    try:
        # 설정 파싱
        try:
            if config is None or config.strip() == "":
                # 기본 설정 사용
                collect_config = RagasCollectRequest()
            else:
                collect_config = RagasCollectRequest(**json.loads(config))
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"설정 파싱 실패: {str(e)}")

        # 파일 확장자 확인
        if not file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="JSON 파일만 업로드 가능합니다.")

        # 임시 파일 저장
        file_content = await file.read()
        with tempfile.NamedTemporaryFile(delete=False, suffix='.json', mode='wb') as tmp_file:
            tmp_file.write(file_content)
            tmp_path = tmp_file.name

        try:
            # RAG 설정 구성
            rag_config = {
                "useRAG": collect_config.use_rag,
                "ragTopK": collect_config.rag_top_k,
                "ragThreshold": collect_config.rag_threshold,
                "temperature": collect_config.temperature,
                "maxTokens": collect_config.max_tokens
            }

            # 평가 설정 구성
            evaluation_config = {
                "retry_attempts": collect_config.retry_attempts,
                "retry_delay": collect_config.retry_delay,
                "timeout": collect_config.timeout
            }

            # RAG API URL
            api_url = collect_config.rag_api_url or RAG_API_URL

            # DataCollector 초기화 및 실행
            collector = DataCollector(
                dataset_path=tmp_path,
                rag_api_url=api_url,
                rag_config=rag_config,
                evaluation_config=evaluation_config
            )

            # 데이터 수집 실행 (비동기)
            output_path, error_count = await collector.collect()

            # 결과 읽기
            with open(tmp_path, 'r', encoding='utf-8') as f:
                dataset = json.load(f)
            total_cases = len(dataset)
            success_count = total_cases - error_count

            # 에러 로그 경로 찾기
            timestamp = Path(output_path).stem.split('_')[-2] + '_' + Path(output_path).stem.split('_')[-1]
            error_path = str(COLLECT_DIR / f"errors_{timestamp}.json") if error_count > 0 else None

            return RagasCollectResponse(
                success=True,
                message=f"데이터 수집 완료: {success_count}/{total_cases} 성공",
                output_path=output_path,
                total_cases=total_cases,
                success_count=success_count,
                error_count=error_count,
                error_path=error_path
            )

        finally:
            # 임시 파일 삭제
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    except Exception as e:
        logger.error(f"데이터 수집 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"데이터 수집 중 오류 발생: {str(e)}")


@router.post("/evaluate",
             response_model=RagasEvaluateResponse,
             tags=["RAGAS"],
             summary="2단계: RAGAS 메트릭 계산")
async def evaluate_with_ragas(
    file: UploadFile = File(..., description="1단계에서 생성된 완성 데이터셋 JSON 파일"),
    config: Optional[str] = Form(
        None,
        description="평가 설정 (JSON 형식, 생략 시 기본값 사용)",
        example='{"llm_provider": "openai", "llm_model": "gpt-3.5-turbo", "llm_temperature": 0.0}'
    )
):
    """
    완성된 데이터셋에 대해 RAGAS 메트릭을 계산합니다.

    **입력:**
    - 1단계(collect)에서 생성된 `dataset_complete_YYYYMMDD_HHMMSS.json` 파일 업로드

    **평가 메트릭:**
    - **faithfulness**: 답변이 contexts에 얼마나 충실한가 (환각 검증)
    - **answer_relevancy**: 답변이 질문과 얼마나 관련있는가
    - **context_precision**: 제공된 contexts가 얼마나 정확한가
    - **context_recall**: ground truth 답변에 필요한 contexts가 모두 포함되었는가
    - **answer_correctness**: 답변이 ground truth와 얼마나 일치하는가

    **점수 해석:**
    - 0.0 ~ 0.5: 개선 필요
    - 0.5 ~ 0.7: 보통
    - 0.7 ~ 0.85: 우수
    - 0.85 ~ 1.0: 매우 우수

    **주의사항:**
    - OPENAI_API_KEY 환경변수 필요
    - 평가에는 시간이 소요됨 (케이스당 약 5-10초)
    """
    try:
        # 설정 파싱
        try:
            if config is None or config.strip() == "":
                # 기본 설정 사용
                evaluate_config = RagasEvaluateRequest(dataset_path="")
            else:
                config_dict = json.loads(config)
                config_dict["dataset_path"] = ""  # 파일 업로드 방식에서는 불필요
                evaluate_config = RagasEvaluateRequest(**config_dict)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"설정 파싱 실패: {str(e)}")

        # 파일 확장자 확인
        if not file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="JSON 파일만 업로드 가능합니다.")

        # 임시 파일 저장
        file_content = await file.read()
        with tempfile.NamedTemporaryFile(delete=False, suffix='.json', mode='wb') as tmp_file:
            tmp_file.write(file_content)
            tmp_path = tmp_file.name

        try:
            # 데이터셋 유효성 검사 (answer와 contexts 포함 여부)
            with open(tmp_path, 'r', encoding='utf-8') as f:
                dataset = json.load(f)

            if not dataset or len(dataset) == 0:
                raise HTTPException(status_code=400, detail="데이터셋이 비어있습니다.")

            # 첫 번째 케이스로 유효성 검사
            first_case = dataset[0]
            # retrieved_contexts 또는 contexts 둘 중 하나가 있어야 함
            has_contexts = "contexts" in first_case or "retrieved_contexts" in first_case
            if "answer" not in first_case or not has_contexts:
                raise HTTPException(
                    status_code=400,
                    detail="완성된 데이터셋이 아닙니다. 1단계(collect)를 먼저 실행하여 answer와 contexts를 추가하세요."
                )

            # LLM 설정 구성
            llm_config = {
                "provider": evaluate_config.llm_provider,
                "model": evaluate_config.llm_model,
                "temperature": evaluate_config.llm_temperature
            }

            # RagasEvaluator 초기화 및 실행
            evaluator = RagasEvaluator(metrics=METRICS, llm_config=llm_config)
            results_path, results_dict = evaluator.evaluate(tmp_path)

            return RagasEvaluateResponse(
                success=True,
                message="RAGAS 평가 완료",
                results_path=results_path,
                metrics=results_dict['metrics'],
                average_score=sum(results_dict['metrics'].values()) / len(results_dict['metrics']),
                total_cases=results_dict['total_cases']
            )

        finally:
            # 임시 파일 삭제
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"RAGAS 평가 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"RAGAS 평가 중 오류 발생: {str(e)}")


@router.post("/analyze",
             response_model=RagasAnalyzeResponse,
             tags=["RAGAS"],
             summary="3단계: 결과 분석")
async def analyze_ragas_results(
    results_file: UploadFile = File(..., description="2단계에서 생성된 RAGAS 평가 결과 JSON 파일 (ragas_results_*.json)"),
    dataset_file: UploadFile = File(..., description="1단계에서 생성된 완성 데이터셋 JSON 파일 (dataset_complete_*.json)"),
    config: Optional[str] = Form(
        None,
        description="분석 설정 (JSON 형식, 생략 시 기본값 사용)",
        example='{"weak_case_threshold": 0.7}'
    )
):
    """
    RAGAS 평가 결과를 상세 분석합니다.

    **입력:**
    - **results_file**: 2단계(evaluate)에서 생성된 `ragas_results_YYYYMMDD_HHMMSS.json` 파일
    - **dataset_file**: 1단계(collect)에서 생성된 `dataset_complete_YYYYMMDD_HHMMSS.json` 파일

    **분석 항목:**
    - 응급도(KTAS Level)별 성능
    - 난이도(Easy/Medium/Hard)별 성능
    - 카테고리별 성능
    - 약점 케이스 식별

    **활용:**
    - 어떤 응급도에서 성능이 낮은지 파악
    - 개선이 필요한 케이스 식별
    - 시스템 약점 분석
    """
    try:
        # 설정 파싱
        try:
            if config is None or config.strip() == "":
                weak_case_threshold = 0.7
            else:
                config_dict = json.loads(config)
                weak_case_threshold = config_dict.get("weak_case_threshold", 0.7)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"설정 파싱 실패: {str(e)}")

        # 파일 확장자 확인
        if not results_file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="평가 결과 파일은 JSON 형식이어야 합니다.")
        if not dataset_file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="데이터셋 파일은 JSON 형식이어야 합니다.")

        # 임시 파일 저장
        results_content = await results_file.read()
        dataset_content = await dataset_file.read()

        with tempfile.NamedTemporaryFile(delete=False, suffix='_results.json', mode='wb') as tmp_results:
            tmp_results.write(results_content)
            tmp_results_path = tmp_results.name

        with tempfile.NamedTemporaryFile(delete=False, suffix='_dataset.json', mode='wb') as tmp_dataset:
            tmp_dataset.write(dataset_content)
            tmp_dataset_path = tmp_dataset.name

        try:
            # 파일 유효성 검사
            with open(tmp_results_path, 'r', encoding='utf-8') as f:
                results_data = json.load(f)
                if "per_case_results" not in results_data:
                    raise HTTPException(
                        status_code=400,
                        detail="올바른 RAGAS 평가 결과 파일이 아닙니다. 2단계(evaluate)를 먼저 실행하세요."
                    )

            with open(tmp_dataset_path, 'r', encoding='utf-8') as f:
                dataset_data = json.load(f)
                if not dataset_data or len(dataset_data) == 0:
                    raise HTTPException(status_code=400, detail="데이터셋이 비어있습니다.")

            # ResultAnalyzer 초기화
            analyzer = ResultAnalyzer(tmp_results_path, tmp_dataset_path)

            # 분석 수행
            ktas_performance = analyzer.analyze_by_ktas_level()
            difficulty_performance = analyzer.analyze_by_difficulty()
            category_performance = analyzer.analyze_by_category()
            weak_cases = analyzer.find_weak_cases(threshold=weak_case_threshold)

            # weak_cases 개수 저장 (자르기 전)
            weak_cases_count = len(weak_cases)

            # NaN을 None으로 변환하는 헬퍼 함수
            def clean_nan(obj):
                """NaN 값을 None으로 재귀적으로 변환"""
                import math
                if isinstance(obj, dict):
                    return {k: clean_nan(v) for k, v in obj.items()}
                elif isinstance(obj, list):
                    return [clean_nan(item) for item in obj]
                elif isinstance(obj, float) and math.isnan(obj):
                    return None
                else:
                    return obj

            # 딕셔너리 키를 문자열로 변환 + NaN 처리
            ktas_performance = clean_nan({str(k): v for k, v in ktas_performance.items()})
            difficulty_performance = clean_nan({str(k): v for k, v in difficulty_performance.items()})
            category_performance = clean_nan({str(k): v for k, v in category_performance.items()})
            weak_cases = clean_nan(weak_cases[:10])

            # 분석 결과를 파일로 저장
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            analysis_filename = f"analysis_results_{timestamp}.json"
            analysis_path = ANALYZE_DIR / analysis_filename

            analysis_data = {
                "timestamp": timestamp,
                "results_file": results_file.filename,
                "dataset_file": dataset_file.filename,
                "weak_case_threshold": weak_case_threshold,
                "ktas_performance": ktas_performance,
                "difficulty_performance": difficulty_performance,
                "category_performance": category_performance,
                "weak_cases_count": weak_cases_count,
                "weak_cases": weak_cases
            }

            with open(analysis_path, 'w', encoding='utf-8') as f:
                json.dump(analysis_data, f, ensure_ascii=False, indent=2)

            return RagasAnalyzeResponse(
                success=True,
                message="결과 분석 완료",
                analysis_path=str(analysis_path),
                ktas_performance=ktas_performance,
                difficulty_performance=difficulty_performance,
                category_performance=category_performance,
                weak_cases_count=weak_cases_count,  # 전체 약점 케이스 수
                weak_cases=weak_cases  # 최대 10개만 반환 (이미 [:10] 처리됨)
            )

        finally:
            # 임시 파일 삭제
            if os.path.exists(tmp_results_path):
                os.remove(tmp_results_path)
            if os.path.exists(tmp_dataset_path):
                os.remove(tmp_dataset_path)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"결과 분석 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"결과 분석 중 오류 발생: {str(e)}")


@router.post("/visualize",
             response_model=RagasVisualizeResponse,
             tags=["RAGAS"],
             summary="4단계: 차트 생성")
async def visualize_ragas_results(
    results_file: UploadFile = File(..., description="2단계에서 생성된 RAGAS 평가 결과 JSON 파일 (ragas_results_*.json)"),
    dataset_file: UploadFile = File(..., description="1단계에서 생성된 완성 데이터셋 JSON 파일 (dataset_complete_*.json)"),
    config: Optional[str] = Form(
        None,
        description="차트 생성 설정 (JSON 형식, 생략 시 기본값 사용)",
        example='{"output_prefix": "my_chart_"}'
    )
):
    """
    RAGAS 평가 결과를 시각화한 차트를 생성합니다.

    **입력:**
    - **results_file**: 2단계(evaluate)에서 생성된 `ragas_results_YYYYMMDD_HHMMSS.json` 파일
    - **dataset_file**: 1단계(collect)에서 생성된 `dataset_complete_YYYYMMDD_HHMMSS.json` 파일

    **생성되는 차트:**
    - **레이더 차트**: 5개 메트릭 종합 비교
    - **응급도별 차트**: KTAS Level별 성능 비교
    - **카테고리 히트맵**: 카테고리별 성능 히트맵
    - **난이도별 차트**: Easy/Medium/Hard 성능 비교

    **파일 형식:** PNG
    **저장 위치:** rag/data/charts/
    """
    try:
        # 설정 파싱
        try:
            if config is None or config.strip() == "":
                output_prefix = None
            else:
                config_dict = json.loads(config)
                output_prefix = config_dict.get("output_prefix", None)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"설정 파싱 실패: {str(e)}")

        # 파일 확장자 확인
        if not results_file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="평가 결과 파일은 JSON 형식이어야 합니다.")
        if not dataset_file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="데이터셋 파일은 JSON 형식이어야 합니다.")

        # 임시 파일 저장
        results_content = await results_file.read()
        dataset_content = await dataset_file.read()

        with tempfile.NamedTemporaryFile(delete=False, suffix='_results.json', mode='wb') as tmp_results:
            tmp_results.write(results_content)
            tmp_results_path = tmp_results.name

        with tempfile.NamedTemporaryFile(delete=False, suffix='_dataset.json', mode='wb') as tmp_dataset:
            tmp_dataset.write(dataset_content)
            tmp_dataset_path = tmp_dataset.name

        try:
            # 파일 유효성 검사
            with open(tmp_results_path, 'r', encoding='utf-8') as f:
                results_data = json.load(f)
                if "per_case_results" not in results_data:
                    raise HTTPException(
                        status_code=400,
                        detail="올바른 RAGAS 평가 결과 파일이 아닙니다. 2단계(evaluate)를 먼저 실행하세요."
                    )

            with open(tmp_dataset_path, 'r', encoding='utf-8') as f:
                dataset_data = json.load(f)
                if not dataset_data or len(dataset_data) == 0:
                    raise HTTPException(status_code=400, detail="데이터셋이 비어있습니다.")

            # Analyzer 및 Visualizer 초기화
            analyzer = ResultAnalyzer(tmp_results_path, tmp_dataset_path)
            visualizer = Visualizer(analyzer)

            # 차트 생성
            prefix = output_prefix or datetime.now().strftime("%Y%m%d_%H%M%S_")
            chart_paths = visualizer.create_all_charts(
                output_dir=CHARTS_DIR,
                prefix=prefix
            )

            return RagasVisualizeResponse(
                success=True,
                message="차트 생성 완료",
                chart_paths=chart_paths
            )

        finally:
            # 임시 파일 삭제
            if os.path.exists(tmp_results_path):
                os.remove(tmp_results_path)
            if os.path.exists(tmp_dataset_path):
                os.remove(tmp_dataset_path)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"차트 생성 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"차트 생성 중 오류 발생: {str(e)}")


@router.post("/report",
             response_model=RagasReportResponse,
             tags=["RAGAS"],
             summary="5단계: 발표 리포트 생성")
async def generate_ragas_report(
    results_file: UploadFile = File(..., description="2단계에서 생성된 RAGAS 평가 결과 JSON 파일 (ragas_results_*.json)"),
    dataset_file: UploadFile = File(..., description="1단계에서 생성된 완성 데이터셋 JSON 파일 (dataset_complete_*.json)"),
    config: Optional[str] = Form(
        None,
        description="리포트 생성 설정 (JSON 형식)",
        example='{"chart_paths": {"radar_chart": "path/to/chart.png", ...}, "output_filename": "my_report.md"}'
    )
):
    """
    RAGAS 평가 결과와 차트를 포함한 마크다운 리포트를 생성합니다.

    **입력:**
    - **results_file**: 2단계(evaluate)에서 생성된 `ragas_results_YYYYMMDD_HHMMSS.json` 파일
    - **dataset_file**: 1단계(collect)에서 생성된 `dataset_complete_YYYYMMDD_HHMMSS.json` 파일
    - **config** (선택):
      - `chart_paths`: 4단계(visualize)의 응답에서 받은 차트 경로 딕셔너리
      - `output_filename`: 리포트 파일명 (생략 시 자동 생성)

    **리포트 내용:**
    - 평가 요약
    - 메트릭별 점수 및 해석
    - 차트 이미지 포함 (chart_paths 제공 시)
    - 응급도/난이도/카테고리별 분석
    - 개선 권장사항

    **파일 형식:** Markdown (.md)
    **저장 위치:** rag/data/reports/
    **활용:** 발표 자료 작성, 결과 보고서
    """
    try:
        # 설정 파싱
        try:
            if config is None or config.strip() == "":
                chart_paths = {}
                output_filename = None
            else:
                config_dict = json.loads(config)
                chart_paths = config_dict.get("chart_paths", {})
                output_filename = config_dict.get("output_filename", None)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"설정 파싱 실패: {str(e)}")

        # 파일 확장자 확인
        if not results_file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="평가 결과 파일은 JSON 형식이어야 합니다.")
        if not dataset_file.filename.endswith('.json'):
            raise HTTPException(status_code=400, detail="데이터셋 파일은 JSON 형식이어야 합니다.")

        # 임시 파일 저장
        results_content = await results_file.read()
        dataset_content = await dataset_file.read()

        with tempfile.NamedTemporaryFile(delete=False, suffix='_results.json', mode='wb') as tmp_results:
            tmp_results.write(results_content)
            tmp_results_path = tmp_results.name

        with tempfile.NamedTemporaryFile(delete=False, suffix='_dataset.json', mode='wb') as tmp_dataset:
            tmp_dataset.write(dataset_content)
            tmp_dataset_path = tmp_dataset.name

        try:
            # 파일 유효성 검사
            with open(tmp_results_path, 'r', encoding='utf-8') as f:
                results_data = json.load(f)
                if "per_case_results" not in results_data:
                    raise HTTPException(
                        status_code=400,
                        detail="올바른 RAGAS 평가 결과 파일이 아닙니다. 2단계(evaluate)를 먼저 실행하세요."
                    )

            with open(tmp_dataset_path, 'r', encoding='utf-8') as f:
                dataset_data = json.load(f)
                if not dataset_data or len(dataset_data) == 0:
                    raise HTTPException(status_code=400, detail="데이터셋이 비어있습니다.")

            # Analyzer 및 ReportGenerator 초기화
            analyzer = ResultAnalyzer(tmp_results_path, tmp_dataset_path)
            report_gen = ReportGenerator(analyzer, chart_paths)

            # 리포트 생성
            if output_filename:
                save_path = REPORTS_DIR / output_filename
            else:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                save_path = REPORTS_DIR / f"report_{timestamp}.md"

            report_path = report_gen.generate_report(save_path=save_path)

            return RagasReportResponse(
                success=True,
                message="리포트 생성 완료",
                report_path=str(report_path)
            )

        finally:
            # 임시 파일 삭제
            if os.path.exists(tmp_results_path):
                os.remove(tmp_results_path)
            if os.path.exists(tmp_dataset_path):
                os.remove(tmp_dataset_path)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"리포트 생성 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"리포트 생성 중 오류 발생: {str(e)}")


@router.post("/full-pipeline",
             response_model=RagasFullPipelineResponse,
             tags=["RAGAS"],
             summary="전체 파이프라인 실행 (1~5단계 통합)")
async def run_full_ragas_pipeline(
    file: UploadFile = File(..., description="평가 데이터셋 JSON 파일"),
    config: Optional[str] = Form(
        None,
        description="전체 파이프라인 설정 (JSON 형식, 생략 시 기본값 사용)",
        example='{"collect_config": {"use_rag": true, "rag_top_k": 3}, "evaluate_config": {}, "visualize_config": {}, "report_config": {}}'
    )
):
    """
    RAGAS 평가 전체 파이프라인을 한 번에 실행합니다.

    **실행 순서:**
    1. 데이터 수집 (RAG 호출)
    2. RAGAS 평가
    3. 결과 분석
    4. 차트 생성
    5. 리포트 생성

    **장점:**
    - 한 번의 API 호출로 모든 과정 완료
    - 자동으로 파일 경로 연결
    - 결과물 일괄 생성

    **주의사항:**
    - 시간이 오래 걸릴 수 있음 (케이스 수에 따라 수 분~수십 분)
    - OPENAI_API_KEY 환경변수 필요
    - RAG 서버가 실행 중이어야 함
    """
    try:
        # 설정 파싱
        try:
            if config is None or config.strip() == "":
                # 기본 설정 사용
                pipeline_config = RagasFullPipelineRequest()
            else:
                pipeline_config = RagasFullPipelineRequest(**json.loads(config))
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"설정 파싱 실패: {str(e)}")

        # 1. 데이터 수집
        logger.info("[1/5] 데이터 수집 시작...")
        collect_result = await collect_ragas_data(
            file=file,
            config=json.dumps(pipeline_config.collect_config.dict())
        )

        # 2. RAGAS 평가
        logger.info("[2/5] RAGAS 평가 시작...")
        # collect 결과 파일을 UploadFile로 변환하여 evaluate에 전달
        with open(collect_result.output_path, 'rb') as f:
            file_content = f.read()

        completed_file = UploadFile(
            filename=os.path.basename(collect_result.output_path),
            file=BytesIO(file_content)
        )

        evaluate_result = await evaluate_with_ragas(
            file=completed_file,
            config=json.dumps(pipeline_config.evaluate_config.dict())
        )

        # 3. 결과 분석
        logger.info("[3/5] 결과 분석 시작...")
        # 평가 결과 파일과 데이터셋 파일을 UploadFile로 변환
        with open(evaluate_result.results_path, 'rb') as f:
            results_content = f.read()
        with open(collect_result.output_path, 'rb') as f:
            dataset_content = f.read()

        results_upload = UploadFile(
            filename=os.path.basename(evaluate_result.results_path),
            file=BytesIO(results_content)
        )
        dataset_upload = UploadFile(
            filename=os.path.basename(collect_result.output_path),
            file=BytesIO(dataset_content)
        )

        analyze_result = await analyze_ragas_results(
            results_file=results_upload,
            dataset_file=dataset_upload,
            config=json.dumps({"weak_case_threshold": pipeline_config.weak_case_threshold})
        )

        # 4. 차트 생성
        logger.info("[4/5] 차트 생성 시작...")
        # 파일을 다시 읽어서 UploadFile로 변환 (analyze에서 이미 읽었지만 재사용 불가)
        with open(evaluate_result.results_path, 'rb') as f:
            results_content_viz = f.read()
        with open(collect_result.output_path, 'rb') as f:
            dataset_content_viz = f.read()

        results_upload_viz = UploadFile(
            filename=os.path.basename(evaluate_result.results_path),
            file=BytesIO(results_content_viz)
        )
        dataset_upload_viz = UploadFile(
            filename=os.path.basename(collect_result.output_path),
            file=BytesIO(dataset_content_viz)
        )

        visualize_result = await visualize_ragas_results(
            results_file=results_upload_viz,
            dataset_file=dataset_upload_viz,
            config=None  # 기본 prefix 사용
        )

        # 5. 리포트 생성
        logger.info("[5/5] 리포트 생성 시작...")
        # 파일을 다시 읽어서 UploadFile로 변환
        with open(evaluate_result.results_path, 'rb') as f:
            results_content_report = f.read()
        with open(collect_result.output_path, 'rb') as f:
            dataset_content_report = f.read()

        results_upload_report = UploadFile(
            filename=os.path.basename(evaluate_result.results_path),
            file=BytesIO(results_content_report)
        )
        dataset_upload_report = UploadFile(
            filename=os.path.basename(collect_result.output_path),
            file=BytesIO(dataset_content_report)
        )

        # chart_paths를 config에 포함
        report_config = {
            "chart_paths": visualize_result.chart_paths
        }

        report_result = await generate_ragas_report(
            results_file=results_upload_report,
            dataset_file=dataset_upload_report,
            config=json.dumps(report_config)
        )

        return RagasFullPipelineResponse(
            success=True,
            message="전체 파이프라인 실행 완료",
            collect_result=collect_result,
            evaluate_result=evaluate_result,
            analyze_result=analyze_result,
            visualize_result=visualize_result,
            report_result=report_result
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"전체 파이프라인 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"전체 파이프라인 실행 중 오류 발생: {str(e)}")


@router.get("/results",
            response_model=ResultListResponse,
            tags=["RAGAS"],
            summary="평가 결과 목록 조회")
async def list_ragas_results(
    limit: int = Query(50, description="최대 결과 개수", ge=1, le=100),
    offset: int = Query(0, description="결과 오프셋", ge=0)
):
    """
    저장된 RAGAS 평가 결과 목록을 조회합니다.

    **반환 정보:**
    - 파일명
    - 생성 시간
    - 파일 크기
    - 평가 케이스 수 (가능한 경우)
    """
    try:
        # 결과 디렉토리에서 파일 목록 가져오기
        result_files = []

        if EVALUATE_DIR.exists():
            for file_path in sorted(EVALUATE_DIR.glob("ragas_results_*.json"), reverse=True):
                try:
                    stat = file_path.stat()
                    # 파일 내용 읽어서 케이스 수 확인
                    with open(file_path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        total_cases = data.get('total_cases', 0)

                    result_files.append({
                        "filename": file_path.name,
                        "path": str(file_path),
                        "created_at": datetime.fromtimestamp(stat.st_ctime).isoformat(),
                        "size_bytes": stat.st_size,
                        "total_cases": total_cases
                    })
                except Exception as e:
                    logger.warning(f"파일 정보 읽기 실패: {file_path.name} - {e}")

        # 페이지네이션
        paginated_results = result_files[offset:offset+limit]

        return ResultListResponse(
            success=True,
            results=paginated_results,
            total_count=len(result_files)
        )

    except Exception as e:
        logger.error(f"결과 목록 조회 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"결과 목록 조회 중 오류 발생: {str(e)}")


@router.get("/results/{result_filename}",
            tags=["RAGAS"],
            summary="특정 평가 결과 상세 조회")
async def get_ragas_result_detail(result_filename: str):
    """
    특정 RAGAS 평가 결과 파일의 내용을 반환합니다.

    **사용 예:**
    - result_filename: "ragas_results_20251111_134442.json"
    """
    try:
        file_path = EVALUATE_DIR / result_filename

        if not file_path.exists():
            raise HTTPException(status_code=404, detail=f"결과 파일을 찾을 수 없습니다: {result_filename}")

        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        return JSONResponse(content=data)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"결과 조회 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"결과 조회 중 오류 발생: {str(e)}")


@router.get("/charts/{chart_filename}",
            tags=["RAGAS"],
            summary="차트 파일 다운로드")
async def download_chart(chart_filename: str):
    """
    생성된 차트 이미지 파일을 다운로드합니다.

    **사용 예:**
    - chart_filename: "20251111_133647_radar_chart.png"
    """
    try:
        file_path = CHARTS_DIR / chart_filename

        if not file_path.exists():
            raise HTTPException(status_code=404, detail=f"차트 파일을 찾을 수 없습니다: {chart_filename}")

        return FileResponse(
            path=str(file_path),
            media_type="image/png",
            filename=chart_filename
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"차트 다운로드 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"차트 다운로드 중 오류 발생: {str(e)}")


@router.get("/reports/{report_filename}",
            tags=["RAGAS"],
            summary="리포트 파일 다운로드")
async def download_report(report_filename: str):
    """
    생성된 리포트 마크다운 파일을 다운로드합니다.

    **사용 예:**
    - report_filename: "report_20251111_133647.md"
    """
    try:
        file_path = REPORTS_DIR / report_filename

        if not file_path.exists():
            raise HTTPException(status_code=404, detail=f"리포트 파일을 찾을 수 없습니다: {report_filename}")

        return FileResponse(
            path=str(file_path),
            media_type="text/markdown",
            filename=report_filename
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"리포트 다운로드 실패: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"리포트 다운로드 중 오류 발생: {str(e)}")


@router.get("/health",
            tags=["RAGAS"],
            summary="RAGAS 서비스 헬스 체크")
async def health_check():
    """
    RAGAS 평가 시스템의 상태를 확인합니다.

    **확인 항목:**
    - 필수 디렉토리 존재 여부
    - OPENAI_API_KEY 환경변수 설정 여부
    - 최근 평가 결과 수
    """
    try:
        # 디렉토리 확인
        dirs_status = {
            "collect_dir": COLLECT_DIR.exists(),
            "evaluate_dir": EVALUATE_DIR.exists(),
            "charts_dir": CHARTS_DIR.exists(),
            "reports_dir": REPORTS_DIR.exists()
        }

        # 환경변수 확인
        openai_key_set = bool(os.getenv("OPENAI_API_KEY"))

        # 최근 결과 수 확인
        recent_results_count = len(list(EVALUATE_DIR.glob("ragas_results_*.json"))) if EVALUATE_DIR.exists() else 0

        return {
            "status": "healthy",
            "directories": dirs_status,
            "openai_key_configured": openai_key_set,
            "recent_results_count": recent_results_count,
            "rag_api_url": RAG_API_URL,
            "metrics_enabled": [m.name if hasattr(m, 'name') else m.__class__.__name__ for m in METRICS]
        }

    except Exception as e:
        logger.error(f"헬스 체크 실패: {str(e)}", exc_info=True)
        return {
            "status": "unhealthy",
            "error": str(e)
        }
