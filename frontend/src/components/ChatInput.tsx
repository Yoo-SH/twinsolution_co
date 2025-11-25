import { useState } from 'react';
import './ChatInput.css';

interface ChatInputProps {
  onSend: (message: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

const ChatInput = ({
  onSend,
  placeholder = '건축 허가, 법령, 서류에 대해 질문하세요..',
  disabled = false,
}: ChatInputProps) => {
  const [message, setMessage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (disabled) {
      return;
    }
    if (message.trim()) {
      onSend(message);
      setMessage('');
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <form className="chat-input-container" onSubmit={handleSubmit}>
      <button type="button" className="attach-btn" disabled={disabled}>
        📎
      </button>
      <input
        type="text"
        className="chat-input"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyPress={handleKeyPress}
        placeholder={placeholder}
        disabled={disabled}
      />
      <button
        type="submit"
        className="send-btn"
        disabled={disabled || !message.trim()}
      >
        ➤
      </button>
    </form>
  );
};

export default ChatInput;

