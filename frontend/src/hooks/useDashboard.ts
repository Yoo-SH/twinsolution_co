import { useState, useEffect } from 'react';
import { getDashboardSummary, getRecentProjects } from '../services/api';
import type { DashboardSummary, Project } from '../types/api';

export const useDashboard = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = async () => {
    setLoading(true);
    setError(null);

    try {
      const [summaryData, projectData] = await Promise.all([
        getDashboardSummary(),
        getRecentProjects(4),
      ]);

      setSummary(summaryData);
      setProjects(projectData);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : '대시보드 데이터를 불러오지 못했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  return {
    summary,
    projects,
    loading,
    error,
    refetch: loadDashboard,
  };
};
