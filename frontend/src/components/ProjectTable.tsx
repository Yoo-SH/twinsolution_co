import './ProjectTable.css';

interface Project {
  id: number;
  name: string;
  status: string;
  date: string;
  progress: number;
}

const ProjectTable = () => {
  const projects: Project[] = [
    {
      id: 1,
      name: '전주 덕진구 근린생활시설',
      status: '승인 대기',
      date: '2024-11-12',
      progress: 85,
    },
    {
      id: 2,
      name: '서울 강남구 오피스텔',
      status: '서류 검토',
      date: '2024-11-10',
      progress: 60,
    },
    {
      id: 3,
      name: '부산 해운대 상가건물',
      status: '설계 진행',
      date: '2024-11-08',
      progress: 40,
    },
    {
      id: 4,
      name: '대전 유성구 주택',
      status: '초기 단계',
      date: '2024-11-05',
      progress: 20,
    },
  ];

  const getStatusClass = (status: string) => {
    switch (status) {
      case '승인 대기':
        return 'status-pending';
      case '서류 검토':
      case '설계 진행':
        return 'status-progress';
      case '초기 단계':
        return 'status-initial';
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
            <div className="table-cell">{project.date}</div>
            <div className="table-cell progress-cell">
              <div className="progress-bar">
                <div
                  className="progress-fill"
                  style={{ width: `${project.progress}%` }}
                ></div>
              </div>
              <span className="progress-text">{project.progress}%</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProjectTable;

