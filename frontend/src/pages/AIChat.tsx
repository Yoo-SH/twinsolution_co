import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import ProjectPanel from '../components/ProjectPanel';
import ChatMessage from '../components/ChatMessage';
import ChatInput from '../components/ChatInput';
import { getProjectMessages, getRecentProjects, sendProjectMessageStream } from '../services/api';
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

  // 시연용: 더미 AI 응답 생성 함수
  const getMockAIResponse = (question: string): string => {
    const lowerQuestion = question.toLowerCase();
    
    if (lowerQuestion.includes('준주거지역') || lowerQuestion.includes('법령') || lowerQuestion.includes('서류')) {
      return `준주거지역의 건축 허가를 받기 위해서는 다음 서류가 필요합니다:

1. 건축허가 신청서
2. 건축계획서
3. 대지 및 건축물 위치도
4. 구조계산서
5. 에너지절약계획서
6. 주차장 설치계획서

이 서류들은 건축법 제11조 및 건축법 시행령에 따라 필수적으로 제출해야 합니다.

출처: 건축법규_매뉴얼_2024.pdf, 페이지 5-7`;
    }
    
    if (lowerQuestion.includes('에너지절약') || lowerQuestion.includes('에너지')) {
      return `에너지절약계획서는 건축법 시행령 제3조에 따라 다음 항목을 포함해야 합니다:

- 건축물의 에너지 소비량 예측
- 단열재 및 창호 성능 기준
- 냉난방 설비 계획
- 신재생에너지 활용 계획
- 조명 설비 효율 계획

에너지절약계획서는 건축허가 신청 시 필수 서류이며, 전문가의 검토를 받아야 합니다.

출처: 건축법규_매뉴얼_2024.pdf, 페이지 12-15`;
    }
    
    if (lowerQuestion.includes('주차장') || lowerQuestion.includes('주차')) {
      return `주차장 설치 기준은 건축법 제61조 및 건축법 시행령 제119조에 규정되어 있습니다:

주거용 건축물: 1세대당 1대 이상
업무용 건축물: 연면적 200㎡당 1대 이상
상업용 건축물: 연면적 150㎡당 1대 이상
공업용 건축물: 연면적 300㎡당 1대 이상

주차장은 건축물의 용도와 규모에 따라 차등 적용되며, 지하주차장 설치 시 추가 기준이 적용됩니다.

출처: 건축법규_매뉴얼_2024.pdf, 페이지 8-10`;
    }
    
    if (lowerQuestion.includes('내진') || lowerQuestion.includes('지진')) {
      return `내진설계 의무 대상은 건축법 시행령 제95조에 따라 다음과 같습니다:

- 지하층이 있는 건축물
- 연면적 1,000㎡ 이상인 건축물
- 높이 13m 이상인 건축물
- 특별시, 광역시, 시 지역의 연면적 500㎡ 이상인 건축물

내진설계는 구조안전성 확보를 위해 필수이며, 구조계산서에 포함되어야 합니다.

출처: 건축법규_매뉴얼_2024.pdf, 페이지 18-20`;
    }
    
    // 기본 응답
    return `건축법 및 관련 법령에 대한 질문이시군요. 

업로드된 매뉴얼을 기반으로 답변드리겠습니다. 좀 더 구체적인 질문을 해주시면 더 정확한 답변을 제공할 수 있습니다.

예를 들어:
- "준주거지역 법령은 서류는?"
- "에너지절약계획서 작성 방법"
- "주차장 설치 기준 법령"
- "내진설계 의무 대상"

출처: 건축법규_매뉴얼_2024.pdf`;
  };

  // 시연용: 스트리밍 시뮬레이션 함수
  const simulateStreaming = async (
    fullText: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
  ) => {
    const words = fullText.split(/(\s+)/);
    let currentIndex = 0;

    const streamInterval = setInterval(() => {
      if (currentIndex >= words.length) {
        clearInterval(streamInterval);
        onComplete();
        return;
      }

      // 한 번에 2-3개 단어씩 전송하여 자연스러운 스트리밍 효과
      const chunkSize = Math.min(2 + Math.floor(Math.random() * 2), words.length - currentIndex);
      const chunk = words.slice(currentIndex, currentIndex + chunkSize).join('');
      onChunk(chunk);
      currentIndex += chunkSize;
    }, 50); // 50ms마다 청크 전송
  };

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
      // 시연용: 더미 AI 응답 스트리밍
      const mockResponse = getMockAIResponse(trimmed);
      let accumulatedContent = '';

      await simulateStreaming(
        mockResponse,
        (chunk: string) => {
          accumulatedContent += chunk;
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === optimisticAssistantMessage.id
                ? { ...msg, content: accumulatedContent }
                : msg
            )
          );
        },
        () => {
          if (!isMountedRef.current) return;
          setSending(false);
        }
      );

      // 실제 API 호출은 주석 처리 (시연용)
      // const payload = {
      //   content: trimmed,
      //   systemPrompt: advancedOptions.systemPrompt || undefined,
      //   model: advancedOptions.model || undefined,
      //   temperature: advancedOptions.temperature ?? undefined,
      //   maxTokens: advancedOptions.maxTokens ?? undefined,
      // };
      // let accumulatedContent = '';
      // await sendProjectMessageStream(
      //   selectedProjectId,
      //   payload,
      //   (chunk: string) => {
      //     accumulatedContent += chunk;
      //     setMessages((prev) =>
      //       prev.map((msg) =>
      //         msg.id === optimisticAssistantMessage.id
      //           ? { ...msg, content: accumulatedContent }
      //           : msg
      //       )
      //     );
      //   },
      //   async () => {
      //     if (!isMountedRef.current) return;
      //     await loadMessages(selectedProjectId);
      //     setSending(false);
      //   },
      //   (error: Error) => {
      //     if (!isMountedRef.current) return;
      //     setError(error.message || '메시지를 전송하지 못했습니다.');
      //     setMessages((prev) =>
      //       prev.filter(
      //         (msg) =>
      //           msg.id !== optimisticUserMessage.id && msg.id !== optimisticAssistantMessage.id
      //       )
      //     );
      //     setSending(false);
      //   }
      // );
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

