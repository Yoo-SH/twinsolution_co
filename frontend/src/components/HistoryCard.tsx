import './HistoryCard.css';

interface HistoryCardProps {
  title: string;
  description: string;
  date: string;
  project: string;
  messageCount: number;
  onPlay?: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
}

const HistoryCard = ({
  title,
  description,
  date,
  project,
  messageCount,
  onPlay,
  onEdit,
  onDelete,
}: HistoryCardProps) => {
  return (
    <div className="history-card">
      <div className="history-card-content">
        <div className="history-card-header">
          <h4 className="history-card-title">{title}</h4>
          <span className="message-count-badge">{messageCount}개 메시지</span>
        </div>
        <p className="history-card-description">{description}</p>
        <p className="history-card-meta">
          {date} • {project}
        </p>
      </div>
      <div className="history-card-actions">
        <button className="action-btn play-btn" onClick={onPlay} title="재생">
          ▶
        </button>
        <button className="action-btn edit-btn" onClick={onEdit} title="편집">
          ✏️
        </button>
        <button className="action-btn delete-btn" onClick={onDelete} title="삭제">
          🗑️
        </button>
      </div>
    </div>
  );
};

export default HistoryCard;

