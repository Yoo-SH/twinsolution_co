import { useCallback, useEffect, useState } from 'react';
import { getChunkSettings, updateChunkSettings } from '../services/api';
import type { ChunkSettings, ChunkSettingsRequest } from '../types/api';

export const useChunkSettings = () => {
  const [settings, setSettings] = useState<ChunkSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadSettings = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getChunkSettings();
      setSettings(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : '청킹 설정을 불러오지 못했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const saveSettings = useCallback(async (payload: ChunkSettingsRequest) => {
    setError(null);
    try {
      const updated = await updateChunkSettings(payload);
      setSettings(updated);
      return updated;
    } catch (err) {
      const message = err instanceof Error ? err.message : '설정을 저장하지 못했습니다.';
      setError(message);
      throw err;
    }
  }, []);

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  return {
    settings,
    loading,
    error,
    loadSettings,
    saveSettings,
  };
};
