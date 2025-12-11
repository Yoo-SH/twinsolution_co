import { useCallback, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import Slider from '../components/Slider';
import SeparatorManager from '../components/SeparatorManager';
import RecommendationPanel from '../components/RecommendationPanel';
import Toast from '../components/Toast';
import { previewChunks } from '../services/api';
import { useChunkSettings } from '../hooks/useChunkSettings';
import type { ChunkPreviewResponse } from '../types/api';
import './ChunkingSettings.css';

const DEFAULT_PREVIEW_TEXT =
  '건축법 제11조에 따른 허가를 받으려는 자는 별지 서식에 따른 신청서를 제출해야 한다.';

const ChunkingSettings = () => {
  const { settings, loading, error, loadSettings, saveSettings } = useChunkSettings();

  const [chunkSize, setChunkSize] = useState(1000);
  const [chunkOverlap, setChunkOverlap] = useState(200);
  const [separators, setSeparators] = useState(['\n\n', '\n', '. ', ' ']);
  const [lengthMethod, setLengthMethod] = useState('문자 수 (Characters)');
  const [minChunkSize, setMinChunkSize] = useState(100);
  const [maxChunkSize, setMaxChunkSize] = useState(2000);
  const [includeSeparator, setIncludeSeparator] = useState(true);
  const [saving, setSaving] = useState(false);
  const [previewText, setPreviewText] = useState(DEFAULT_PREVIEW_TEXT);
  const [previewResult, setPreviewResult] = useState<ChunkPreviewResponse | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [showSuccessToast, setShowSuccessToast] = useState(false);
  const [showInfoToast, setShowInfoToast] = useState(false);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Load settings from hook
  useEffect(() => {
    if (settings) {
      setChunkSize(settings.chunkSize);
      setChunkOverlap(settings.chunkOverlap);
    }
  }, [settings]);

  const handleAddSeparator = (separator: string) => {
    setSeparators([...separators, separator]);
  };

  const handleRemoveSeparator = (index: number) => {
    setSeparators(separators.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await saveSettings({ chunkSize, chunkOverlap });
      setShowSuccessToast(true);
    } catch (err) {
      // Error already handled in hook
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (settings) {
      setChunkSize(settings.chunkSize);
      setChunkOverlap(settings.chunkOverlap);
    }
  };

  const handlePreview = async () => {
    if (!previewText.trim()) {
      setShowInfoToast(true);
      return;
    }

    setPreviewLoading(true);
    setError(null);

    try {
      const result = await previewChunks({
        text: previewText,
        chunkSize,
        chunkOverlap,
        separators,
      });
      if (!isMountedRef.current) return;
      setPreviewResult(result);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '미리보기를 생성하지 못했습니다.';
      setError(message);
      setPreviewResult(null);
    } finally {
      if (!isMountedRef.current) return;
      setPreviewLoading(false);
    }
  };

  return (
    <div className="chunking-settings-layout">
      <Sidebar />
      <main className="chunking-settings-main">
        <Header title="청킹 설정" />
        <div className="chunking-settings-content">
          {error && (
            <div className="chunk-error">
              <span>{error}</span>
              <button type="button" className="chunk-retry-button" onClick={loadSettings}>
                다시 시도
              </button>
            </div>
          )}

          {/* Action Buttons */}
          <div className="settings-header-actions">
            <button className="cancel-btn" onClick={handleCancel} disabled={loading || saving}>
              취소
            </button>
            <button className="save-btn" onClick={handleSave} disabled={loading || saving}>
              {saving ? '저장 중...' : '설정 저장'}
            </button>
          </div>

          {/* Recommendation Panel (Right Side) */}
          <RecommendationPanel />

          {/* Main Settings Panel */}
          <div className="settings-panel">
            <h2 className="section-title">기본 설정</h2>

            {loading ? (
              <div className="chunk-loading">설정을 불러오는 중입니다...</div>
            ) : (
              <>
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
              </>
            )}
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

          <div className="settings-panel preview-panel">
            <h2 className="section-title">미리보기</h2>
            <textarea
              className="preview-textarea"
              value={previewText}
              onChange={(e) => setPreviewText(e.target.value)}
              placeholder="청크 미리보기용 텍스트를 입력하세요."
              rows={6}
            />
            <div className="preview-actions">
              <button
                type="button"
                className="preview-btn"
                onClick={handlePreview}
                disabled={previewLoading || !previewText.trim()}
              >
                {previewLoading ? '미리보기 생성 중...' : '미리보기 생성'}
              </button>
              {previewResult && (
                <div className="preview-summary">
                  <span>총 청크: {previewResult.totalChunks}</span>
                  <span>평균 길이: {Math.round(previewResult.averageLength)}자</span>
                  <span>오버랩 비율: {previewResult.overlapRatio.toFixed(1)}%</span>
                </div>
              )}
            </div>

            {previewResult && (
              <div className="preview-chunks">
                {previewResult.chunks.slice(0, 3).map((chunk) => (
                  <div key={chunk.index} className="preview-chunk-card">
                    <div className="preview-chunk-header">
                      <span>Chunk #{chunk.index + 1}</span>
                      <span>{chunk.length}자</span>
                    </div>
                    <p>{chunk.content}</p>
                  </div>
                ))}
                {previewResult.totalChunks > 3 && (
                  <p className="preview-more">...총 {previewResult.totalChunks}개의 청크가 생성됩니다.</p>
                )}
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Toast 알림 */}
      {showSuccessToast && createPortal(
        <Toast
          message="설정이 저장되었습니다!"
          type="success"
          onClose={() => setShowSuccessToast(false)}
        />,
        document.body
      )}

      {showInfoToast && createPortal(
        <Toast
          message="미리보기 텍스트를 입력해주세요."
          type="info"
          onClose={() => setShowInfoToast(false)}
        />,
        document.body
      )}
    </div>
  );
};

export default ChunkingSettings;

