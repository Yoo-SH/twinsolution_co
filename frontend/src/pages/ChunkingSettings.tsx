import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import Slider from '../components/Slider';
import SeparatorManager from '../components/SeparatorManager';
import RecommendationPanel from '../components/RecommendationPanel';
import './ChunkingSettings.css';

const ChunkingSettings = () => {
  const [chunkSize, setChunkSize] = useState(1000);
  const [chunkOverlap, setChunkOverlap] = useState(200);
  const [separators, setSeparators] = useState(['\n\n', '\n', '. ', ' ']);
  const [lengthMethod, setLengthMethod] = useState('문자 수 (Characters)');
  const [minChunkSize, setMinChunkSize] = useState(100);
  const [maxChunkSize, setMaxChunkSize] = useState(2000);
  const [includeSeparator, setIncludeSeparator] = useState(true);

  const handleAddSeparator = (separator: string) => {
    setSeparators([...separators, separator]);
  };

  const handleRemoveSeparator = (index: number) => {
    setSeparators(separators.filter((_, i) => i !== index));
  };

  const handleSave = () => {
    console.log('Saving settings:', {
      chunkSize,
      chunkOverlap,
      separators,
      lengthMethod,
      minChunkSize,
      maxChunkSize,
      includeSeparator,
    });
    alert('설정이 저장되었습니다!');
  };

  const handleCancel = () => {
    console.log('Cancel settings');
  };

  return (
    <div className="chunking-settings-layout">
      <Sidebar />
      <main className="chunking-settings-main">
        <Header title="청킹 설정" />
        <div className="chunking-settings-content">
          {/* Action Buttons */}
          <div className="settings-header-actions">
            <button className="cancel-btn" onClick={handleCancel}>
              취소
            </button>
            <button className="save-btn" onClick={handleSave}>
              설정 저장
            </button>
          </div>

          {/* Recommendation Panel (Right Side) */}
          <RecommendationPanel />

          {/* Main Settings Panel */}
          <div className="settings-panel">
            <h2 className="section-title">기본 설정</h2>

            <Slider
              label="청크 크기 (Chunk Size)"
              value={chunkSize}
              min={200}
              max={3000}
              step={50}
              unit="문자"
              onChange={setChunkSize}
              description="한 청크당 포함될 문자 수입니다. 너무 작으면 문맥이 손실되고, 너무 크면 검색 정확도가 떨어질 수 있습니다."
            />

            <Slider
              label="청크 오버랩 (Chunk Overlap)"
              value={chunkOverlap}
              min={0}
              max={500}
              step={10}
              unit="문자"
              onChange={setChunkOverlap}
            />

            <SeparatorManager
              separators={separators}
              onAdd={handleAddSeparator}
              onRemove={handleRemoveSeparator}
            />
          </div>

          {/* Advanced Settings Panel */}
          <div className="settings-panel advanced">
            <h2 className="section-title">고급 설정</h2>

            <div className="advanced-setting">
              <label className="advanced-label">길이 계산 방식</label>
              <select
                className="advanced-select"
                value={lengthMethod}
                onChange={(e) => setLengthMethod(e.target.value)}
              >
                <option>문자 수 (Characters)</option>
                <option>토큰 수 (Tokens)</option>
              </select>
            </div>

            <div className="advanced-grid">
              <div className="advanced-setting">
                <label className="advanced-label">최소 청크 크기</label>
                <input
                  type="number"
                  className="advanced-input"
                  value={minChunkSize}
                  onChange={(e) => setMinChunkSize(Number(e.target.value))}
                />
              </div>
              <div className="advanced-setting">
                <label className="advanced-label">최대 청크 크기</label>
                <input
                  type="number"
                  className="advanced-input"
                  value={maxChunkSize}
                  onChange={(e) => setMaxChunkSize(Number(e.target.value))}
                />
              </div>
            </div>

            <div className="checkbox-setting">
              <input
                type="checkbox"
                id="includeSeparator"
                checked={includeSeparator}
                onChange={(e) => setIncludeSeparator(e.target.checked)}
              />
              <label htmlFor="includeSeparator">구분자를 청크에 포함</label>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ChunkingSettings;

