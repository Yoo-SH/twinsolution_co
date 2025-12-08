"""평가 결과 분석"""

import json
import pandas as pd
from typing import Dict, List, Any


class ResultAnalyzer:
    """RAGAS 평가 결과 분석"""

    def __init__(self, results_path, dataset_path):
        """
        Args:
            results_path: RAGAS 결과 파일 경로
            dataset_path: 완성된 데이터셋 경로 (metadata 포함)
        """
        # RAGAS 결과 로드
        with open(results_path, 'r', encoding='utf-8') as f:
            self.results = json.load(f)

        # 원본 데이터셋 로드 (metadata 포함)
        with open(dataset_path, 'r', encoding='utf-8') as f:
            self.dataset = json.load(f)

        # DataFrame 생성
        self.df = pd.DataFrame(self.results["per_case_results"])

        # metadata 추가
        if self.dataset:
            self.df['ktas_level'] = [d['metadata']['ktas_level'] for d in self.dataset]
            self.df['age_group'] = [d['metadata']['age_group'] for d in self.dataset]
            self.df['body_system'] = [d['metadata']['body_system'] for d in self.dataset]
            self.df['difficulty'] = [d['metadata']['difficulty'] for d in self.dataset]
            self.df['case_id'] = [d.get('id', f'case_{i}') for i, d in enumerate(self.dataset)]

    def get_overall_metrics(self) -> Dict[str, float]:
        """전체 평균 메트릭 반환"""
        return self.results["metrics"]

    def analyze_by_ktas_level(self) -> Dict[int, Dict[str, float]]:
        """응급도별 성능 분석"""
        metric_columns = [col for col in self.df.columns
                          if col in ['faithfulness', 'answer_relevancy',
                                     'context_precision', 'context_recall',
                                     'answer_correctness']]

        grouped = self.df.groupby('ktas_level')[metric_columns].mean()

        return grouped.to_dict('index')

    def analyze_by_age_group(self) -> Dict[str, Dict[str, float]]:
        """나이 그룹별 성능 분석"""
        metric_columns = [col for col in self.df.columns
                          if col in ['faithfulness', 'answer_relevancy',
                                     'context_precision', 'context_recall',
                                     'answer_correctness']]

        grouped = self.df.groupby('age_group')[metric_columns].mean()

        return grouped.to_dict('index')

    def analyze_by_category(self) -> Dict[str, Dict[str, float]]:
        """신체계통별 성능 분석"""
        # 실제로 존재하는 메트릭만 사용
        metric_columns = [col for col in self.df.columns
                          if col in ['faithfulness', 'answer_relevancy',
                                     'context_precision', 'context_recall',
                                     'answer_correctness']]

        if not metric_columns:
            return {}

        grouped = self.df.groupby('body_system')[metric_columns].mean()

        return grouped.to_dict('index')

    def analyze_by_difficulty(self) -> Dict[str, Dict[str, float]]:
        """난이도별 성능 분석"""
        metric_columns = [col for col in self.df.columns
                          if col in ['faithfulness', 'answer_relevancy',
                                     'answer_correctness']]

        grouped = self.df.groupby('difficulty')[metric_columns].mean()

        # 난이도 순서 정렬
        difficulty_order = {'easy': 0, 'medium': 1, 'hard': 2}
        sorted_result = dict(sorted(
            grouped.to_dict('index').items(),
            key=lambda x: difficulty_order.get(x[0], 999)
        ))

        return sorted_result

    def find_weak_cases(self, metric='answer_correctness', threshold=0.7) -> List[Dict[str, Any]]:
        """
        성능 낮은 케이스 찾기

        Args:
            metric: 기준 메트릭
            threshold: 임계값 (이하인 케이스 반환)

        Returns:
            약점 케이스 리스트
        """
        if metric not in self.df.columns:
            return []

        weak_indices = self.df[self.df[metric] < threshold].index.tolist()

        weak_cases = []
        for idx in weak_indices:
            case_data = {
                "id": self.df.loc[idx, 'case_id'] if 'case_id' in self.df.columns else f"case_{idx}",
                "score": float(self.df.loc[idx, metric]),
                "metrics": {}
            }

            # 모든 메트릭 점수 추가
            for m in ['faithfulness', 'answer_relevancy', 'context_precision',
                      'context_recall', 'answer_correctness']:
                if m in self.df.columns:
                    case_data["metrics"][m] = float(self.df.loc[idx, m])

            # metadata 추가
            if idx < len(self.dataset):
                case_data["question"] = self.dataset[idx]["question"][:100] + "..."
                case_data["metadata"] = self.dataset[idx]["metadata"]

            weak_cases.append(case_data)

        # 점수 낮은 순으로 정렬
        weak_cases.sort(key=lambda x: x["score"])

        return weak_cases

    def find_strong_cases(self, metric='answer_correctness', threshold=0.9) -> List[Dict[str, Any]]:
        """
        성능 높은 케이스 찾기

        Args:
            metric: 기준 메트릭
            threshold: 임계값 (이상인 케이스 반환)

        Returns:
            우수 케이스 리스트
        """
        if metric not in self.df.columns:
            return []

        strong_indices = self.df[self.df[metric] >= threshold].index.tolist()

        strong_cases = []
        for idx in strong_indices:
            case_data = {
                "id": self.df.loc[idx, 'case_id'] if 'case_id' in self.df.columns else f"case_{idx}",
                "score": float(self.df.loc[idx, metric]),
                "metrics": {}
            }

            # 모든 메트릭 점수 추가
            for m in ['faithfulness', 'answer_relevancy', 'context_precision',
                      'context_recall', 'answer_correctness']:
                if m in self.df.columns:
                    case_data["metrics"][m] = float(self.df.loc[idx, m])

            # metadata 추가
            if idx < len(self.dataset):
                case_data["question"] = self.dataset[idx]["question"][:100] + "..."
                case_data["metadata"] = self.dataset[idx]["metadata"]

            strong_cases.append(case_data)

        # 점수 높은 순으로 정렬
        strong_cases.sort(key=lambda x: x["score"], reverse=True)

        return strong_cases

    def get_summary_statistics(self) -> Dict[str, Any]:
        """요약 통계"""
        metric_columns = [col for col in self.df.columns
                          if col in ['faithfulness', 'answer_relevancy',
                                     'context_precision', 'context_recall',
                                     'answer_correctness']]

        stats = {
            "total_cases": len(self.df),
            "metrics": {}
        }

        for metric in metric_columns:
            stats["metrics"][metric] = {
                "mean": float(self.df[metric].mean()),
                "std": float(self.df[metric].std()),
                "min": float(self.df[metric].min()),
                "max": float(self.df[metric].max()),
                "median": float(self.df[metric].median())
            }

        return stats


