import { useState, useEffect } from 'react';
import { getProjectDocuments } from '../services/api';
import type { DocumentItem } from '../types/api';

export const useDocuments = (projectId: number | null) => {
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadDocuments = async () => {
    if (!projectId) {
      setDocuments([]);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const items = await getProjectDocuments(projectId);
      setDocuments(items);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : '문서를 불러오지 못했습니다.';
      setError(message);
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDocuments();
  }, [projectId]);

  return {
    documents,
    loading,
    error,
    refetch: loadDocuments,
    setDocuments,
  };
};
