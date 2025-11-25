import { useState } from 'react';
import './SeparatorManager.css';

interface SeparatorManagerProps {
  separators: string[];
  onAdd: (separator: string) => void;
  onRemove: (index: number) => void;
}

const SeparatorManager = ({ separators, onAdd, onRemove }: SeparatorManagerProps) => {
  const [newSeparator, setNewSeparator] = useState('');

  const handleAdd = () => {
    if (newSeparator.trim()) {
      onAdd(newSeparator);
      setNewSeparator('');
    }
  };

  return (
    <div className="separator-manager">
      <label className="separator-label">구분자 (Separators)</label>
      
      <div className="separator-list">
        {separators.map((sep, index) => (
          <div key={index} className="separator-item">
            <input
              type="text"
              className="separator-input"
              value={sep}
              readOnly
            />
            <button
              className="separator-delete-btn"
              onClick={() => onRemove(index)}
              title="삭제"
            >
              🗑️
            </button>
          </div>
        ))}
        
        <button className="separator-add-btn" onClick={handleAdd}>
          + 구분자 추가
        </button>
      </div>

      <p className="separator-description">
        문서 파싱 시 청크를 나누는 기준 문자입니다. 먼저 입력한 구분자부터 적용됩니다.
      </p>
    </div>
  );
};

export default SeparatorManager;

