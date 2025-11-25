import { useState } from 'react';
import './ChatInput.css';

interface ChatInputProps {
  onSend: (message: string) => void;
  placeholder?: string;
}

const ChatInput = ({ onSend, placeholder = '건축 허가, 법령, 서류에 대해 질문하세요..' }: ChatInputProps) => {
  const [message, setMessage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
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
      <button type="button" className="attach-btn">
        📎
      </button>
      <input
        type="text"
        className="chat-input"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyPress={handleKeyPress}
        placeholder={placeholder}
      />
      <button type="submit" className="send-btn" disabled={!message.trim()}>
        ➤
      </button>
    </form>
  );
};

export default ChatInput;

