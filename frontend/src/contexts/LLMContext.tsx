import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import type { LLMProvider, LLMProviderInfo } from '../types/api';
import { getLLMProviders } from '../services/api';

interface LLMSettings {
  provider: LLMProvider;
  modelName: string;
}

interface LLMContextType {
  settings: LLMSettings;
  setSettings: (settings: LLMSettings) => void;
  providers: LLMProviderInfo[];
  loadingProviders: boolean;
  currentProviderInfo: LLMProviderInfo | undefined;
  currentModelInfo: { name: string; displayName: string; description: string } | undefined;
}

const defaultSettings: LLMSettings = {
  provider: 'OPENAI',
  modelName: 'gpt-3.5-turbo',
};

const LLMContext = createContext<LLMContextType | undefined>(undefined);

const STORAGE_KEY = 'twinsolution_llm_settings';

export const LLMProvider = ({ children }: { children: ReactNode }) => {
  const [settings, setSettingsState] = useState<LLMSettings>(() => {
    // localStorage에서 설정 로드
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return defaultSettings;
      }
    }
    return defaultSettings;
  });

  const [providers, setProviders] = useState<LLMProviderInfo[]>([]);
  const [loadingProviders, setLoadingProviders] = useState(true);

  // Provider 목록 로드
  useEffect(() => {
    const fetchProviders = async () => {
      try {
        console.log('LLM Providers 로드 시작...');
        const response = await getLLMProviders();
        console.log('LLM Providers 로드 성공:', response);
        setProviders(response.providers);

        // Ollama 모델이 있는지 확인
        const ollamaProvider = response.providers.find(p => p.id === 'OLLAMA');
        if (ollamaProvider) {
          console.log('Ollama 사용 가능:', ollamaProvider.available);
          console.log('Ollama 모델 목록:', ollamaProvider.models);
        }
      } catch (err) {
        console.error('LLM Providers 로드 실패:', err);
        console.error('에러 상세:', err instanceof Error ? err.message : String(err));
        // 실패 시 기본 OpenAI 옵션만 표시
        setProviders([{
          name: 'OpenAI',
          id: 'OPENAI',
          available: true,
          models: [
            { name: 'gpt-3.5-turbo', displayName: 'GPT-3.5 Turbo', description: '빠르고 효율적인 모델' },
            { name: 'gpt-4', displayName: 'GPT-4', description: '더 정확한 추론 가능' },
          ]
        }]);
      } finally {
        setLoadingProviders(false);
      }
    };
    fetchProviders();
  }, []);

  // 설정 변경 시 localStorage에 저장
  const setSettings = (newSettings: LLMSettings) => {
    setSettingsState(newSettings);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newSettings));
  };

  // 현재 Provider 정보
  const currentProviderInfo = providers.find(p => p.id === settings.provider);

  // 현재 모델 정보
  const currentModelInfo = currentProviderInfo?.models.find(m => m.name === settings.modelName);

  return (
    <LLMContext.Provider value={{
      settings,
      setSettings,
      providers,
      loadingProviders,
      currentProviderInfo,
      currentModelInfo,
    }}>
      {children}
    </LLMContext.Provider>
  );
};

export const useLLM = () => {
  const context = useContext(LLMContext);
  if (!context) {
    throw new Error('useLLM must be used within a LLMProvider');
  }
  return context;
};

export default LLMContext;
