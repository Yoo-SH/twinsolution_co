import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import APICard from '../components/APICard';
import './APIIntegration.css';

interface APIConfig {
  id: number;
  name: string;
  url: string;
  method: string;
  schedule: string;
  lastSync: string;
  nextSync: string;
  status: 'active' | 'error';
  category: string;
  errorMessage?: string;
}

const APIIntegration = () => {
  const [apis] = useState<APIConfig[]>([
    {
      id: 1,
      name: '조달청 나라장터',
      url: 'https://www.g2b.go.kr/api/construction-prices',
      method: 'GET',
      schedule: '매주 월요일 00:00',
      lastSync: '2024-11-11 00:00:15',
      nextSync: '2024-11-18 00:00:00',
      status: 'active',
      category: '단가정보',
    },
    {
      id: 2,
      name: '국가법령정보센터',
      url: 'https://www.law.go.kr/api/building-law',
      method: 'GET',
      schedule: '매월 1일 00:00',
      lastSync: '2024-11-01 00:05:22',
      nextSync: '2024-12-01 00:00:00',
      status: 'active',
      category: '법령',
    },
    {
      id: 3,
      name: '한국건설기술연구원',
      url: 'https://www.kict.re.kr/api/standards',
      method: 'GET',
      schedule: '매주 수요일 03:00',
      lastSync: '2024-11-06 03:00:00',
      nextSync: '2024-11-13 03:00:00',
      status: 'error',
      category: '표준품셈',
      errorMessage: '연결 실패 : 인증 토큰이 만료되었습니다. API 키를 확인해주세요.',
    },
  ]);

  const stats = [
    {
      title: '전체 API',
      value: 3,
      trend: '',
      icon: '🌐',
      iconColor: 'rgba(200, 200, 200, 0.3)',
    },
    {
      title: '활성화',
      value: 2,
      trend: '',
      icon: '✅',
      iconColor: 'rgba(144, 238, 144, 0.3)',
    },
    {
      title: '오류',
      value: 1,
      trend: '',
      icon: '⚠️',
      iconColor: 'rgba(255, 182, 193, 0.3)',
    },
    {
      title: '비활성화',
      value: 0,
      trend: '',
      icon: '⭕',
      iconColor: 'rgba(255, 255, 224, 0.5)',
    },
  ];

  const handleTest = (id: number) => {
    console.log('Test API:', id);
  };

  const handleRefresh = (id: number) => {
    console.log('Refresh API:', id);
  };

  const handleEdit = (id: number) => {
    console.log('Edit API:', id);
  };

  const handleDelete = (id: number) => {
    console.log('Delete API:', id);
  };

  const handleAddAPI = () => {
    console.log('Add new API');
  };

  return (
    <div className="api-integration-layout">
      <Sidebar />
      <main className="api-integration-main">
        <Header title="API 연동" />
        <div className="api-integration-content">
          {/* Header Actions */}
          <div className="api-header-actions">
            <button className="add-api-btn" onClick={handleAddAPI}>
              + API 추가
            </button>
          </div>

          {/* Statistics Section */}
          <section className="api-stats-section">
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

          {/* API Cards List */}
          <section className="api-cards-section">
            {apis.map((api) => (
              <APICard
                key={api.id}
                name={api.name}
                url={api.url}
                method={api.method}
                schedule={api.schedule}
                lastSync={api.lastSync}
                nextSync={api.nextSync}
                status={api.status}
                category={api.category}
                errorMessage={api.errorMessage}
                onTest={() => handleTest(api.id)}
                onRefresh={() => handleRefresh(api.id)}
                onEdit={() => handleEdit(api.id)}
                onDelete={() => handleDelete(api.id)}
              />
            ))}
          </section>

          {/* Info Panel */}
          <div className="api-info-panel">
            <div className="info-panel-header">
              <span className="info-icon">🌍</span>
              <h3>API 연동 안내</h3>
            </div>
            <ul className="info-list">
              <li>• API 추가 후 '연결 테스트'로 바로는 연결을 확인하세요.</li>
              <li>• 스케줄은 Airflow DAG으로 관리됩니다.</li>
              <li>• 수집된 데이터는 자동으로 Vector DB에 임베딩됩니다.</li>
              <li>• 민감한 API 키는 환경 변수로 관리됩니다.</li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  );
};

export default APIIntegration;

