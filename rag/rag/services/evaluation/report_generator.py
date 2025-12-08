"""발표용 마크다운 리포트 생성"""

from datetime import datetime
from pathlib import Path
import logging


class ReportGenerator:
    """RAGAS 평가 결과 발표용 리포트 생성"""

    def __init__(self, analyzer, charts=None):
        """
        Args:
            analyzer: ResultAnalyzer 인스턴스
            charts: 생성된 차트 경로 딕셔너리 (선택)
        """
        self.analyzer = analyzer
        self.charts = charts or {}
        self.logger = logging.getLogger(__name__)

    def generate_report(self, save_path):
        """발표용 마크다운 리포트 생성"""

        results = self.analyzer.results
        overall_metrics = self.analyzer.get_overall_metrics()
        ktas_analysis = self.analyzer.analyze_by_ktas_level()
        difficulty_analysis = self.analyzer.analyze_by_difficulty()
        age_analysis = self.analyzer.analyze_by_age_group()
        weak_cases = self.analyzer.find_weak_cases(threshold=0.7)
        strong_cases = self.analyzer.find_strong_cases(threshold=0.9)

        # 리포트 작성
        report = f"""# RAG 시스템 RAGAS 평가 리포트

**평가 일시**: {datetime.now().strftime("%Y년 %m월 %d일 %H:%M")}
**평가 케이스 수**: {results['total_cases']}개
**평가 프레임워크**: RAGAS (Retrieval-Augmented Generation Assessment)
**데이터셋**: {Path(results.get('dataset_path', '')).name}

---

## 📊 전체 평가 결과

### 종합 성능 메트릭

| 메트릭 | 점수 | 등급 | 설명 |
|--------|------|------|------|
"""

        # 메트릭별 점수 표
        for metric_name, score in overall_metrics.items():
            if score >= 0.9:
                grade = "⭐⭐⭐ 우수"
            elif score >= 0.8:
                grade = "⭐⭐ 양호"
            elif score >= 0.7:
                grade = "⭐ 보통"
            else:
                grade = "⚠️ 개선 필요"

            desc = self._get_metric_description(metric_name)
            report += f"| **{metric_name}** | {score:.4f} | {grade} | {desc} |\n"

        # 평균 점수
        avg_score = sum(overall_metrics.values()) / len(overall_metrics)
        report += f"\n**평균 점수**: {avg_score:.4f}\n\n"

        # 레이더 차트
        if 'radar' in self.charts:
            report += f"""### 시각화
![RAGAS Metrics Overview]({Path(self.charts['radar']).name})

---

"""

        # 응급도별 분석
        report += """## 🏥 응급도별 성능 분석

"""
        if ktas_analysis:
            report += "| KTAS 등급 | Faithfulness | Answer Correctness | Context Recall | 케이스 수 |\n"
            report += "|-----------|--------------|--------------------|-----------------|-----------|\n"

            for level in sorted(ktas_analysis.keys()):
                data = ktas_analysis[level]
                case_count = len([d for d in self.analyzer.dataset if d['metadata']['ktas_level'] == level])
                report += f"| Level {level} | {data.get('faithfulness', 0):.4f} | {data.get('answer_correctness', 0):.4f} | {data.get('context_recall', 0):.4f} | {case_count}개 |\n"

        if 'ktas' in self.charts:
            report += f"\n![응급도별 성능]({Path(self.charts['ktas']).name})\n\n"

        report += """
### 분석

"""
        # 응급도별 인사이트
        if ktas_analysis:
            level_1_correctness = ktas_analysis.get(1, {}).get('answer_correctness', 0)
            if level_1_correctness >= 0.8:
                report += f"- **1등급 (소생)**: Answer Correctness {level_1_correctness:.3f}로, 가장 위급한 상황에서도 높은 정확도 유지\n"
            else:
                report += f"- **1등급 (소생)**: Answer Correctness {level_1_correctness:.3f}로, 위급 상황에서의 정확도 개선 필요\n"

            level_2_correctness = ktas_analysis.get(2, {}).get('answer_correctness', 0)
            report += f"- **2등급 (응급)**: Answer Correctness {level_2_correctness:.3f}로, 응급 상황에 대한 적절한 대응\n"

        report += """
---

## 📈 난이도별 성능 분석

"""

        if difficulty_analysis:
            report += "| 난이도 | Faithfulness | Answer Relevancy | Answer Correctness |\n"
            report += "|--------|--------------|------------------|--------------------|  \n"

            for diff in ['easy', 'medium', 'hard']:
                if diff in difficulty_analysis:
                    data = difficulty_analysis[diff]
                    report += f"| {diff.capitalize()} | {data.get('faithfulness', 0):.4f} | {data.get('answer_relevancy', 0):.4f} | {data.get('answer_correctness', 0):.4f} |\n"

        if 'difficulty' in self.charts:
            report += f"\n![난이도별 성능]({Path(self.charts['difficulty']).name})\n\n"

        # 나이 그룹별 분석
        if age_analysis:
            report += """
---

## 👶👨 나이 그룹별 성능 분석

| 나이 그룹 | Faithfulness | Answer Correctness | 케이스 수 |
|-----------|--------------|--------------------|-----------|\n"""

            for age_group in sorted(age_analysis.keys()):
                data = age_analysis[age_group]
                case_count = len([d for d in self.analyzer.dataset if d['metadata']['age_group'] == age_group])
                report += f"| {age_group} | {data.get('faithfulness', 0):.4f} | {data.get('answer_correctness', 0):.4f} | {case_count}개 |\n"

            report += "\n"

        # 신체계통별 (히트맵)
        if 'category' in self.charts:
            report += f"""
---

## 🏥 신체계통별 성능 분석

![신체계통별 히트맵]({Path(self.charts['category']).name})

---

"""

        # 우수 케이스
        report += f"""
## ⭐ 우수 케이스 (Answer Correctness ≥ 0.9)

총 {len(strong_cases)}개 케이스

"""
        if strong_cases:
            for case in strong_cases[:3]:  # 상위 3개
                report += f"""
### {case['id']} (점수: {case['score']:.4f})
- **응급도**: KTAS {case['metadata']['ktas_level']}등급
- **카테고리**: {case['metadata']['body_system']} - {case['metadata']['main_symptom']}
- **메트릭**:
"""
                for metric, score in case['metrics'].items():
                    report += f"  - {metric}: {score:.4f}\n"

        # 약점 케이스
        report += f"""
---

## ⚠️ 개선 필요 케이스 (Answer Correctness < 0.7)

총 {len(weak_cases)}개 케이스

"""
        if weak_cases:
            for case in weak_cases[:5]:  # 하위 5개
                report += f"""
### {case['id']} (점수: {case['score']:.4f})
- **응급도**: KTAS {case['metadata']['ktas_level']}등급
- **카테고리**: {case['metadata']['body_system']} - {case['metadata']['main_symptom']}
- **질문**: {case['question']}
- **메트릭**:
"""
                for metric, score in case['metrics'].items():
                    report += f"  - {metric}: {score:.4f}\n"

        # 핵심 발견사항
        report += f"""
---

## 💡 핵심 발견 사항

1. **신뢰성 (Faithfulness)**: {overall_metrics.get('faithfulness', 0):.3f}
   - AI가 검색된 문서에 근거하여 답변하며, 환각(hallucination)이 {"매우 적습니다" if overall_metrics.get('faithfulness', 0) >= 0.9 else "있습니다"}

2. **정확성 (Answer Correctness)**: {overall_metrics.get('answer_correctness', 0):.3f}
   - 의료 정보 제공의 정확도가 {"우수합니다" if overall_metrics.get('answer_correctness', 0) >= 0.8 else "개선이 필요합니다"}

3. **검색 품질 (Context Recall)**: {overall_metrics.get('context_recall', 0):.3f}
   - 필요한 정보를 {"효과적으로" if overall_metrics.get('context_recall', 0) >= 0.8 else "부분적으로"} 검색합니다

4. **일관성**:
   - {"모든 응급도 등급에서 안정적인 성능을 유지합니다" if min(ktas_analysis[level].get('answer_correctness', 0) for level in ktas_analysis) >= 0.7 else "일부 응급도에서 성능 편차가 있습니다"}

---

## 🎯 발표 포인트

### 슬라이드 1: 평가 규모
> "{results['total_cases']}개의 실제 환자 케이스, 33개 의료 카테고리에 대해 RAGAS 평가를 수행했습니다."

### 슬라이드 2: 신뢰성
> "Faithfulness {overall_metrics.get('faithfulness', 0):.2f}로, AI가 근거 없는 답변(환각)을 {"거의 생성하지 않습니다" if overall_metrics.get('faithfulness', 0) >= 0.9 else "일부 생성합니다"}."

### 슬라이드 3: 정확성
> "의료 정보의 정확도는 Answer Correctness {overall_metrics.get('answer_correctness', 0):.2f}를 달성했습니다."

### 슬라이드 4: 위급 상황 대응
"""
        if ktas_analysis and 1 in ktas_analysis:
            level_1_score = ktas_analysis[1].get('answer_correctness', 0)
            report += f'> "KTAS 1등급(소생) 케이스에서 Answer Correctness {level_1_score:.2f}를 기록하여, 위급 상황에서도 {"신뢰할 수 있습니다" if level_1_score >= 0.8 else "개선이 필요합니다"}."'

        report += f"""

---

## 📝 권장 사항

"""

        # 개선 권장사항
        if overall_metrics.get('context_recall', 0) < 0.8:
            report += "1. **Context Recall 개선**: RAG 검색 파라미터(top_k, threshold) 조정 필요\n"

        if overall_metrics.get('faithfulness', 0) < 0.9:
            report += "2. **Faithfulness 향상**: 프롬프트 개선을 통해 문서 기반 답변 강화\n"

        if weak_cases and len(weak_cases) > 10:
            report += f"3. **약점 케이스 분석**: {len(weak_cases)}개의 낮은 성능 케이스에 대한 상세 분석 및 개선\n"

        report += """
---

**생성 일시**: {timestamp}
**평가 도구**: RAGAS v1.0
**프로젝트**: 2025 통합테스트베드 AI 경진대회 Season 2
""".format(timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

        # 리포트 저장
        save_path = Path(save_path)
        save_path.parent.mkdir(parents=True, exist_ok=True)

        with open(save_path, 'w', encoding='utf-8') as f:
            f.write(report)

        self.logger.info(f"리포트 생성: {save_path}")
        print(f"📄 리포트 저장: {save_path}")

        return str(save_path)

    def _get_metric_description(self, metric_name):
        """메트릭 설명 반환"""
        descriptions = {
            "faithfulness": "답변이 검색된 문서에 근거하는가 (환각 검증)",
            "answer_relevancy": "답변이 질문과 관련있는가",
            "context_precision": "검색된 문서가 정확한가",
            "context_recall": "필요한 문서를 모두 찾았는가",
            "answer_correctness": "답변이 정답과 일치하는가"
        }
        return descriptions.get(metric_name, "")


# 독립 실행용
if __name__ == "__main__":
    import sys
    import argparse
    from analyzer import ResultAnalyzer

    parser = argparse.ArgumentParser(description="RAGAS 리포트 생성")
    parser.add_argument("results", help="RAGAS 결과 파일 경로")
    parser.add_argument("dataset", help="완성된 데이터셋 경로")
    parser.add_argument("--output", help="리포트 저장 경로", default="./report.md")
    args = parser.parse_args()

    # 분석기 생성
    analyzer = ResultAnalyzer(args.results, args.dataset)

    # 리포트 생성
    report_gen = ReportGenerator(analyzer)
    report_path = report_gen.generate_report(args.output)

    print(f"\n✅ 리포트 생성 완료: {report_path}")
