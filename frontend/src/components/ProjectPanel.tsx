import type { Project } from '../types/api';
import './ProjectPanel.css';

interface ProjectPanelProps {
  project?: Project | null;
  quickQuestions: string[];
  onQuestionClick: (question: string) => void;
}

const formatArea = (value?: number | null) => {
  if (value === undefined || value === null) return '-';
  return `${value.toLocaleString()}㎡`;
};

const formatParking = (value?: number | null) => {
  if (value === undefined || value === null) return '-';
  return `${value}대`;
};

const ProjectPanel = ({ project, quickQuestions, onQuestionClick }: ProjectPanelProps) => {
  const projectName = project?.name ?? '프로젝트를 선택하세요';
  return (
    <div className="project-panel">
      <div className="project-card">
        <div className="project-card-header">
          <h3>현재 프로젝트</h3>
          <p className="project-title">{projectName}</p>
          {project?.status && <span className="project-status">{project.status}</span>}
        </div>
        <div className="project-details">
          <div className="detail-row">
            <span className="detail-label">위치</span>
            <span className="detail-value">{project?.location ?? '-'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">지역/지구</span>
            <span className="detail-value">{project?.zoning ?? '-'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">용도</span>
            <span className="detail-value">{project?.usage ?? '-'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">연면적</span>
            <span className="detail-value">{formatArea(project?.totalFloorArea)}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">층수</span>
            <span className="detail-value">{project?.floors ?? '-'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">주차</span>
            <span className="detail-value">{formatParking(project?.parkingSpaces)}</span>
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

