import { useState, useEffect } from 'react';
import { getRecentProjects } from '../services/api';
import type { Project } from '../types/api';

export const useProjects = (limit: number = 20) => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadProjects = async () => {
    setLoading(true);
    setError(null);

    try {
      const list = await getRecentProjects(limit);
      setProjects(list);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : '프로젝트 목록을 불러오지 못했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProjects();
  }, [limit]);

  return {
    projects,
    loading,
    error,
    refetch: loadProjects,
    setProjects,
  };
};
