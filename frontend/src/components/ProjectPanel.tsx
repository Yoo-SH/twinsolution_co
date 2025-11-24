import './ProjectPanel.css';

interface ProjectInfo {
  location: string;
  zone: string;
  purpose: string;
  area: string;
  floors: string;
  parking: string;
}

interface ProjectPanelProps {
  projectInfo: ProjectInfo;
  quickQuestions: string[];
  onQuestionClick: (question: string) => void;
}

const ProjectPanel = ({ projectInfo, quickQuestions, onQuestionClick }: ProjectPanelProps) => {
  return (
    <div className="project-panel">
      <div className="project-card">
        <div className="project-card-header">
          <h3>현재 프로젝트</h3>
          <p className="project-title">전주 덕진구 건축</p>
        </div>
        <div className="project-details">
          <div className="detail-row">
            <span className="detail-label">위치</span>
            <span className="detail-value">{projectInfo.location}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">지역/지구</span>
            <span className="detail-value">{projectInfo.zone}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">용도</span>
            <span className="detail-value">{projectInfo.purpose}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">연면적</span>
            <span className="detail-value">{projectInfo.area}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">층수</span>
            <span className="detail-value">{projectInfo.floors}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">주차</span>
            <span className="detail-value">{projectInfo.parking}</span>
          </div>
        </div>
      </div>

      <div className="quick-questions">
        <h4 className="quick-questions-title">빠른 질문</h4>
        <div className="questions-list">
          {quickQuestions.map((question, index) => (
            <button
              key={index}
              className="question-btn"
              onClick={() => onQuestionClick(question)}
            >
              {question}
            </button>
          ))}
        </div>
      </div>

      <div className="action-buttons">
        <button className="panel-action-btn primary">
          📄 서류 생성
        </button>
        <button className="panel-action-btn secondary">
          📚 참고 자료
        </button>
      </div>
    </div>
  );
};

export default ProjectPanel;

