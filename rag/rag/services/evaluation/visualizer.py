"""평가 결과 시각화"""

import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from pathlib import Path
import logging

# 한글 폰트 설정 (Windows)
try:
    plt.rcParams['font.family'] = 'Malgun Gothic'  # Windows
    plt.rcParams['axes.unicode_minus'] = False
except:
    try:
        plt.rcParams['font.family'] = 'AppleGothic'  # macOS
        plt.rcParams['axes.unicode_minus'] = False
    except:
        # 한글 폰트가 없으면 기본 폰트 사용
        pass


class Visualizer:
    """평가 결과 시각화"""

    def __init__(self, analyzer):
        """
        Args:
            analyzer: ResultAnalyzer 인스턴스
        """
        self.analyzer = analyzer
        self.logger = logging.getLogger(__name__)

        # 색상 팔레트
        self.colors = sns.color_palette("Set2", 8)

    def create_radar_chart(self, save_path):
        """5개 메트릭 레이더 차트 생성"""
        metrics = self.analyzer.get_overall_metrics()

        categories = list(metrics.keys())
        values = list(metrics.values())

        # 레이더 차트를 위한 각도 계산
        angles = np.linspace(0, 2 * np.pi, len(categories), endpoint=False).tolist()
        values += values[:1]  # 닫힌 다각형을 위해 첫 값 추가
        angles += angles[:1]

        # 그래프 생성
        fig, ax = plt.subplots(figsize=(10, 10), subplot_kw=dict(projection='polar'))

        # 데이터 플롯
        ax.plot(angles, values, 'o-', linewidth=2, label='RAG System', color=self.colors[0])
        ax.fill(angles, values, alpha=0.25, color=self.colors[0])

        # 축 설정
        ax.set_xticks(angles[:-1])
        ax.set_xticklabels(categories, size=12)
        ax.set_ylim(0, 1)
        ax.set_yticks([0.2, 0.4, 0.6, 0.8, 1.0])
        ax.set_yticklabels(['0.2', '0.4', '0.6', '0.8', '1.0'])

        # 그리드
        ax.grid(True, linestyle='--', alpha=0.7)

        # 제목
        ax.set_title('RAGAS Metrics Overview', size=16, pad=20, weight='bold')

        # 범례
        ax.legend(loc='upper right', bbox_to_anchor=(1.3, 1.1))

        # 저장
        plt.tight_layout()
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.close()

        self.logger.info(f"레이더 차트 생성: {save_path}")
        print(f"📊 레이더 차트 저장: {save_path}")

    def create_ktas_performance_chart(self, save_path):
        """응급도별 성능 바 차트 생성"""
        ktas_data = self.analyzer.analyze_by_ktas_level()

        if not ktas_data:
            self.logger.warning("응급도별 데이터가 없습니다")
            return

        levels = sorted(ktas_data.keys())
        metrics = ['faithfulness', 'answer_correctness', 'context_recall']

        x = np.arange(len(levels))
        width = 0.25

        fig, ax = plt.subplots(figsize=(12, 7))

        # 각 메트릭별 바 생성
        for i, metric in enumerate(metrics):
            values = [ktas_data[level].get(metric, 0) for level in levels]
            ax.bar(x + i*width, values, width, label=metric.replace('_', ' ').title(),
                   color=self.colors[i])

            # 값 표시
            for j, v in enumerate(values):
                ax.text(x[j] + i*width, v + 0.02, f'{v:.3f}',
                        ha='center', va='bottom', fontsize=9)

        # 축 설정
        ax.set_xlabel('KTAS Level', fontsize=12, weight='bold')
        ax.set_ylabel('Score', fontsize=12, weight='bold')
        ax.set_title('Performance by KTAS Level', fontsize=14, weight='bold', pad=15)
        ax.set_xticks(x + width)
        ax.set_xticklabels([f'Level {l}' for l in levels])
        ax.set_ylim(0, 1.1)
        ax.legend(loc='upper left', framealpha=0.9)
        ax.grid(axis='y', linestyle='--', alpha=0.3)

        # 저장
        plt.tight_layout()
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.close()

        self.logger.info(f"응급도별 차트 생성: {save_path}")
        print(f"📊 응급도별 차트 저장: {save_path}")

    def create_category_heatmap(self, save_path):
        """신체계통별 히트맵 생성"""
        category_data = self.analyzer.analyze_by_category()

        if not category_data:
            self.logger.warning("카테고리별 데이터가 없습니다")
            return

        categories = list(category_data.keys())
        metrics = ['faithfulness', 'answer_correctness']

        # 데이터 행렬 생성
        data = np.array([
            [category_data[cat].get(metric, 0) for metric in metrics]
            for cat in categories
        ])

        # 그래프 크기 조정 (카테고리 수에 따라)
        fig_height = max(8, len(categories) * 0.4)
        fig, ax = plt.subplots(figsize=(10, fig_height))

        # 히트맵 생성
        sns.heatmap(
            data,
            annot=True,
            fmt='.3f',
            xticklabels=[m.replace('_', ' ').title() for m in metrics],
            yticklabels=categories,
            cmap='YlOrRd',
            vmin=0, vmax=1,
            cbar_kws={'label': 'Score'},
            ax=ax
        )

        # 제목
        ax.set_title('Performance by Body System Category', fontsize=14, weight='bold', pad=15)

        # 저장
        plt.tight_layout()
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.close()

        self.logger.info(f"카테고리 히트맵 생성: {save_path}")
        print(f"📊 카테고리 히트맵 저장: {save_path}")

    def create_difficulty_chart(self, save_path):
        """난이도별 성능 차트 생성"""
        difficulty_data = self.analyzer.analyze_by_difficulty()

        if not difficulty_data:
            self.logger.warning("난이도별 데이터가 없습니다")
            return

        difficulties = list(difficulty_data.keys())
        metrics = ['faithfulness', 'answer_relevancy', 'answer_correctness']

        x = np.arange(len(difficulties))
        width = 0.25

        fig, ax = plt.subplots(figsize=(10, 6))

        # 각 메트릭별 바 생성
        for i, metric in enumerate(metrics):
            values = [difficulty_data[diff].get(metric, 0) for diff in difficulties]
            ax.bar(x + i*width, values, width, label=metric.replace('_', ' ').title(),
                   color=self.colors[i])

            # 값 표시
            for j, v in enumerate(values):
                ax.text(x[j] + i*width, v + 0.02, f'{v:.3f}',
                        ha='center', va='bottom', fontsize=9)

        # 축 설정
        ax.set_xlabel('Difficulty Level', fontsize=12, weight='bold')
        ax.set_ylabel('Score', fontsize=12, weight='bold')
        ax.set_title('Performance by Difficulty', fontsize=14, weight='bold', pad=15)
        ax.set_xticks(x + width)
        ax.set_xticklabels([d.capitalize() for d in difficulties])
        ax.set_ylim(0, 1.1)
        ax.legend(loc='upper left', framealpha=0.9)
        ax.grid(axis='y', linestyle='--', alpha=0.3)

        # 저장
        plt.tight_layout()
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.close()

        self.logger.info(f"난이도별 차트 생성: {save_path}")
        print(f"📊 난이도별 차트 저장: {save_path}")

    def create_all_charts(self, output_dir, prefix=""):
        """모든 차트 생성"""
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)

        charts = {}

        # 레이더 차트
        radar_path = output_dir / f"{prefix}radar_chart.png"
        self.create_radar_chart(radar_path)
        charts['radar'] = str(radar_path)

        # 응급도별 차트
        ktas_path = output_dir / f"{prefix}ktas_performance.png"
        self.create_ktas_performance_chart(ktas_path)
        charts['ktas'] = str(ktas_path)

        # 카테고리 히트맵
        category_path = output_dir / f"{prefix}category_heatmap.png"
        self.create_category_heatmap(category_path)
        charts['category'] = str(category_path)

        # 난이도별 차트
        difficulty_path = output_dir / f"{prefix}difficulty_performance.png"
        self.create_difficulty_chart(difficulty_path)
        charts['difficulty'] = str(difficulty_path)

        return charts


# 독립 실행용
if __name__ == "__main__":
    import sys
    import argparse
    from analyzer import ResultAnalyzer

    parser = argparse.ArgumentParser(description="RAGAS 결과 시각화")
    parser.add_argument("results", help="RAGAS 결과 파일 경로")
    parser.add_argument("dataset", help="완성된 데이터셋 경로")
    parser.add_argument("--output-dir", help="차트 저장 디렉토리", default="./charts")
    args = parser.parse_args()

    # 분석기 생성
    analyzer = ResultAnalyzer(args.results, args.dataset)

    # 시각화
    visualizer = Visualizer(analyzer)
    charts = visualizer.create_all_charts(args.output_dir)

    print(f"\n✅ 모든 차트 생성 완료!")
    for chart_type, path in charts.items():
        print(f"  - {chart_type}: {path}")
