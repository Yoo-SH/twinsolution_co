import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import SearchBar from '../components/SearchBar';
import FilterDropdown from '../components/FilterDropdown';
import HistoryCard from '../components/HistoryCard';
import './History.css';

const History = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterValue, setFilterValue] = useState('전체');
  const [activeTab, setActiveTab] = useState('대화 내역');

  const stats = [
    {
      title: '전체 대화',
      value: 4,
      trend: '',
      icon: '💬',
      iconColor: 'rgba(200, 200, 200, 0.3)',
    },
    {
      title: '생성된 서류',
      value: 5,
      trend: '',
      icon: '📄',
      iconColor: 'rgba(144, 238, 144, 0.3)',
    },
    {
      title: '이번 주 활동',
      value: 24,
      trend: '',
      icon: '📅',
      iconColor: 'rgba(255, 182, 193, 0.3)',
    },
    {
      title: '총 저장 용량',
      value: '6.5GB',
      trend: '',
      icon: '💾',
      iconColor: 'rgba(255, 255, 224, 0.5)',
    },
  ];

  const conversations = [
    {
      id: 1,
      title: '준주거지역 건축허가 문의',
      description: '준주거지역에 3층 근린생활시설을 지으려면 어떤 서류가...',
      date: '2024-11-13 03:00',
      project: '전주 덕진구 근린생활시설',
      messageCount: 15,
    },
    {
      id: 2,
      title: '에너지절약설계 작성법',
      description: '에너지절약설계서 작성 시 필수 항목에 대해...',
      date: '2024-11-11 10:15',
      project: '전주 덕진구 근린생활시설',
      messageCount: 8,
    },
    {
      id: 3,
      title: '주차장 설치 기준 확인',
      description: '근린생활시설 주차장 설치 기준은 어떻게 되나요?',
      date: '2024-11-10 16:45',
      project: '전주 덕진구 근린생활시설',
      messageCount: 12,
    },
    {
      id: 4,
      title: '소방시설 관련 법령',
      description: '3층 건물의 소방시설 설치 의무에 대해...',
      date: '2024-11-09 09:20',
      project: '전주 덕진구 근린생활시설',
      messageCount: 6,
    },
  ];

  const filterOptions = ['전체', '최근 7일', '최근 30일', '최근 90일'];

  return (
    <div className="history-layout">
      <Sidebar />
      <main className="history-main">
        <Header title="히스토리" />
        <div className="history-content">
          {/* Statistics Section */}
          <section className="history-stats-section">
            {stats.map((stat, index) => (
              <StatCard
                key={index}
                title={stat.title}
                value={stat.value}
                trend={stat.trend}
                icon={stat.icon}
                iconColor={stat.iconColor}
              />
            ))}
          </section>

          {/* Tabs Section */}
          <section className="tabs-section">
            <div className="tabs">
              <button
                className={`tab ${activeTab === '대화 내역' ? 'active' : ''}`}
                onClick={() => setActiveTab('대화 내역')}
              >
                대화 내역
                <span className="tab-badge">4</span>
              </button>
              <button
                className={`tab ${activeTab === '생성된 서류' ? 'active' : ''}`}
                onClick={() => setActiveTab('생성된 서류')}
              >
                생성된 서류
                <span className="tab-badge">5</span>
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

          {/* Search and Filter Section */}
          <section className="search-filter-section">
            <SearchBar
              value={searchQuery}
              onChange={setSearchQuery}
              placeholder="검색"
            />
            <FilterDropdown
              value={filterValue}
              options={filterOptions}
              onChange={setFilterValue}
            />
          </section>

          {/* Conversations List */}
          <section className="conversations-section">
            {conversations.map((conversation) => (
              <HistoryCard
                key={conversation.id}
                title={conversation.title}
                description={conversation.description}
                date={conversation.date}
                project={conversation.project}
                messageCount={conversation.messageCount}
                onPlay={() => console.log('Play:', conversation.id)}
                onEdit={() => console.log('Edit:', conversation.id)}
                onDelete={() => console.log('Delete:', conversation.id)}
              />
            ))}
          </section>
        </div>
      </main>
    </div>
  );
};

export default History;

