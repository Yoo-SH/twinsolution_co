import { useCallback, useEffect, useState } from 'react';
import { getApis } from '../services/api';
import type { ApiIntegrationItem } from '../types/api';

export const useApis = () => {
  const [apis, setApis] = useState<ApiIntegrationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadApis = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getApis();
      setApis(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'API 목록을 불러오지 못했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadApis();
  }, [loadApis]);

  return {
    apis,
    loading,
    error,
    loadApis,
    setApis,
  };
};
