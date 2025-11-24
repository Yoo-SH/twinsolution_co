import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import ChartCard from '../components/ChartCard';
import ProjectTable from '../components/ProjectTable';
import ActionCard from '../components/ActionCard';
import './Dashboard.css';

const Dashboard = () => {
  const stats = [
    {
      title: '생성된 서류',
      value: 127,
      trend: '+12%',
      icon: '📄',
      iconColor: 'rgba(27, 38, 59, 0.2)',
    },
    {
      title: '처리 중',
      value: 8,
      trend: '+3%',
      icon: '⏰',
      iconColor: 'rgba(237, 197, 49, 0.2)',
    },
    {
      title: '완료된 프로젝트',
      value: 45,
      trend: '+5%',
      icon: '✅',
      iconColor: 'rgba(76, 175, 80, 0.2)',
    },
    {
      title: '대화 세션',
      value: 234,
      trend: '+23%',
      icon: '💬',
      iconColor: 'rgba(123, 104, 238, 0.2)',
    },
  ];

  const actions = [
    {
      title: '새 서류 작성',
      description: 'AI 어시스턴트로 서류를 빠르게 작성하세요',
      icon: '📝',
      color: '#7b68ee',
    },
    {
      title: '문서 업로드',
      description: 'RAG 시스템에 새로운 문서를 추가하세요',
      icon: '⬆️',
      color: '#415a77',
    },
    {
      title: '분석 리포트',
      description: '프로젝트 진행 상황을 확인하세요',
      icon: '📊',
      color: '#4caf50',
    },
  ];

  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="dashboard-main">
        <Header title="대시보드" />
        <div className="dashboard-content">
          <section className="stats-section">
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

          <section className="charts-section">
            <ChartCard title="서류 생성 추이" />
            <ChartCard title="서류 생성 추이" />
          </section>

          <ProjectTable />

          <section className="actions-section">
            {actions.map((action, index) => (
              <ActionCard
                key={index}
                title={action.title}
                description={action.description}
                icon={action.icon}
                color={action.color}
              />
            ))}
          </section>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;

