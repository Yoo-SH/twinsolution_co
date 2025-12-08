"""RAGAS 메트릭 계산"""

import json
import logging
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any

try:
    from ragas import evaluate
    from ragas.metrics import (
        faithfulness,
        answer_relevancy,
        context_precision,
        context_recall,
        answer_correctness
    )
    from datasets import Dataset

    # CLOVA Studio LLM 사용 (OpenAI 대신)
    try:
        # evaluation 디렉토리에서 실행 시 상대 경로 사용
        import sys
        from pathlib import Path

        # rag/ 디렉토리를 sys.path에 추가
        rag_root = Path(__file__).parent.parent.parent
        if str(rag_root) not in sys.path:
            sys.path.insert(0, str(rag_root))

        from services.clova_chat_llm import ClovaStudioChatModel
        from services.rag_embedding_service import ClovaStudioEmbeddingService
        USE_CLOVA = True
        print("[OK] CLOVA Studio LLM 사용")
    except ImportError as import_err:
        # CLOVA 사용 불가 시 OpenAI 폴백
        print(f"[WARNING] CLOVA Studio LLM 임포트 실패: {import_err}")
        try:
            from langchain_openai import ChatOpenAI
            from langchain_openai import OpenAIEmbeddings
            USE_CLOVA = False
            print("[WARNING] OpenAI 사용으로 폴백")
        except ImportError:
            print("[ERROR] OpenAI 라이브러리도 없습니다. langchain-openai 설치 필요")
            raise

    RAGAS_AVAILABLE = True
except ImportError as e:
    RAGAS_AVAILABLE = False
    USE_CLOVA = False
    print("[ERROR] RAGAS가 설치되지 않았습니다. 다음 명령어로 설치하세요:")
    print("   pip install ragas datasets")
    print("   OpenAI 사용 시: pip install langchain-openai")
    print(f"   에러: {e}")


