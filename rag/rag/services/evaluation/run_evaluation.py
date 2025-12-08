"""RAGAS 평가 파이프라인 전체 실행"""

import argparse
import sys
import logging
from pathlib import Path
from datetime import datetime

# rag/ 디렉토리를 sys.path에 추가
rag_root = Path(__file__).parent.parent.parent
if str(rag_root) not in sys.path:
    sys.path.insert(0, str(rag_root))

# 모듈 import
from services.evaluation.config import (
    DATASET_PATH,
    SAMPLE_DATASET_PATH,
    RAG_API_URL,
    RAG_CONFIG,
    EVALUATION_CONFIG,
    RAGAS_LLM_CONFIG,
    METRICS,
    CHARTS_DIR,
    REPORTS_DIR,
    LOGGING_CONFIG
)
from services.evaluation.data_collector import DataCollector
from services.evaluation.ragas_evaluator import RagasEvaluator
from services.evaluation.analyzer import ResultAnalyzer
from services.evaluation.visualizer import Visualizer
from services.evaluation.report_generator import ReportGenerator


def setup_logging():
    """로깅 설정"""
    logging.basicConfig(
        level=getattr(logging, LOGGING_CONFIG.get('level', 'INFO')),
        format=LOGGING_CONFIG.get('format'),
        handlers=[
            logging.FileHandler(LOGGING_CONFIG.get('file')),
            logging.StreamHandler(sys.stdout)
        ]
    )
    return logging.getLogger(__name__)


def print_banner():
    """배너 출력"""
    print("\n" + "="*70)
    print(" "*20 + "RAGAS 평가 파이프라인")
    print(" "*15 + "RAG System Performance Evaluation")
    print("="*70 + "\n")


