import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import ProjectPanel from '../components/ProjectPanel';
import ChatMessage from '../components/ChatMessage';
import ChatInput from '../components/ChatInput';
import {
  createChatSession,
  getChatMessages,
  getChatSessionsByProject,
  getRecentProjects,
  sendChatMessage,
} from '../services/api';
import type { ChatMessage as ChatMessageDto, ChatSession, Project } from '../types/api';
import './AIChat.css';

const projectInfo = {
  location: '전북 전주시 덕진구',
  zone: '준주거지역',
  purpose: '제1종 근린생활시설',
  area: '850㎡',
  floors: '지상 3층',
  parking: '15대',
};

const quickQuestions = [
  '준주거지역 법령은 서류는?',
  '에너지절약계획서 작성 방법',
  '주차장 설치 기준 법령',
  '내진설계 의무 대상',
];

const AIChat = () => {
  const [activeProject, setActiveProject] = useState<Project | null>(null);
  const [session, setSession] = useState<ChatSession | null>(null);
  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const loadMessages = useCallback(async (sessionId: number) => {
    const history = await getChatMessages(sessionId);
    if (!isMountedRef.current) return;
    setMessages(history);
  }, []);

  const bootstrapChat = useCallback(async () => {
    if (!isMountedRef.current) return;
    setLoading(true);
    setError(null);

    try {
      const projectList = await getRecentProjects(1);
      if (!projectList.length) {
        throw new Error('등록된 프로젝트가 없습니다. 먼저 프로젝트를 생성해주세요.');
      }

      const project = projectList[0];
      if (!isMountedRef.current) return;
      setActiveProject(project);

      let sessions = await getChatSessionsByProject(project.id);
      let activeSession = sessions[0];

      if (!activeSession) {
        activeSession = await createChatSession(project.id, {});
      }

      if (!isMountedRef.current) return;
      setSession(activeSession);
      await loadMessages(activeSession.id);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '대화 세션을 불러오지 못했습니다.';
      setError(message);
      setMessages([]);
    } finally {
      if (!isMountedRef.current) return;
      setLoading(false);
    }
  }, [loadMessages]);

  useEffect(() => {
    bootstrapChat();
  }, [bootstrapChat]);

  const handleSendMessage = async (text: string) => {
    if (!session) return;
    const trimmed = text.trim();
    if (!trimmed) return;

    const tempId = Date.now() * -1;
    const optimisticMessage: ChatMessageDto = {
      id: tempId,
      sessionId: session.id,
      role: 'user',
      content: trimmed,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, optimisticMessage]);
    setSending(true);
    setError(null);

    try {
      await sendChatMessage(session.id, { content: trimmed });
      await loadMessages(session.id);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '메시지를 전송하지 못했습니다.';
      setError(message);
      setMessages((prev) => prev.filter((msg) => msg.id !== tempId));
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
            projectInfo={projectInfo}
            quickQuestions={quickQuestions}
            onQuestionClick={handleQuestionClick}
          />
          <div className="chat-area">
            {error && (
              <div className="chat-error">
                <span>{error}</span>
                <button type="button" className="chat-retry-button" onClick={bootstrapChat}>
                  다시 시도
                </button>
              </div>
            )}
            <div className="messages-container">
              {loading ? (
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
              disabled={!session || sending}
              placeholder={
                activeProject
                  ? `${activeProject.name}과 관련된 내용을 질문해보세요`
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

