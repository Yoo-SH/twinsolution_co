import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import ProjectPanel from '../components/ProjectPanel';
import ChatMessage from '../components/ChatMessage';
import ChatInput from '../components/ChatInput';
import { useLLM } from '../contexts/LLMContext';
import { getProjectMessages, getRecentProjects, sendProjectMessageStream } from '../services/api';
import type { ChatMessage as ChatMessageDto, Project } from '../types/api';
import './AIChat.css';

const quickQuestions = [
  '준주거지역 법령은 서류는?',
  '에너지절약계획서 작성 방법',
  '주차장 설치 기준 법령',
  '내진설계 의무 대상',
];

const AIChat = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(false);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 전역 LLM 설정 사용
  const { settings: llmSettings, currentModelInfo } = useLLM();

  const isMountedRef = useRef(true);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!messagesContainerRef.current) return;
    messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
  }, [messages]);

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

    const optimisticUserMessage: ChatMessageDto = {
      id: Date.now() * -1,
      sessionId: -1,
      role: 'user',
      content: trimmed,
      createdAt: new Date().toISOString(),
    };

    const optimisticAssistantMessage: ChatMessageDto = {
      id: Date.now() * -1 - 1,
      sessionId: -1,
      role: 'assistant',
      content: '',
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, optimisticUserMessage, optimisticAssistantMessage]);
    setSending(true);
    setError(null);

    try {
      const payload = {
        content: trimmed,
        systemPrompt: llmSettings.systemPrompt || undefined,
        model: llmSettings.modelName,
        llmProvider: llmSettings.provider,
        temperature: llmSettings.temperature ?? undefined,
        maxTokens: llmSettings.maxTokens ?? undefined,
      };

      let accumulatedContent = '';

      await sendProjectMessageStream(
        selectedProjectId,
        payload,
        (chunk: string) => {
          // 스트리밍 청크를 받을 때마다 메시지 업데이트
          accumulatedContent += chunk;
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === optimisticAssistantMessage.id
                ? { ...msg, content: accumulatedContent }
                : msg
            )
          );
        },
        async () => {
          // 스트리밍 완료 후 서버에서 최신 메시지 다시 로드
          if (!isMountedRef.current) return;
          await loadMessages(selectedProjectId);
          setSending(false);
        },
        (error: Error) => {
          // 에러 처리
          if (!isMountedRef.current) return;
          setError(error.message || '메시지를 전송하지 못했습니다.');
          setMessages((prev) =>
            prev.filter(
              (msg) =>
                msg.id !== optimisticUserMessage.id && msg.id !== optimisticAssistantMessage.id
            )
          );
          setSending(false);
        }
      );
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '메시지를 전송하지 못했습니다.';
      setError(message);
      setMessages((prev) =>
        prev.filter(
          (msg) =>
            msg.id !== optimisticUserMessage.id && msg.id !== optimisticAssistantMessage.id
        )
      );
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
        isWaiting: sending && msg.role === 'assistant' && msg.id < 0 && msg.content === '',
        isStreaming: sending && msg.role === 'assistant' && msg.id < 0 && msg.content !== '',
      })),
    [messages, sending],
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

            <div className="current-settings-info">
              <div className="current-model-info">
                현재 모델: <strong>{currentModelInfo?.displayName || llmSettings.modelName}</strong>
                <span className="model-provider">({llmSettings.provider})</span>
              </div>
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
            <div className="messages-container" ref={messagesContainerRef}>
              {messagesLoading ? (
                <div className="chat-loading">대화를 불러오는 중입니다...</div>
              ) : (
                formattedMessages.map((msg) => (
                  <ChatMessage
                    key={msg.id}
                    role={msg.role === 'assistant' ? 'assistant' : 'user'}
                    message={msg.content}
                    timestamp={msg.formattedTime}
                    isWaiting={msg.isWaiting}
                    isStreaming={msg.isStreaming}
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