async def main(use_sample=False, skip_collection=False, dataset_path=None):
    """
    전체 파이프라인 실행

    Args:
        use_sample: 샘플 데이터셋 사용 여부
        skip_collection: 데이터 수집 스킵 (이미 수집된 데이터 사용)
        dataset_path: 사용자 지정 데이터셋 경로
    """
    logger = setup_logging()
    print_banner()

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    # 데이터셋 선택
    if dataset_path:
        selected_dataset = dataset_path
    else:
        selected_dataset = SAMPLE_DATASET_PATH if use_sample else DATASET_PATH

    logger.info(f"평가 시작: {selected_dataset}")
    print(f"[+] 데이터셋: {selected_dataset}")
    print(f"[+] 샘플 모드: {'예 (10개 케이스)' if use_sample else '아니오 (전체)'}")
    print(f"[+] RAG API: {RAG_API_URL}")
    print(f"\n{'='*70}\n")

    try:
        # ==========================================
        # Step 1: 데이터 수집 (RAG 호출)
        # ==========================================
        if not skip_collection:
            print(f"{'='*70}")
            print(f"[1/5] RAG 시스템 호출 및 데이터 수집")
            print(f"{'='*70}\n")

            collector = DataCollector(
                dataset_path=selected_dataset,
                rag_api_url=RAG_API_URL,
                rag_config=RAG_CONFIG,
                evaluation_config=EVALUATION_CONFIG
            )

            completed_dataset_path, error_count = await collector.collect()

            if error_count > 0:
                print(f"\n[WARNING] {error_count}개 케이스 실패. 계속 진행하시겠습니까? (y/n): ", end='')
                if input().lower() != 'y':
                    logger.warning("사용자가 평가를 중단했습니다")
                    return 1

        else:
            print(f"[1/5] 데이터 수집 스킵 (기존 데이터 사용)")
            # 가장 최근 완성된 데이터셋 찾기
            completed_dataset_path = dataset_path or selected_dataset

        # ==========================================
        # Step 2: RAGAS 평가
        # ==========================================
        print(f"\n{'='*70}")
        print(f"[2/5] RAGAS 메트릭 계산")
        print(f"{'='*70}\n")

        evaluator = RagasEvaluator(metrics=METRICS, llm_config=RAGAS_LLM_CONFIG)
        results_path, results_dict = evaluator.evaluate(completed_dataset_path)

        # ==========================================
        # Step 3: 결과 분석
        # ==========================================
        print(f"\n{'='*70}")
        print(f"[3/5] 결과 분석")
        print(f"{'='*70}\n")

        analyzer = ResultAnalyzer(results_path, completed_dataset_path)

        # 주요 분석 결과 출력
        print("\n📊 응급도별 성능:")
        ktas_analysis = analyzer.analyze_by_ktas_level()
        for level in sorted(ktas_analysis.keys()):
            data = ktas_analysis[level]
            print(f"  Level {level}: "
                  f"Faithfulness={data.get('faithfulness', 0):.3f}, "
                  f"Correctness={data.get('answer_correctness', 0):.3f}")

        print("\n📊 난이도별 성능:")
        difficulty_analysis = analyzer.analyze_by_difficulty()
        for diff in ['easy', 'medium', 'hard']:
            if diff in difficulty_analysis:
                data = difficulty_analysis[diff]
                print(f"  {diff.capitalize():6s}: "
                      f"Correctness={data.get('answer_correctness', 0):.3f}")

        # 약점 케이스
        weak_cases = analyzer.find_weak_cases(threshold=0.7)
        print(f"\n[WARNING]  개선 필요 케이스: {len(weak_cases)}개")

        # ==========================================
        # Step 4: 시각화
        # ==========================================
        print(f"\n{'='*70}")
        print(f"[4/5] 차트 생성")
        print(f"{'='*70}\n")

        visualizer = Visualizer(analyzer)

        charts = visualizer.create_all_charts(
            output_dir=CHARTS_DIR,
            prefix=f"{timestamp}_"
        )

        # ==========================================
        # Step 5: 리포트 생성
        # ==========================================
        print(f"\n{'='*70}")
        print(f"[5/5] 발표 리포트 생성")
        print(f"{'='*70}\n")

        report_gen = ReportGenerator(analyzer, charts)
        report_path = report_gen.generate_report(
            save_path=REPORTS_DIR / f"report_{timestamp}.md"
        )

        # ==========================================
        # 최종 요약
        # ==========================================
        print(f"\n{'='*70}")
        print(f"[OK] 평가 완료!")
        print(f"{'='*70}\n")

        print(f"[INFO] 최종 결과:")
        print(f"  - 총 케이스: {results_dict['total_cases']}개")
        print(f"  - 평균 점수: {sum(results_dict['metrics'].values()) / len(results_dict['metrics']):.4f}")
        print(f"\n[FILES] 생성된 파일:")
        print(f"  - RAGAS 결과: {results_path}")
        print(f"  - 레이더 차트: {charts.get('radar', 'N/A')}")
        print(f"  - 응급도 차트: {charts.get('ktas', 'N/A')}")
        print(f"  - 카테고리 히트맵: {charts.get('category', 'N/A')}")
        print(f"  - 난이도 차트: {charts.get('difficulty', 'N/A')}")
        print(f"  - 발표 리포트: {report_path}")

        print(f"\n{'='*70}")
        print(f"💡 다음 단계:")
        print(f"  1. 리포트 확인: {report_path}")
        print(f"  2. 차트를 발표 자료에 삽입")
        print(f"  3. 약점 케이스 분석 및 개선")
        print(f"{'='*70}\n")

        logger.info("평가 파이프라인 완료")
        return 0

    except KeyboardInterrupt:
        print(f"\n\n[WARNING]  사용자가 평가를 중단했습니다")
        logger.warning("평가가 중단되었습니다")
        return 1

    except Exception as e:
        print(f"\n\n[ERROR] 에러 발생: {e}")
        logger.error(f"평가 실패: {e}", exc_info=True)

        print(f"\n[INFO] 가능한 원인:")
        print(f"  1. RAG 서버가 실행 중이지 않음")
        print(f"  2. OPENAI_API_KEY 환경변수가 설정되지 않음")
        print(f"  3. contexts가 비어있음 (RAG 서비스 수정 필요)")
        print(f"  4. 네트워크 연결 문제")

        return 1


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="RAGAS 평가 파이프라인 실행",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
사용 예시:
  # 샘플 데이터로 빠른 테스트 (10개 케이스)
  python run_evaluation.py --sample

  # 전체 데이터로 평가 (282개 케이스)
  python run_evaluation.py

  # 이미 수집된 데이터로 평가만 실행
  python run_evaluation.py --skip-collection --dataset path/to/dataset.json

  # 사용자 지정 데이터셋으로 평가
  python run_evaluation.py --dataset ../data_ver2/ragas_by_category/15세미만_일반_의미문장추가.json
        """
    )

    parser.add_argument(
        "--sample",
        action="store_true",
        help="샘플 데이터셋 사용 (10개 케이스, 빠른 테스트용)"
    )

    parser.add_argument(
        "--skip-collection",
        action="store_true",
        help="데이터 수집 스킵 (이미 수집된 데이터 사용)"
    )

    parser.add_argument(
        "--dataset",
        type=str,
        help="사용자 지정 데이터셋 경로"
    )

    args = parser.parse_args()

    # 실행 (async)
    import asyncio
    exit_code = asyncio.run(main(
        use_sample=args.sample,
        skip_collection=args.skip_collection,
        dataset_path=args.dataset
    ))

    sys.exit(exit_code)
