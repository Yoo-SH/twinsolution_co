import './ChatMessage.css';

interface ChatMessageProps {
  role: 'assistant' | 'user';
  message: string;
  timestamp: string;
  source?: string;
}

const ChatMessage = ({ role, message, timestamp, source }: ChatMessageProps) => {
  const type = role === 'assistant' ? 'ai' : 'user';

  return (
    <div className={`chat-message ${type}-message`}>
      <div className="message-header">
        <div className="message-avatar">
          {type === 'ai' ? '🤖' : '👤'}
        </div>
        <div className="message-info">
          <span className="message-sender">
            {type === 'ai' ? 'AI 어시스턴트' : '사용자'}
          </span>
        </div>
      </div>
      <div className="message-content">
        <p className="message-text">{message}</p>
        <div className="message-meta">
          <span className="message-time">{timestamp}</span>
          {source && (
            <span className="message-source">
              출처: 📄 <span className="source-link">{source}</span>
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

export default ChatMessage;

