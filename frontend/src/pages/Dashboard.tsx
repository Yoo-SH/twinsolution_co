import { useCallback, useEffect, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import ChartCard from '../components/ChartCard';
import ProjectTable from '../components/ProjectTable';
import ActionCard from '../components/ActionCard';
import { getDashboardSummary, getRecentProjects } from '../services/api';
import type { DashboardSummary, Project } from '../types/api';
import './Dashboard.css';

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

const formatDelta = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '-';
  }
  const sign = value >= 0 ? '+' : '';
  return `${sign}${value}%`;
};

const Dashboard = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [projectLoading, setProjectLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const loadDashboard = useCallback(async () => {
    if (!isMountedRef.current) return;

    setSummaryLoading(true);
    setProjectLoading(true);
    setError(null);

    try {
      const [summaryData, projectData] = await Promise.all([
        getDashboardSummary(),
        getRecentProjects(4),
      ]);

      if (!isMountedRef.current) return;

      setSummary(summaryData);
      setProjects(projectData);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : '대시보드 데이터를 불러오지 못했습니다.';
      setError(message);
    } finally {
      if (!isMountedRef.current) return;
      setSummaryLoading(false);
      setProjectLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const stats = [
    {
      title: '전체 문서',
      value: summaryLoading ? '...' : summary?.totalDocuments ?? 0,
      trend: summaryLoading
        ? '불러오는 중'
        : `최근 30일 대비 ${formatDelta(summary?.documentsDelta)}`,
      icon: '📄',
      iconColor: 'rgba(27, 38, 59, 0.2)',
    },
    {
      title: '진행 중 프로젝트',
      value: summaryLoading ? '...' : summary?.inProgressProjects ?? 0,
      trend: '실시간 집계',
      icon: '⏰',
      iconColor: 'rgba(237, 197, 49, 0.2)',
    },
    {
      title: '완료된 프로젝트',
      value: summaryLoading ? '...' : summary?.completedProjects ?? 0,
      trend: '실시간 집계',
      icon: '✅',
      iconColor: 'rgba(76, 175, 80, 0.2)',
    },
    {
      title: '대화 세션',
      value: summaryLoading ? '...' : summary?.chatSessionsCount ?? 0,
      trend: summaryLoading
        ? '불러오는 중'
        : `최근 30일 대비 ${formatDelta(summary?.chatSessionsDelta)}`,
      icon: '💬',
      iconColor: 'rgba(123, 104, 238, 0.2)',
    },
  ];

  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="dashboard-main">
        <Header title="대시보드" />
        <div className="dashboard-content">
          {error && (
            <div className="dashboard-error">
              <span>{error}</span>
              <button type="button" className="retry-button" onClick={loadDashboard}>
                다시 시도
              </button>
            </div>
          )}

          <section className="stats-section">
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

          <section className="charts-section">
            <ChartCard title="서류 생성 추이" />
            <ChartCard title="대화 세션 추이" />
          </section>

          <ProjectTable
            projects={projects}
            loading={projectLoading}
            emptyMessage={error ? '프로젝트 데이터를 불러오지 못했습니다.' : undefined}
          />

          <section className="actions-section">
            {actions.map((action) => (
              <ActionCard
                key={action.title}
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

