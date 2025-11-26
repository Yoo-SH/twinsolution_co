import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import ProjectPanel from '../components/ProjectPanel';
import ChatMessage from '../components/ChatMessage';
import ChatInput from '../components/ChatInput';
import { getProjectMessages, getRecentProjects, sendProjectMessage } from '../services/api';
import type { ChatMessage as ChatMessageDto, Project } from '../types/api';
import './AIChat.css';

const quickQuestions = [
  '준주거지역 법령은 서류는?',
  '에너지절약계획서 작성 방법',
  '주차장 설치 기준 법령',
  '내진설계 의무 대상',
];

const openAiModels = ['gpt-3.5-turbo', 'gpt-4', 'gpt-4-turbo', 'gpt-4o'];

const AIChat = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(false);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false);
  const [advancedOptions, setAdvancedOptions] = useState({
    systemPrompt: '',
    model: 'gpt-3.5-turbo',
    temperature: 0.7,
    maxTokens: 1000,
    stream: false,
  });

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
      const data = await getRecentProjects(20);
      if (!isMountedRef.current) return;
      setProjects(data);
      setSelectedProjectId((prev) => {
        if (prev) return prev;
        return data.length ? data[0].id : null;
      });
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '프로젝트 목록을 불러오지 못했습니다.';
      setError(message);
    } finally {
      if (!isMountedRef.current) return;
      setProjectsLoading(false);
    }
  }, []);

  const loadMessages = useCallback(async (projectId: number) => {
    if (!isMountedRef.current) return;
    setMessagesLoading(true);
    setError(null);
    try {
      const history = await getProjectMessages(projectId);
      if (!isMountedRef.current) return;
      setMessages(history);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '대화 이력을 불러오지 못했습니다.';
      setError(message);
      setMessages([]);
    } finally {
      if (!isMountedRef.current) return;
      setMessagesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  useEffect(() => {
    const handleProjectCreated = (event: Event) => {
      const customEvent = event as CustomEvent<Project>;
      const newProject = customEvent.detail;
      setProjects((prev) => {
        const exists = prev.some((project) => project.id === newProject.id);
        const updated = exists
          ? prev.map((project) => (project.id === newProject.id ? newProject : project))
          : [newProject, ...prev];
        return updated.slice(0, 20);
      });
      setSelectedProjectId(newProject.id);
      loadMessages(newProject.id);
    };

    window.addEventListener('project-created', handleProjectCreated as EventListener);
    return () => {
      window.removeEventListener('project-created', handleProjectCreated as EventListener);
    };
  }, [loadMessages]);

  useEffect(() => {
    if (selectedProjectId) {
      loadMessages(selectedProjectId);
    }
  }, [selectedProjectId, loadMessages]);

  const handleSendMessage = async (text: string) => {
    if (!selectedProjectId) return;
    const trimmed = text.trim();
    if (!trimmed) return;

    const optimisticMessage: ChatMessageDto = {
      id: Date.now() * -1,
      sessionId: -1,
      role: 'user',
      content: trimmed,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, optimisticMessage]);
    setSending(true);
    setError(null);

    try {
      const payload = {
        content: trimmed,
        systemPrompt: advancedOptions.systemPrompt || undefined,
        model: advancedOptions.model || undefined,
        temperature: advancedOptions.temperature ?? undefined,
        maxTokens: advancedOptions.maxTokens ?? undefined,
        stream: advancedOptions.stream,
      };
      await sendProjectMessage(selectedProjectId, payload);
      await loadMessages(selectedProjectId);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '메시지를 전송하지 못했습니다.';
      setError(message);
      setMessages((prev) => prev.filter((msg) => msg.id !== optimisticMessage.id));
    } finally {
      if (!isMountedRef.current) return;
      setSending(false);
    }
  };

  const handleQuestionClick = (question: string) => {
    handleSendMessage(question);
  };

  const formattedMessages = useMemo(
    () =>
      messages.map((msg) => ({
        ...msg,
        formattedTime: new Date(msg.createdAt).toLocaleTimeString('ko-KR', {
          hour: '2-digit',
          minute: '2-digit',
          hour12: true,
        }),
      })),
    [messages],
  );

  return (
    <div className="ai-chat-layout">
      <Sidebar />
      <main className="ai-chat-main">
        <Header title="서류작성 AI" />
        <div className="ai-chat-container">
          <ProjectPanel
            project={selectedProject}
            quickQuestions={quickQuestions}
            onQuestionClick={handleQuestionClick}
          />
          <div className="chat-area">
            <div className="ai-chat-toolbar">
              <div className="toolbar-field">
                <label htmlFor="aiProjectSelect">프로젝트 선택</label>
                <select
                  id="aiProjectSelect"
                  value={selectedProjectId ?? ''}
                  onChange={(e) =>
                    setSelectedProjectId(e.target.value ? Number(e.target.value) : null)
                  }
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
                onClick={() => selectedProjectId && loadMessages(selectedProjectId)}
                disabled={!selectedProjectId || messagesLoading}
              >
                대화 새로고침
              </button>
            </div>

            <div className="advanced-settings">
              <button
                type="button"
                className="advanced-toggle"
                onClick={() => setShowAdvancedOptions((prev) => !prev)}
              >
                {showAdvancedOptions ? '고급 옵션 숨기기' : '고급 옵션 보기'}
              </button>
              {showAdvancedOptions && (
                <div className="advanced-panel">
                  <div className="advanced-field">
                    <label htmlFor="systemPrompt">시스템 프롬프트</label>
                    <textarea
                      id="systemPrompt"
                      rows={3}
                      value={advancedOptions.systemPrompt}
                      onChange={(e) =>
                        setAdvancedOptions((prev) => ({
                          ...prev,
                          systemPrompt: e.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="advanced-grid">
                    <div className="advanced-field">
                      <label htmlFor="modelSelect">모델</label>
                      <select
                        id="modelSelect"
                        value={advancedOptions.model}
                        onChange={(e) =>
                          setAdvancedOptions((prev) => ({
                            ...prev,
                            model: e.target.value,
                          }))
                        }
                      >
                        {openAiModels.map((model) => (
                          <option key={model} value={model}>
                            {model}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="advanced-field">
                      <label htmlFor="temperature">Temperature (0.0 ~ 2.0)</label>
                      <input
                        id="temperature"
                        type="number"
                        min={0}
                        max={2}
                        step={0.1}
                        value={advancedOptions.temperature}
                        onChange={(e) =>
                          setAdvancedOptions((prev) => ({
                            ...prev,
                            temperature: Number(e.target.value),
                          }))
                        }
                      />
                    </div>
                    <div className="advanced-field">
                      <label htmlFor="maxTokens">Max Tokens</label>
                      <input
                        id="maxTokens"
                        type="number"
                        min={1}
                        value={advancedOptions.maxTokens}
                        onChange={(e) =>
                          setAdvancedOptions((prev) => ({
                            ...prev,
                            maxTokens: Number(e.target.value),
                          }))
                        }
                      />
                    </div>
                    <div className="advanced-field checkbox-field">
                      <label htmlFor="streamToggle">스트리밍 모드 (추후 지원)</label>
                      <input
                        id="streamToggle"
                        type="checkbox"
                        checked={advancedOptions.stream}
                        onChange={(e) =>
                          setAdvancedOptions((prev) => ({
                            ...prev,
                            stream: e.target.checked,
                          }))
                        }
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            {error && (
              <div className="chat-error">
                <span>{error}</span>
                <button
                  type="button"
                  className="chat-retry-button"
                  onClick={() => selectedProjectId && loadMessages(selectedProjectId)}
                >
                  다시 시도
                </button>
              </div>
            )}
            <div className="messages-container">
              {messagesLoading ? (
                <div className="chat-loading">대화를 불러오는 중입니다...</div>
              ) : (
                formattedMessages.map((msg) => (
                  <ChatMessage
                    key={msg.id}
                    role={msg.role === 'assistant' ? 'assistant' : 'user'}
                    message={msg.content}
                    timestamp={msg.formattedTime}
                  />
                ))
              )}
            </div>
            <ChatInput
              onSend={handleSendMessage}
              disabled={!selectedProjectId || sending}
              placeholder={
                selectedProject
                  ? `${selectedProject.name}과 관련된 내용을 질문해보세요`
                  : '프로젝트를 불러오는 중입니다...'
              }
            />
          </div>
        </div>
      </main>
    </div>
  );
};

export default AIChat;