class RagasEvaluator:
    """RAGAS 프레임워크를 사용한 RAG 성능 평가"""

    def __init__(self, metrics=None, llm_config=None):
        """
        Args:
            metrics: 평가할 메트릭 리스트 (기본: 전체)
            llm_config: RAGAS 내부 LLM 설정
        """
        if not RAGAS_AVAILABLE:
            raise ImportError("RAGAS 라이브러리가 필요합니다. pip install ragas datasets")

        self.metrics = metrics or [
            faithfulness,
            answer_relevancy,
            context_precision,
            context_recall,
            answer_correctness
        ]
        self.llm_config = llm_config or {}
        self.logger = logging.getLogger(__name__)

    def evaluate(self, dataset_path, output_path=None):
        """
        RAGAS 평가 실행

        Args:
            dataset_path: 완성된 데이터셋 경로 (question, answer, contexts, ground_truth 포함)
            output_path: 결과 저장 경로 (None이면 자동 생성)

        Returns:
            tuple: (결과 파일 경로, 결과 딕셔너리)
        """
        # 데이터셋 로드
        with open(dataset_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        total_cases = len(data)
        self.logger.info(f"RAGAS 평가 시작: {total_cases}개 케이스")

        print(f"\n{'='*60}")
        print(f"RAGAS 평가 실행")
        print(f"{'='*60}")
        print(f"데이터셋: {dataset_path}")
        print(f"총 케이스: {total_cases}개")
        print(f"평가 메트릭: {len(self.metrics)}개")
        for metric in self.metrics:
            print(f"  - {metric.name}")
        print(f"{'='*60}\n")

        # RAGAS 형식으로 변환 (RAGAS v0.2+ 필드명 사용)
        print("[INFO] 데이터셋 변환 중...")
        ragas_dataset = Dataset.from_dict({
            "user_input": [d["question"] for d in data],      # RAGAS v0.2+에서는 'user_input' 사용
            "response": [d["answer"] for d in data],          # RAGAS v0.2+에서는 'response' 사용
            "retrieved_contexts": [d["retrieved_contexts"] for d in data],
            "reference": [d["ground_truth"] for d in data]    # RAGAS v0.2+에서는 'reference' 사용
        })

        # 평가 실행
        print("[INFO] RAGAS 메트릭 계산 중... (시간이 걸릴 수 있습니다)")
        print("       RAGAS는 내부적으로 LLM을 사용하여 메트릭을 계산합니다.")

        try:
            # LLM 설정 - CLOVA 또는 OpenAI
            if USE_CLOVA:
                print("[INFO] CLOVA Studio API 사용 중...")
                llm = ClovaStudioChatModel(
                    model="HCX-DASH-002",  # 빠르고 저렴한 모델
                    temperature=0.0,       # 일관성을 위해 0으로 설정
                )
                embeddings = ClovaStudioEmbeddingService()
            else:
                print("[INFO] OpenAI API 사용 중...")
                llm = ChatOpenAI(model="gpt-3.5-turbo", temperature=0)
                embeddings = OpenAIEmbeddings()

            # RAGAS 실행 설정 (타임아웃 증가)
            from ragas.run_config import RunConfig
            run_config = RunConfig(
                timeout=300,          # 각 메트릭 계산 타임아웃을 300초로 증가
                max_workers=4,        # 병렬 처리 워커 수 (기본값 유지)
                max_wait=600          # 전체 대기 시간을 600초로 증가
            )

            results = evaluate(
                ragas_dataset,
                metrics=self.metrics,
                llm=llm,
                embeddings=embeddings,
                run_config=run_config
            )
        except Exception as e:
            self.logger.error(f"RAGAS 평가 실패: {e}")
            print(f"\n[ERROR] RAGAS 평가 중 에러 발생:")
            print(f"   {e}")
            print(f"\n[INFO] 가능한 원인:")
            print(f"   1. API 키 환경변수가 설정되지 않음")
            print(f"   2. API 키가 유효하지 않음")
            print(f"   3. API 호출 한도 초과")
            print(f"   4. contexts가 비어있음 (RAG 서비스 수정 필요)")
            raise

        # 결과 저장
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        if output_path is None:
            from config import EVALUATE_DIR
            output_path = EVALUATE_DIR / f"ragas_results_{timestamp}.json"

        # DataFrame을 JSON으로 변환
        results_dict = {
            "timestamp": timestamp,
            "dataset_path": str(dataset_path),
            "total_cases": total_cases,
            "metrics": {},
            "per_case_results": []
        }

        # 케이스별 상세 결과를 먼저 가져오기
        results_df = results.to_pandas()
        results_dict["per_case_results"] = results_df.to_dict('records')

        # 전체 평균 메트릭 - DataFrame의 컬럼에서 가져오기
        for metric in self.metrics:
            metric_name = metric.name
            if metric_name in results_df.columns:
                # NaN이 아닌 값들의 평균 계산
                metric_values = results_df[metric_name].dropna()
                if len(metric_values) > 0:
                    results_dict["metrics"][metric_name] = float(metric_values.mean())

        # JSON 저장
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(results_dict, f, ensure_ascii=False, indent=2)

        # 결과 출력
        print(f"\n{'='*60}")
        print(f"RAGAS 평가 완료")
        print(f"{'='*60}")
        print(f"[FILES] 결과 저장: {output_path}")
        print(f"\n[INFO] 평가 결과:")
        print(f"{'-'*60}")

        for metric_name, score in results_dict["metrics"].items():
            # 메트릭별 평가 기준
            if score >= 0.9:
                grade = "우수 ***"
            elif score >= 0.8:
                grade = "양호 **"
            elif score >= 0.7:
                grade = "보통 *"
            else:
                grade = "개선 필요"

            print(f"{metric_name:20s}: {score:.4f}  ({grade})")

        print(f"{'-'*60}")

        # 종합 평가
        avg_score = sum(results_dict["metrics"].values()) / len(results_dict["metrics"])
        print(f"{'평균 점수':20s}: {avg_score:.4f}")
        print(f"{'='*60}\n")

        return str(output_path), results_dict


# 독립 실행용
if __name__ == "__main__":
    import sys
    import argparse

    # 로깅 설정
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # 명령줄 인자
    parser = argparse.ArgumentParser(description="RAGAS 평가 실행")
    parser.add_argument("dataset", help="완성된 데이터셋 경로 (JSON)")
    parser.add_argument("--output", help="결과 저장 경로", default=None)
    args = parser.parse_args()

    # 평가 실행
    evaluator = RagasEvaluator()

    try:
        output_path, results_dict = evaluator.evaluate(
            dataset_path=args.dataset,
            output_path=args.output
        )
        print(f"\n[OK] 평가 완료! 결과: {output_path}")
        sys.exit(0)

    except Exception as e:
        print(f"\n[ERROR] 평가 실패: {e}")
        sys.exit(1)
