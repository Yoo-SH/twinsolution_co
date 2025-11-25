import './RecommendationPanel.css';

interface Recommendation {
  icon: string;
  title: string;
  chunkSize: string;
  overlap: string;
}

const RecommendationPanel = () => {
  const recommendations: Recommendation[] = [
    {
      icon: '⚖️',
      title: '법률 문서',
      chunkSize: '800-1200',
      overlap: '150-200',
    },
    {
      icon: '📖',
      title: '기술 가이드',
      chunkSize: '1000-1500',
      overlap: '200-300',
    },
    {
      icon: '📄',
      title: '양식 문서',
      chunkSize: '500-800',
      overlap: '100-150',
    },
  ];

  const tips = [
    '• 법률 문서는 조문 단위로 분할',
    '• 가이드는 섹션별 분할 권장',
    '• 오버랩은 청크 크기의 15-20% 권장',
    '• 성능 문제 시 청크 크기 늘리세요',
  ];

  return (
    <div className="recommendation-container">
      <div className="recommendation-panel">
        <h3 className="recommendation-title">문서 유형별 권장 설정</h3>
        <div className="recommendation-list">
          {recommendations.map((rec, index) => (
            <div key={index} className="recommendation-card">
              <div className="rec-card-header">
                <span className="rec-icon">{rec.icon}</span>
                <span className="rec-title">{rec.title}</span>
              </div>
              <div className="rec-card-content">
                <p>Chunk Size : {rec.chunkSize}</p>
                <p>Overlap : {rec.overlap}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="tips-panel">
        <div className="tips-header">
          <span className="tip-icon">💡</span>
          <span className="tip-title">팁</span>
        </div>
        <ul className="tips-list">
          {tips.map((tip, index) => (
            <li key={index}>{tip}</li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default RecommendationPanel;

