import { useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, useLocation } from 'react-router-dom';
import { useLLM } from '../contexts/LLMContext';
import type { LLMProvider } from '../types/api';
import './Sidebar.css';

const Sidebar = () => {
  const location = useLocation();
  const [showSettings, setShowSettings] = useState(false);
  const { settings, setSettings, providers, loadingProviders, currentProviderInfo, currentModelInfo } = useLLM();

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
          onClick={() => setShowSettings(!showSettings)}
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
                        value={settings.provider}
                        onChange={(e) => {
                          const newProvider = providers.find(p => p.id === e.target.value);
                          const firstModel = newProvider?.models[0]?.name ?? '';
                          setSettings({
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
                        value={settings.modelName}
                        onChange={(e) => setSettings({ ...settings, modelName: e.target.value })}
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

