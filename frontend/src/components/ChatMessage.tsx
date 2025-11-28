import './ChatMessage.css';
import TypingIndicator from './TypingIndicator';

interface ChatMessageProps {
  role: 'assistant' | 'user';
  message: string;
  timestamp: string;
  source?: string;
  isStreaming?: boolean;
  isWaiting?: boolean;
}

const ChatMessage = ({ role, message, timestamp, source, isStreaming, isWaiting }: ChatMessageProps) => {
  const type = role === 'assistant' ? 'ai' : 'user';

  // 응답 대기중이면 점만 표시
  if (isWaiting) {
    return <TypingIndicator />;
  }

  return (
    <div className={`chat-message ${type}-message`}>
      <div className="message-header">
        <div className="message-avatar">
          {type === 'ai' ? (
            <img src="/tm-logo.png" alt="TM" className="avatar-logo" />
          ) : (
            '👤'
          )}
        </div>
        <div className="message-info">
          <span className="message-sender">
            {type === 'ai' ? 'AI 어시스턴트' : '사용자'}
          </span>
        </div>
      </div>
      <div className="message-content">
        <p className="message-text">
          {message}
          {isStreaming && <span className="streaming-cursor">▊</span>}
        </p>
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

