import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import ProjectPanel from '../components/ProjectPanel';
import ChatMessage from '../components/ChatMessage';
import ChatInput from '../components/ChatInput';
import './AIChat.css';

interface Message {
  id: number;
  type: 'ai' | 'user';
  message: string;
  timestamp: string;
  source?: string;
}

const AIChat = () => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 1,
      type: 'ai',
      message: `안녕하세요! 건축 허가 서류 작성을 도와드리는 AI 어시스턴트입니다.

다음과 같은 질문을 해주세요:

• "준주거지역에 3층 근린생활시설을 지으려면 어떤 서류가 필요한가요?"
• "건축허가신청서 작성 방법을 알려주세요"
• "소방시설 설치 기준을 보여주세요"
• "건축법 제11조에 대해 설명해주세요"`,
      timestamp: '오후 2:30',
    },
    {
      id: 2,
      type: 'user',
      message: '준주거지역에 3층 근린생활시설을 지으려면 어떤 서류가 필요한가요?',
      timestamp: '오후 2:35',
    },
    {
      id: 3,
      type: 'ai',
      message: `준주거지역 3층 근린생활시설을 필요한 서류

1. 기본 서류
• 건축허가신청서 (건축법 시행규칙 별지 제1호의2서식)
• 위임장 (대리인 신청 시)
• 설계자 자격 증명서류
• 토지 및 건물의 등기사항증명서
• 토지사용승낙서 (토지 소유자가 다를 경우)

2. 설계도서
• 배치도 (축척 1/100 이상)
• 평면도, 입면도, 단면도
• 구조도 및 구조계산서
• 소방시설도

3. 관련 검토 서류
• 건축물 에너지 절약 계획서
• 교통영향분석·개선대책
• 주차장 설치 계획서

더 자세한 내용이 필요하신가요?`,
      timestamp: '오후 2:30',
      source: '건축법 시행규칙 제7조, KCS 기준',
    },
  ]);

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

  const handleSendMessage = (message: string) => {
    const newMessage: Message = {
      id: messages.length + 1,
      type: 'user',
      message,
      timestamp: new Date().toLocaleTimeString('ko-KR', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: true 
      }),
    };
    setMessages([...messages, newMessage]);

    // AI 응답 시뮬레이션
    setTimeout(() => {
      const aiResponse: Message = {
        id: messages.length + 2,
        type: 'ai',
        message: '답변을 생성 중입니다...',
        timestamp: new Date().toLocaleTimeString('ko-KR', { 
          hour: '2-digit', 
          minute: '2-digit',
          hour12: true 
        }),
      };
      setMessages(prev => [...prev, aiResponse]);
    }, 1000);
  };

  const handleQuestionClick = (question: string) => {
    handleSendMessage(question);
  };

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
            <div className="messages-container">
              {messages.map((msg) => (
                <ChatMessage
                  key={msg.id}
                  type={msg.type}
                  message={msg.message}
                  timestamp={msg.timestamp}
                  source={msg.source}
                />
              ))}
            </div>
            <ChatInput onSend={handleSendMessage} />
          </div>
        </div>
      </main>
    </div>
  );
};

export default AIChat;

