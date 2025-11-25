import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import SearchBar from '../components/SearchBar';
import FilterDropdown from '../components/FilterDropdown';
import HistoryCard from '../components/HistoryCard';
import {
  getChatMessages,
  getChatSessionsByProject,
  getProjectDocuments,
  getRecentProjects,
} from '../services/api';
import type { ChatMessage, ChatSession, DocumentItem, Project } from '../types/api';
import './History.css';

const FILTER_OPTIONS = ['전체', '최근 7일', '최근 30일', '최근 90일'];
const MAX_SESSIONS = 10;

interface SessionSummary {
  session: ChatSession;
  messageCount: number;
  lastMessage?: ChatMessage;
}

const History = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterValue, setFilterValue] = useState(FILTER_OPTIONS[0]);
  const [activeTab, setActiveTab] = useState<'대화 내역' | '생성된 서류'>('대화 내역');
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [generatedDocuments, setGeneratedDocuments] = useState<DocumentItem[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(false);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const loadProjects = useCallback(async () => {
    if (!isMountedRef.current) return;
    setProjectsLoading(true);
    setError(null);

    try {
      const list = await getRecentProjects(20);
      if (!isMountedRef.current) return;
      setProjects(list);
      if (!selectedProjectId && list.length) {
        setSelectedProjectId(list[0].id);
      }
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '프로젝트를 불러오지 못했습니다.';
      setError(message);
    } finally {
      if (!isMountedRef.current) return;
      setProjectsLoading(false);
    }
  }, [selectedProjectId]);

  const loadSessions = useCallback(
    async (projectId: number) => {
      if (!isMountedRef.current) return;
      setSessionsLoading(true);
      setError(null);

      try {
        const sessionList = await getChatSessionsByProject(projectId);
        const limited = sessionList.slice(0, MAX_SESSIONS);

        const summaries = await Promise.all(
          limited.map(async (session) => {
            const messages = await getChatMessages(session.id);
            return {
              session,
              messageCount: messages.length,
              lastMessage: messages[messages.length - 1],
            } as SessionSummary;
          }),
        );

        if (!isMountedRef.current) return;
        setSessions(summaries);
      } catch (err) {
        if (!isMountedRef.current) return;
        const message =
          err instanceof Error ? err.message : '대화 내역을 불러오지 못했습니다.';
        setError(message);
        setSessions([]);
      } finally {
        if (!isMountedRef.current) return;
        setSessionsLoading(false);
      }
    },
    [],
  );

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
    if (selectedProjectId) {
      loadSessions(selectedProjectId);
      loadGeneratedDocuments(selectedProjectId);
    }
  }, [selectedProjectId, loadSessions, loadGeneratedDocuments]);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId) ?? null,
    [projects, selectedProjectId],
  );

  const filteredSessions = useMemo(() => {
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

    return sessions.filter(({ session, lastMessage }) => {
      const matchesQuery = normalizedQuery
        ? session.quickQuestion?.toLowerCase().includes(normalizedQuery) ||
          lastMessage?.content.toLowerCase().includes(normalizedQuery)
        : true;

      const matchesRange = rangeDays
        ? (() => {
            const updatedAt = new Date(session.updatedAt ?? session.createdAt).getTime();
            return now - updatedAt <= rangeDays * 24 * 60 * 60 * 1000;
          })()
        : true;

      return matchesQuery && matchesRange;
    });
  }, [sessions, searchQuery, filterValue]);

  const stats = useMemo(() => {
    const totalMessages = sessions.reduce((sum, item) => sum + item.messageCount, 0);
    const latestSession = [...sessions].sort(
      (a, b) =>
        new Date(b.session.updatedAt ?? b.session.createdAt).getTime() -
        new Date(a.session.updatedAt ?? a.session.createdAt).getTime(),
    )[0];

    return [
      {
        title: '전체 대화',
        value: sessions.length,
        trend: selectedProject ? selectedProject.name : '',
        icon: '💬',
        iconColor: 'rgba(200, 200, 200, 0.3)',
      },
      {
        title: '총 메시지',
        value: totalMessages,
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
        value: latestSession
          ? new Date(
              latestSession.session.updatedAt ?? latestSession.session.createdAt,
            ).toLocaleString('ko-KR', {
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
  }, [sessions, selectedProject, generatedDocuments]);

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
                  onClick={() => loadSessions(selectedProjectId)}
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
              disabled={!selectedProjectId || sessionsLoading}
              onClick={() => selectedProjectId && loadSessions(selectedProjectId)}
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
                <span className="tab-badge">{sessions.length}</span>
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
                  placeholder="세션 검색"
                />
                <FilterDropdown
                  value={filterValue}
                  options={FILTER_OPTIONS}
                  onChange={setFilterValue}
                />
              </section>

              <section className="conversations-section">
                {sessionsLoading ? (
                  <div className="history-loading">대화를 불러오는 중입니다...</div>
                ) : filteredSessions.length ? (
                  filteredSessions.map((item) => (
                    <HistoryCard
                      key={item.session.id}
                      title={
                        item.session.quickQuestion
                          ? item.session.quickQuestion
                          : `대화 세션 #${item.session.id}`
                      }
                      description={
                        item.lastMessage?.content ?? '최근 메시지가 아직 없습니다.'
                      }
                      date={new Date(
                        item.session.updatedAt ?? item.session.createdAt,
                      ).toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                      project={selectedProject?.name ?? '프로젝트 미지정'}
                      messageCount={item.messageCount}
                      onPlay={() => console.log('Play:', item.session.id)}
                      onEdit={() => console.log('Edit:', item.session.id)}
                      onDelete={() => console.log('Delete:', item.session.id)}
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