# 독립 실행용
if __name__ == "__main__":
    import sys
    import argparse

    parser = argparse.ArgumentParser(description="RAGAS 결과 분석")
    parser.add_argument("results", help="RAGAS 결과 파일 경로")
    parser.add_argument("dataset", help="완성된 데이터셋 경로")
    args = parser.parse_args()

    # 분석 실행
    analyzer = ResultAnalyzer(args.results, args.dataset)

    # 전체 메트릭
    print("\n=== 전체 평균 메트릭 ===")
    for metric, score in analyzer.get_overall_metrics().items():
        print(f"{metric:20s}: {score:.4f}")

    # 응급도별 분석
    print("\n=== 응급도별 성능 ===")
    ktas_analysis = analyzer.analyze_by_ktas_level()
    for level, data in sorted(ktas_analysis.items()):
        print(f"\nLevel {level}:")
        for metric, score in data.items():
            print(f"  {metric:20s}: {score:.4f}")

    # 약점 케이스
    print("\n=== 개선 필요 케이스 (Answer Correctness < 0.7) ===")
    weak_cases = analyzer.find_weak_cases(threshold=0.7)
    print(f"총 {len(weak_cases)}개")
    for case in weak_cases[:5]:
        print(f"\n- {case['id']}: {case['score']:.4f}")
        print(f"  {case['question']}")
