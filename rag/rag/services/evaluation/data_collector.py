"""RAG 시스템 호출 및 answer/contexts 수집"""

import json
import httpx  # 비동기 HTTP 클라이언트
import asyncio
from tqdm import tqdm
from datetime import datetime
from pathlib import Path
import logging

class DataCollector:
    """RAG 시스템을 호출하여 평가용 데이터 수집"""

    def __init__(self, dataset_path, rag_api_url, rag_config, evaluation_config):
        """
        Args:
            dataset_path: 평가 데이터셋 경로
            rag_api_url: RAG API 엔드포인트
            rag_config: RAG 호출 설정
            evaluation_config: 평가 실행 설정
        """
        self.dataset_path = dataset_path
        self.rag_api_url = rag_api_url
        self.rag_config = rag_config
        self.evaluation_config = evaluation_config

        # 로깅 설정
        self.logger = logging.getLogger(__name__)

    async def collect(self, output_path=None):
        """
        모든 케이스에 대해 RAG 실행 및 데이터 수집

        Returns:
            tuple: (완성된 데이터셋 경로, 에러 수)
        """
        # 데이터셋 로드
        with open(self.dataset_path, 'r', encoding='utf-8') as f:
            dataset = json.load(f)

        total_cases = len(dataset)
        self.logger.info(f"총 {total_cases}개 케이스 처리 시작...")
        print(f"\n{'='*60}")
        print(f"RAG 시스템 호출 시작")
        print(f"{'='*60}")
        print(f"데이터셋: {self.dataset_path}")
        print(f"총 케이스: {total_cases}개")
        print(f"API 엔드포인트: {self.rag_api_url}")
        print(f"{'='*60}\n")

        completed_dataset = []
        errors = []

        # 진행상황 표시
        with tqdm(total=total_cases, desc="RAG 호출 중") as pbar:
            for idx, case in enumerate(dataset):
                case_id = case.get("id", f"case_{idx}")

                try:
                    # RAG 호출
                    answer, contexts = await self._call_rag(case["question"])

                    # answer와 contexts 추가
                    case["answer"] = answer
                    case["retrieved_contexts"] = contexts  # RAGAS는 'retrieved_contexts' 필드명을 요구

                    completed_dataset.append(case)
                    pbar.set_postfix({"성공": len(completed_dataset), "실패": len(errors)})

                except Exception as e:
                    error_info = {
                        "case_id": case_id,
                        "error": str(e),
                        "timestamp": datetime.now().isoformat()
                    }
                    errors.append(error_info)
                    self.logger.error(f"케이스 {case_id} 실패: {e}")
                    pbar.set_postfix({"성공": len(completed_dataset), "실패": len(errors)})

                pbar.update(1)

        # 결과 저장
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        if output_path is None:
            from config import COLLECT_DIR
            output_path = COLLECT_DIR / f"dataset_complete_{timestamp}.json"

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(completed_dataset, f, ensure_ascii=False, indent=2)

        # 통계 출력
        print(f"\n{'='*60}")
        print(f"데이터 수집 완료")
        print(f"{'='*60}")
        print(f"[OK] 성공: {len(completed_dataset)}/{total_cases} ({len(completed_dataset)/total_cases*100:.1f}%)")
        print(f"[ERROR] 실패: {len(errors)}/{total_cases} ({len(errors)/total_cases*100:.1f}%)")
        print(f"[FILES] 저장 위치: {output_path}")

        # 에러 로그 저장
        if errors:
            from config import COLLECT_DIR
            error_path = COLLECT_DIR / f"errors_{timestamp}.json"
            with open(error_path, 'w', encoding='utf-8') as f:
                json.dump(errors, f, ensure_ascii=False, indent=2)
            print(f"[WARNING] 에러 로그: {error_path}")

            # 에러 상세 출력
            print(f"\n에러 케이스:")
            for error in errors[:5]:  # 처음 5개만 출력
                print(f"  - {error['case_id']}: {error['error']}")
            if len(errors) > 5:
                print(f"  ... 외 {len(errors)-5}개")

        print(f"{'='*60}\n")

        return str(output_path), len(errors)

    async def _call_rag(self, question):
        """
        RAG API 호출 (비동기)

        Args:
            question: 질문 텍스트

        Returns:
            tuple: (answer, contexts)
        """
        max_retries = self.evaluation_config.get("retry_attempts", 3)
        retry_delay = self.evaluation_config.get("retry_delay", 5)
        timeout = self.evaluation_config.get("timeout", 60)

        for attempt in range(max_retries):
            # 매 시도마다 새로운 비동기 클라이언트 생성 (연결 누수 방지)
            async with httpx.AsyncClient(timeout=timeout) as client:
                try:
                    # API 요청 (비동기)
                    response = await client.post(
                        self.rag_api_url,
                        json={
                            "messages": [
                                {"role": "user", "content": question}
                            ],
                            **self.rag_config
                        }
                    )

                    if response.status_code == 200:
                        result = response.json()

                        # answer 추출
                        answer = result.get("result", {}).get("message", {}).get("content", "")

                        # contexts 추출 (최상위 레벨에 있음)
                        contexts = result.get("contexts", [])

                        # contexts 로깅
                        if contexts:
                            self.logger.info(f"[OK] contexts 추출 성공: {len(contexts)}개")
                        else:
                            self.logger.warning("[WARNING] RAG 응답에 contexts가 없거나 비어있습니다")

                        return answer, contexts

                    else:
                        raise Exception(f"HTTP {response.status_code}: {response.text}")

                except Exception as e:
                    if attempt < max_retries - 1:
                        self.logger.warning(f"재시도 {attempt+1}/{max_retries}: {e}")
                        await asyncio.sleep(retry_delay)
                    else:
                        raise e
                # async with가 자동으로 client.aclose() 호출 (CLOSE_WAIT 방지)

        raise Exception(f"최대 재시도 횟수 ({max_retries}) 초과")


# 독립 실행용
if __name__ == "__main__":
    import sys
    sys.path.append(str(Path(__file__).parent))

    from config import (
        SAMPLE_DATASET_PATH,
        RAG_API_URL,
        RAG_CONFIG,
        EVALUATION_CONFIG
    )

    # 로깅 설정
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # 비동기 메인 함수
    async def main():
        # 데이터 수집 실행
        collector = DataCollector(
            dataset_path=SAMPLE_DATASET_PATH,
            rag_api_url=RAG_API_URL,
            rag_config=RAG_CONFIG,
            evaluation_config=EVALUATION_CONFIG
        )

        output_path, error_count = await collector.collect()

        if error_count > 0:
            print(f"\n[WARNING] {error_count}개 케이스에서 에러 발생")
            sys.exit(1)
        else:
            print(f"\n[OK] 모든 케이스 수집 완료!")
            sys.exit(0)

    # 비동기 실행
    asyncio.run(main())
