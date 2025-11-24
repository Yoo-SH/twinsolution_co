import './APICard.css';

interface APICardProps {
  name: string;
  url: string;
  method: string;
  schedule: string;
  lastSync: string;
  nextSync: string;
  status: 'active' | 'error';
  category: string;
  errorMessage?: string;
  onTest: () => void;
  onRefresh: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

const APICard = ({
  name,
  url,
  method,
  schedule,
  lastSync,
  nextSync,
  status,
  category,
  errorMessage,
  onTest,
  onRefresh,
  onEdit,
  onDelete,
}: APICardProps) => {
  return (
    <div className={`api-card ${status === 'error' ? 'error' : ''}`}>
      <div className="api-card-header">
        <div className="api-header-left">
          <span className="api-icon">🔗</span>
          <h3 className="api-name">{name}</h3>
          <span className={`api-status-badge ${status}`}>
            {status === 'active' ? '활성' : '오류'}
          </span>
          <span className="api-category-badge">{category}</span>
        </div>
      </div>

      <div className="api-details">
        <div className="api-detail-row">
          <span className="detail-label">URL :</span>
          <a href={url} className="detail-value url-link" target="_blank" rel="noopener noreferrer">
            {url}
          </a>
        </div>
        <div className="api-detail-row">
          <span className="detail-label">Method :</span>
          <span className="detail-value method">{method}</span>
        </div>
        <div className="api-detail-row">
          <span className="detail-label">스케줄 :</span>
          <span className="detail-value">{schedule}</span>
        </div>
        <div className="api-detail-row">
          <span className="detail-label">마지막 동기화 :</span>
          <span className="detail-value">{lastSync}</span>
        </div>
        <div className="api-detail-row">
          <span className="detail-label">다음 동기화 :</span>
          <span className="detail-value">{nextSync}</span>
        </div>
      </div>

      {errorMessage && (
        <div className="api-error-message">
          <span className="error-icon">⚠️</span>
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="api-actions">
        <button className="api-action-btn test" onClick={onTest} title="연결 테스트">
          ▶
        </button>
        <button className="api-action-btn refresh" onClick={onRefresh} title="새로고침">
          🔄
        </button>
        <button className="api-action-btn edit" onClick={onEdit} title="편집">
          ✏️
        </button>
        <button className="api-action-btn delete" onClick={onDelete} title="삭제">
          🗑️
        </button>
      </div>
    </div>
  );
};

export default APICard;

