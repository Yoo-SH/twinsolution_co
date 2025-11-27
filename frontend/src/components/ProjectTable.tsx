import type { Project } from '../types/api';
import './ProjectTable.css';

interface ProjectTableProps {
  projects: Project[];
  loading?: boolean;
  emptyMessage?: string;
}

const formatDate = (value?: string | null) => {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  } catch {
    return value;
  }
};

const ProjectTable = ({
  projects,
  loading = false,
  emptyMessage = '최근 프로젝트가 없습니다.',
}: ProjectTableProps) => {
  if (loading) {
    return (
      <div className="project-table-container">
        <h3 className="table-title">최근 프로젝트</h3>
        <div className="project-table loading">데이터를 불러오는 중...</div>
      </div>
    );
  }

  if (!projects.length) {
    return (
      <div className="project-table-container">
        <h3 className="table-title">최근 프로젝트</h3>
        <div className="project-table empty-state">{emptyMessage}</div>
      </div>
    );
  }

const getStatusClass = (status: string) => {
  switch (status) {
    case '승인 대기':
    case '승인대기':
      return 'status-pending';
    case '서류 검토':
    case '서류검토':
    case '설계 진행':
    case '설계진행':
    case '진행중':
      return 'status-progress';
    case '초기 단계':
    case '초기단계':
      return 'status-initial';
    case '완료':
      return 'status-completed';
    default:
      return '';
  }
};

  return (
    <div className="project-table-container">
      <h3 className="table-title">최근 프로젝트</h3>
      <div className="project-table">
        <div className="table-header">
          <div className="header-cell">프로젝트명</div>
          <div className="header-cell">상태</div>
          <div className="header-cell">날짜</div>
          <div className="header-cell">진행률</div>
        </div>
        {projects.map((project) => (
          <div key={project.id} className="table-row">
            <div className="table-cell project-name">{project.name}</div>
            <div className="table-cell">
              <span className={`status-badge ${getStatusClass(project.status)}`}>
                {project.status}
              </span>
            </div>
            <div className="table-cell">{formatDate(project.createdAt)}</div>
            <div className="table-cell progress-cell">
              <div className="progress-bar">
                <div
                  className="progress-fill"
                  style={{ width: `${project.progress ?? 0}%` }}
                ></div>
              </div>
              <span className="progress-text">{project.progress ?? 0}%</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProjectTable;

