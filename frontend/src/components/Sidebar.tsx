import { Link, useLocation } from 'react-router-dom';
import './Sidebar.css';

const Sidebar = () => {
  const location = useLocation();

  const mainMenuItems = [
    { path: '/', label: '대시보드', icon: '📊' },
    { path: '/documents', label: '서류작성 AI', icon: '📄' },
  ];

  const ragMenuItems = [
    { path: '/rag', label: '문서 관리', icon: '💾' },
    { path: '/chunking', label: '청킹 설정', icon: '⚙️' },
    { path: '/api', label: 'API 연동', icon: '🔗' },
  ];

  const historyItem = { path: '/history', label: '히스토리', icon: '🕐' };

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="logo">
          <img src="/triplemahorlogo.png" alt="Triple Major Logo" className="logo-image" />
        </div>
        <div className="project-info">
          <h2>건축허가</h2>
          <p>자동화 시스템</p>
        </div>
        <button className="settings-btn">⚙️</button>
      </div>

      <nav className="sidebar-nav">
        {mainMenuItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </Link>
        ))}

        <div className="nav-section">
          <div className="nav-section-title">RAG 관리</div>
          {ragMenuItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
            >
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
            </Link>
          ))}
        </div>

        <Link
          to={historyItem.path}
          className={`nav-item ${location.pathname === historyItem.path ? 'active' : ''}`}
        >
          <span className="nav-icon">{historyItem.icon}</span>
          <span className="nav-label">{historyItem.label}</span>
        </Link>
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile">
          <div className="user-avatar">TM</div>
          <div className="user-info">
            <span className="user-name">트리플 메이저</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;

