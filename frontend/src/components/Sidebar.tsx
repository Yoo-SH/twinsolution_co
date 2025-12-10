import { useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, useLocation } from 'react-router-dom';
import { useLLM } from '../contexts/LLMContext';
import type { LLMProvider } from '../types/api';
import './Sidebar.css';

const Sidebar = () => {
  const location = useLocation();
  const [showSettings, setShowSettings] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string>('');
  const { settings, setSettings, providers, loadingProviders, currentProviderInfo, currentModelInfo } = useLLM();

  // 임시 설정 (모달에서 수정 중인 설정)
  const [tempSettings, setTempSettings] = useState(settings);

  // 모델별 최대 토큰 수 반환
  const getMaxTokensForModel = (modelName: string): number => {
    if (modelName.includes('gpt-3.5-turbo')) return 4096;
    if (modelName.includes('gpt-4')) return 8192;
    return 4096; // 기본값
  };

  const currentMaxTokens = getMaxTokensForModel(tempSettings.modelName);

  // 모달이 열릴 때 현재 설정을 임시 설정에 복사
  const handleOpenSettings = () => {
    setTempSettings(settings);
    setSaveMessage('');
    setShowSettings(true);
  };

  // 저장 버튼 클릭
  const handleSaveSettings = () => {
    setSettings(tempSettings);
    setSaveMessage('설정이 저장되었습니다!');
    setTimeout(() => {
      setSaveMessage('');
      setShowSettings(false);
    }, 1500);
  };

  const mainMenuItems = [
    { path: '/', label: '대시보드', icon: '📊' },
    { path: '/documents', label: '서류작성 AI', icon: '📄' },
    { path: '/projects/new', label: '프로젝트 생성', icon: '🏗️' },
  ];

  const ragMenuItems = [
    { path: '/rag', label: '문서 관리', icon: '💾' },
    { path: '/chunking', label: '청킹 설정', icon: '⚙️' },
    { path: '/api', label: 'API 연동', icon: '🔗' },
  ];

  const historyItem = { path: '/history', label: '히스토리', icon: '🕐' };

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="logo">
          <img src="/triplemahorlogo.png" alt="Triple Major Logo" className="logo-image" />
        </div>
        <div className="project-info">
          <h2>건축허가</h2>
          <p>자동화 시스템</p>
        </div>
        <button
          className="settings-btn"
          onClick={handleOpenSettings}
          title="AI 모델 설정"
        >
          ⚙️
        </button>

        {/* AI 모델 선택 모달 - Portal로 body에 렌더링 */}
        {showSettings && createPortal(
          <>
            {/* 백드롭 (어두운 배경) */}
            <div
              className="settings-backdrop"
              onClick={() => setShowSettings(false)}
            />

            {/* 모달 */}
            <div className="settings-modal">
              <div className="settings-modal-header">
                <h3>AI 모델 설정</h3>
                <button
                  className="settings-modal-close"
                  onClick={() => setShowSettings(false)}
                >
                  ×
                </button>
              </div>

              <div className="settings-modal-content">
                {loadingProviders ? (
                  <div className="settings-loading">로딩 중...</div>
                ) : (
                  <>
                    <div className="settings-field">
                      <label>LLM Provider</label>
                      <select
                        value={tempSettings.provider}
                        onChange={(e) => {
                          const newProvider = providers.find(p => p.id === e.target.value);
                          const firstModel = newProvider?.models[0]?.name ?? '';
                          setTempSettings({
                            ...tempSettings,
                            provider: e.target.value as LLMProvider,
                            modelName: firstModel,
                          });
                        }}
                      >
                        {providers.map((provider) => (
                          <option
                            key={provider.id}
                            value={provider.id}
                            disabled={!provider.available}
                          >
                            {provider.name} {!provider.available && '(사용 불가)'}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="settings-field">
                      <label>모델</label>
                      <select
                        value={tempSettings.modelName}
                        onChange={(e) => {
                          const newModelName = e.target.value;
                          const newMaxTokens = getMaxTokensForModel(newModelName);
                          // 현재 maxTokens가 새 모델의 최대값을 초과하면 조정
                          const adjustedMaxTokens = Math.min(tempSettings.maxTokens, newMaxTokens);
                          setTempSettings({
                            ...tempSettings,
                            modelName: newModelName,
                            maxTokens: adjustedMaxTokens
                          });
                        }}
                      >
                        {currentProviderInfo?.models.map((model) => (
                          <option key={model.name} value={model.name}>
                            {model.displayName}
                          </option>
                        ))}
                      </select>
                    </div>

                    {currentModelInfo?.description && (
                      <div className="settings-model-desc">
                        {currentModelInfo.description}
                      </div>
                    )}

                    <div className="settings-divider" />

                    <div className="settings-field">
                      <label>시스템 프롬프트</label>
                      <textarea
                        rows={3}
                        value={tempSettings.systemPrompt}
                        onChange={(e) => setTempSettings({ ...tempSettings, systemPrompt: e.target.value })}
                        placeholder="시스템 프롬프트를 입력하세요 (선택사항)"
                      />
                    </div>

                    <div className="settings-field">
                      <label>Temperature (0.0 ~ 1.0)</label>
                      <input
                        type="number"
                        min={0}
                        max={1}
                        step={0.1}
                        value={tempSettings.temperature}
                        onChange={(e) => {
                          const value = Math.min(1, Math.max(0, Number(e.target.value)));
                          setTempSettings({ ...tempSettings, temperature: value });
                        }}
                      />
                      <small>값이 높을수록 창의적이고 다양한 응답을 생성합니다</small>
                    </div>

                    <div className="settings-field">
                      <label>Max Tokens (최대: {currentMaxTokens.toLocaleString()})</label>
                      <input
                        type="number"
                        min={1}
                        max={currentMaxTokens}
                        value={tempSettings.maxTokens}
                        onChange={(e) => {
                          const value = Math.min(currentMaxTokens, Math.max(1, Number(e.target.value)));
                          setTempSettings({ ...tempSettings, maxTokens: value });
                        }}
                      />
                      <small>생성할 최대 토큰 수를 설정합니다 (현재 모델: {currentMaxTokens.toLocaleString()} 토큰)</small>
                    </div>

                    {/* 저장 버튼 */}
                    <div className="settings-actions">
                      <button
                        className="settings-save-btn"
                        onClick={handleSaveSettings}
                      >
                        저장
                      </button>
                      {saveMessage && (
                        <span className="settings-save-message">{saveMessage}</span>
                      )}
                    </div>
                  </>
                )}
              </div>
            </div>
          </>,
          document.body
        )}
      </div>

      <nav className="sidebar-nav">
        {mainMenuItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </Link>
        ))}

        <div className="nav-section">
          <div className="nav-section-title">RAG 관리</div>
          {ragMenuItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
            >
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
            </Link>
          ))}
        </div>

        <Link
          to={historyItem.path}
          className={`nav-item ${location.pathname === historyItem.path ? 'active' : ''}`}
        >
          <span className="nav-icon">{historyItem.icon}</span>
          <span className="nav-label">{historyItem.label}</span>
        </Link>
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile">
          <div className="user-avatar">TM</div>
          <div className="user-info">
            <span className="user-name">트리플 메이저</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;

