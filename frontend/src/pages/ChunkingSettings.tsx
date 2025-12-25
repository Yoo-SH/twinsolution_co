import { useEffect, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import Slider from '../components/Slider';
import SeparatorManager from '../components/SeparatorManager';
import RecommendationPanel from '../components/RecommendationPanel';
// import { previewChunks } from '../services/api';
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
  const [previewError, setPreviewError] = useState<string | null>(null);
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
      alert('설정이 저장되었습니다!');
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

  // 시연용: 더미 청킹 결과 생성 함수
  const generateMockChunks = (
    text: string,
    size: number,
    overlap: number,
  ): ChunkPreviewResponse['chunks'] => {
    const chunks: ChunkPreviewResponse['chunks'] = [];
    
    // 텍스트가 청크 크기보다 작으면 전체를 하나의 청크로
    if (text.length <= size) {
      chunks.push({
        index: 0,
        content: text,
        length: text.length,
      });
      return chunks;
    }

    let currentIndex = 0;
    let chunkIndex = 0;

    while (currentIndex < text.length) {
      const endIndex = Math.min(currentIndex + size, text.length);
      const chunkContent = text.slice(currentIndex, endIndex);
      
      chunks.push({
        index: chunkIndex,
        content: chunkContent,
        length: chunkContent.length,
      });

      // 다음 청크 시작 위치 계산 (오버랩 고려)
      currentIndex = endIndex - overlap;
      chunkIndex++;

      // 무한 루프 방지
      if (currentIndex >= text.length) break;
      if (chunkIndex > 20) break; // 최대 청크 수 제한
      if (currentIndex <= 0) break; // 진행이 없으면 중단
    }

    // 최소 1개 청크는 보장
    if (chunks.length === 0) {
      chunks.push({
        index: 0,
        content: text,
        length: text.length,
      });
    }

    return chunks;
  };

  const handlePreview = async () => {
    if (!previewText.trim()) {
      alert('미리보기 텍스트를 입력해주세요.');
      return;
    }

    setPreviewLoading(true);
    setPreviewError(null);
    setPreviewResult(null); // 이전 결과 초기화

    try {
      // 시연용: 더미 청킹 결과 생성 (실제 API 호출 대신)
      await new Promise((resolve) => setTimeout(resolve, 2000)); // 미리보기 생성 시뮬레이션 (2초)

      if (!isMountedRef.current) {
        setPreviewLoading(false);
        return;
      }

      const mockChunks = generateMockChunks(previewText, chunkSize, chunkOverlap);
      const totalLength = mockChunks.reduce((sum, chunk) => sum + chunk.length, 0);
      const averageLength = mockChunks.length > 0 ? totalLength / mockChunks.length : 0;
      const overlapRatio = chunkSize > 0 ? (chunkOverlap / chunkSize) * 100 : 0;

      const mockResult: ChunkPreviewResponse = {
        chunks: mockChunks,
        totalChunks: mockChunks.length,
        averageLength: Math.round(averageLength),
        overlapRatio: Math.round(overlapRatio * 10) / 10,
      };

      setPreviewResult(mockResult);
      setPreviewLoading(false);

      // 실제 API 호출은 주석 처리 (시연용)
      // const result = await previewChunks({
      //   text: previewText,
      //   chunkSize,
      //   chunkOverlap,
      //   separators,
      // });
      // if (!isMountedRef.current) return;
      // setPreviewResult(result);
    } catch (err) {
      if (!isMountedRef.current) {
        setPreviewLoading(false);
        return;
      }
      const message =
        err instanceof Error ? err.message : '미리보기를 생성하지 못했습니다.';
      setPreviewError(message);
      setPreviewResult(null);
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

            {previewError && (
              <div className="chunk-error">
                <span>{previewError}</span>
              </div>
            )}
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
    </div>
  );
};

export default ChunkingSettings;

