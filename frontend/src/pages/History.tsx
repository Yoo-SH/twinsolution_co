import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import SearchBar from '../components/SearchBar';
import FilterDropdown from '../components/FilterDropdown';
import HistoryCard from '../components/HistoryCard';
import { getProjectDocuments, getProjectMessages, getRecentProjects } from '../services/api';
import type { ChatMessage, DocumentItem, Project } from '../types/api';
import './History.css';

const FILTER_OPTIONS = ['전체', '최근 7일', '최근 30일', '최근 90일'];

const History = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterValue, setFilterValue] = useState(FILTER_OPTIONS[0]);
  const [activeTab, setActiveTab] = useState<'대화 내역' | '생성된 서류'>('대화 내역');
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [generatedDocuments, setGeneratedDocuments] = useState<DocumentItem[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(false);
  const [messageLoading, setMessageLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId) ?? null,
    [projects, selectedProjectId],
  );

  const loadProjects = useCallback(async () => {
    if (!isMountedRef.current) return;
    setProjectsLoading(true);
    setError(null);

    try {
      const list = await getRecentProjects(20);
      if (!isMountedRef.current) return;
      setProjects(list);
      setSelectedProjectId((prev) => {
        if (prev) return prev;
        return list.length ? list[0].id : null;
      });
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '프로젝트를 불러오지 못했습니다.';
      setError(message);
    } finally {
      if (!isMountedRef.current) return;
      setProjectsLoading(false);
    }
  }, []);

  const loadMessages = useCallback(async (projectId: number) => {
    if (!isMountedRef.current) return;
    setMessageLoading(true);
    setError(null);

    try {
      const history = await getProjectMessages(projectId);
      if (!isMountedRef.current) return;
      setMessages(history);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '대화 내역을 불러오지 못했습니다.';
      setError(message);
      setMessages([]);
    } finally {
      if (!isMountedRef.current) return;
      setMessageLoading(false);
    }
  }, []);

  const loadGeneratedDocuments = useCallback(async (projectId: number) => {
    if (!isMountedRef.current) return;
    try {
      const docs = await getProjectDocuments(projectId);
      if (!isMountedRef.current) return;
      setGeneratedDocuments(docs.filter((doc) => doc.origin === 'generated'));
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '생성된 문서를 불러오지 못했습니다.';
      setError(message);
      setGeneratedDocuments([]);
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  useEffect(() => {
    const handleProjectCreated = (event: Event) => {
      const newProject = (event as CustomEvent<Project>).detail;
      setProjects((prev) => {
        const exists = prev.some((project) => project.id === newProject.id);
        const updated = exists
          ? prev.map((project) => (project.id === newProject.id ? newProject : project))
          : [newProject, ...prev];
        return updated.slice(0, 20);
      });
      setSelectedProjectId(newProject.id);
      loadMessages(newProject.id);
      loadGeneratedDocuments(newProject.id);
    };

    window.addEventListener('project-created', handleProjectCreated as EventListener);
    return () => {
      window.removeEventListener('project-created', handleProjectCreated as EventListener);
    };
  }, [loadMessages, loadGeneratedDocuments]);

  useEffect(() => {
    if (selectedProjectId) {
      loadMessages(selectedProjectId);
      loadGeneratedDocuments(selectedProjectId);
    }
  }, [selectedProjectId, loadMessages, loadGeneratedDocuments]);

  const filteredConversations = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    const now = Date.now();

    const rangeDays =
      filterValue === '최근 7일'
        ? 7
        : filterValue === '최근 30일'
        ? 30
        : filterValue === '최근 90일'
        ? 90
        : null;

    const conversations = [];
    for (let i = 0; i < messages.length; i += 1) {
      const userMessage = messages[i];
      if (userMessage.role === 'assistant') continue;
      const assistantMessage =
        i + 1 < messages.length && messages[i + 1].role === 'assistant'
          ? messages[i + 1]
          : undefined;

      const matchesQuery = normalizedQuery
        ? userMessage.content.toLowerCase().includes(normalizedQuery) ||
          assistantMessage?.content.toLowerCase().includes(normalizedQuery)
        : true;

      const updatedAt = new Date(userMessage.createdAt).getTime();
      const matchesRange = rangeDays ? now - updatedAt <= rangeDays * 24 * 60 * 60 * 1000 : true;

      if (!matchesQuery || !matchesRange) continue;

      conversations.push({
        id: userMessage.id,
        userMessage,
        assistantMessage,
      });
    }

    return conversations;
  }, [messages, searchQuery, filterValue]);

  const stats = useMemo(() => {
    const latestMessage = messages[messages.length - 1];

    return [
      {
        title: '전체 대화',
        value: filteredConversations.length,
        trend: selectedProject ? selectedProject.name : '',
        icon: '💬',
        iconColor: 'rgba(200, 200, 200, 0.3)',
      },
      {
        title: '총 메시지',
        value: messages.length,
        trend: '',
        icon: '✉️',
        iconColor: 'rgba(123, 104, 238, 0.2)',
      },
      {
        title: '생성된 서류',
        value: generatedDocuments.length,
        trend: '',
        icon: '📄',
        iconColor: 'rgba(144, 238, 144, 0.3)',
      },
      {
        title: '최근 업데이트',
        value: latestMessage
          ? new Date(latestMessage.createdAt).toLocaleString('ko-KR', {
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
            })
          : '-',
        trend: '',
        icon: '📅',
        iconColor: 'rgba(255, 182, 193, 0.3)',
      },
    ];
  }, [filteredConversations.length, generatedDocuments.length, messages, selectedProject]);

  return (
    <div className="history-layout">
      <Sidebar />
      <main className="history-main">
        <Header title="히스토리" />
        <div className="history-content">
          {error && (
            <div className="history-error">
              <span>{error}</span>
              {selectedProjectId && (
                <button
                  type="button"
                  className="history-retry-button"
                  onClick={() => loadMessages(selectedProjectId)}
                >
                  다시 시도
                </button>
              )}
            </div>
          )}

          <section className="history-stats-section">
            {stats.map((stat) => (
              <StatCard
                key={stat.title}
                title={stat.title}
                value={stat.value}
                trend={stat.trend}
                icon={stat.icon}
                iconColor={stat.iconColor}
              />
            ))}
          </section>

          <div className="project-selector history-selector">
            <div>
              <label htmlFor="historyProjectSelect">프로젝트 선택</label>
              <select
                id="historyProjectSelect"
                value={selectedProjectId ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setSelectedProjectId(value ? Number(value) : null);
                }}
                disabled={projectsLoading}
              >
                {!projects.length && <option value="">프로젝트 없음</option>}
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              className="refresh-button"
              disabled={!selectedProjectId || messageLoading}
              onClick={() => selectedProjectId && loadMessages(selectedProjectId)}
            >
              새로고침
            </button>
          </div>

          <section className="tabs-section">
            <div className="tabs">
              <button
                className={`tab ${activeTab === '대화 내역' ? 'active' : ''}`}
                onClick={() => setActiveTab('대화 내역')}
              >
                대화 내역
                <span className="tab-badge">{filteredConversations.length}</span>
              </button>
              <button
                className={`tab ${activeTab === '생성된 서류' ? 'active' : ''}`}
                onClick={() => setActiveTab('생성된 서류')}
              >
                생성된 서류
                <span className="tab-badge">{generatedDocuments.length}</span>
              </button>
            </div>
            <div className="progress-bar-container">
              <div className="progress-bar-track">
                <div
                  className="progress-bar-fill"
                  style={{
                    width: activeTab === '대화 내역' ? '150px' : 'calc(150px + 165px)',
                  }}
                />
              </div>
            </div>
          </section>

          {activeTab === '대화 내역' && (
            <>
              <section className="search-filter-section">
                <SearchBar
                  value={searchQuery}
                  onChange={setSearchQuery}
                  placeholder="대화 검색"
                />
                <FilterDropdown
                  value={filterValue}
                  options={FILTER_OPTIONS}
                  onChange={setFilterValue}
                />
              </section>

              <section className="conversations-section">
                {messageLoading ? (
                  <div className="history-loading">대화를 불러오는 중입니다...</div>
                ) : filteredConversations.length ? (
                  filteredConversations.map(({ id, userMessage, assistantMessage }) => (
                    <HistoryCard
                      key={id}
                      title={
                        userMessage.content.length > 40
                          ? `${userMessage.content.slice(0, 40)}...`
                          : userMessage.content
                      }
                      description={
                        assistantMessage
                          ? assistantMessage.content.length > 80
                            ? `${assistantMessage.content.slice(0, 80)}...`
                            : assistantMessage.content
                          : 'AI 응답이 아직 없습니다.'
                      }
                      date={new Date(userMessage.createdAt).toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                      project={selectedProject?.name ?? '프로젝트 미지정'}
                      messageCount={assistantMessage ? 2 : 1}
                      onPlay={() => console.log('Play:', id)}
                      onEdit={() => console.log('Edit:', id)}
                      onDelete={() => console.log('Delete:', id)}
                    />
                  ))
                ) : (
                  <div className="history-empty">표시할 대화가 없습니다.</div>
                )}
              </section>
            </>
          )}

          {activeTab === '생성된 서류' && (
            <section className="documents-section">
              {generatedDocuments.length ? (
                generatedDocuments.map((doc) => (
                  <div key={doc.id} className="generated-doc-card">
                    <div>
                      <h4>{doc.name}</h4>
                      <p>{doc.fileType}</p>
                    </div>
                    <span>
                      {new Date(doc.createdAt).toLocaleString('ko-KR', {
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>
                ))
              ) : (
                <div className="history-empty">생성된 서류가 없습니다.</div>
              )}
            </section>
          )}
        </div>
      </main>
    </div>
  );
};

export default History;

