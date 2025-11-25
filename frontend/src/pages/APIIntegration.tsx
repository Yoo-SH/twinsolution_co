import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import APICard from '../components/APICard';
import {
  createApi,
  deleteApi,
  getApis,
  testApi,
  updateApi,
  updateApiStatus,
} from '../services/api';
import type { ApiIntegrationItem } from '../types/api';
import './APIIntegration.css';

const APIIntegration = () => {
  const [apis, setApis] = useState<ApiIntegrationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const loadApis = useCallback(async () => {
    if (!isMountedRef.current) return;
    setLoading(true);
    setError(null);

    try {
      const data = await getApis();
      if (!isMountedRef.current) return;
      setApis(data);
    } catch (err) {
      if (!isMountedRef.current) return;
      const message =
        err instanceof Error ? err.message : 'API 목록을 불러오지 못했습니다.';
      setError(message);
    } finally {
      if (!isMountedRef.current) return;
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadApis();
  }, [loadApis]);

  const stats = useMemo(() => {
    const total = apis.length;
    const active = apis.filter((api) => api.status === 'active').length;
    const errorCount = apis.filter((api) => api.status === 'error').length;
    const inactive = total - active - errorCount;

    return [
      {
        title: '전체 API',
        value: total,
        trend: '',
        icon: '🌐',
        iconColor: 'rgba(200, 200, 200, 0.3)',
      },
      {
        title: '활성화',
        value: active,
        trend: '',
        icon: '✅',
        iconColor: 'rgba(144, 238, 144, 0.3)',
      },
      {
        title: '오류',
        value: errorCount,
        trend: '',
        icon: '⚠️',
        iconColor: 'rgba(255, 182, 193, 0.3)',
      },
      {
        title: '비활성화',
        value: inactive,
        trend: '',
        icon: '⭕',
        iconColor: 'rgba(255, 255, 224, 0.5)',
      },
    ];
  }, [apis]);

  const handleTest = async (id: number) => {
    try {
      const result = await testApi(id);
      alert(`상태: ${result.status}\n메시지: ${result.message}`);
    } catch (err) {
      const message = err instanceof Error ? err.message : '테스트를 실행하지 못했습니다.';
      alert(message);
    }
  };

  const handleToggleStatus = async (api: ApiIntegrationItem) => {
    const nextStatus = api.status === 'active' ? 'inactive' : 'active';
    try {
      await updateApiStatus(api.id, { status: nextStatus });
      await loadApis();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : '상태를 변경하지 못했습니다.';
      alert(message);
    }
  };

  const handleEdit = async (api: ApiIntegrationItem) => {
    const name = window.prompt('API 이름을 입력하세요', api.name);
    if (!name) return;
    const baseUrl = window.prompt('API URL을 입력하세요', api.baseUrl);
    if (!baseUrl) return;
    const method = window.prompt('HTTP 메서드 (GET/POST 등)', api.method) ?? api.method;
    const authKeyInput = window.prompt('인증 키(선택)', api.authKey ?? '');
    const nextAuthKey =
      authKeyInput === null ? api.authKey : authKeyInput.trim() ? authKeyInput : undefined;

    try {
      await updateApi(api.id, { name, baseUrl, method, authKey: nextAuthKey ?? undefined });
      await loadApis();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'API 정보를 수정하지 못했습니다.';
      alert(message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('API를 삭제하시겠습니까?')) return;
    try {
      await deleteApi(id);
      await loadApis();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'API를 삭제하지 못했습니다.';
      alert(message);
    }
  };

  const handleAddAPI = async () => {
    const name = window.prompt('API 이름을 입력하세요');
    if (!name) return;
    const baseUrl = window.prompt('API URL을 입력하세요');
    if (!baseUrl) return;
    const method = window.prompt('HTTP 메서드 (GET/POST 등)', 'GET') ?? 'GET';
    const authKeyInput = window.prompt('인증 키(선택)');
    const authKey = authKeyInput && authKeyInput.trim() ? authKeyInput : undefined;

    try {
      await createApi({ name, baseUrl, method, authKey });
      await loadApis();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'API를 추가하지 못했습니다.';
      alert(message);
    }
  };

  return (
    <div className="api-integration-layout">
      <Sidebar />
      <main className="api-integration-main">
        <Header title="API 연동" />
        <div className="api-integration-content">
          {error && (
            <div className="api-error">
              <span>{error}</span>
              <button type="button" className="api-retry-button" onClick={loadApis}>
                다시 시도
              </button>
            </div>
          )}

          <div className="api-header-actions">
            <button className="add-api-btn" onClick={handleAddAPI}>
              + API 추가
            </button>
          </div>

          <section className="api-stats-section">
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

          <section className="api-cards-section">
            {loading ? (
              <div className="api-loading">API 정보를 불러오는 중입니다...</div>
            ) : apis.length ? (
              apis.map((api) => (
                <APICard
                  key={api.id}
                  name={api.name}
                  url={api.baseUrl}
                  method={api.method}
                  status={api.status}
                  category="외부 연동"
                  onTest={() => handleTest(api.id)}
                  onRefresh={() => handleToggleStatus(api)}
                  onEdit={() => handleEdit(api)}
                  onDelete={() => handleDelete(api.id)}
                />
              ))
            ) : (
              <div className="api-loading">등록된 API가 없습니다.</div>
            )}
          </section>

          <div className="api-info-panel">
            <div className="info-panel-header">
              <span className="info-icon">🌍</span>
              <h3>API 연동 안내</h3>
            </div>
            <ul className="info-list">
              <li>• API 추가 후 '연결 테스트'로 즉시 연결 상태를 확인하세요.</li>
              <li>• 스케줄 관리는 추후 Airflow DAG로 확장될 예정입니다.</li>
              <li>• 수집된 데이터는 Vector DB에 자동으로 적재됩니다.</li>
              <li>• 민감한 API 키는 환경 변수로 안전하게 관리하세요.</li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  );
};

export default APIIntegration;

