import { useEffect, useMemo, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import FileUpload from '../components/FileUpload';
import DocumentTable from '../components/DocumentTable';
import SearchBar from '../components/SearchBar';
import FilterDropdown from '../components/FilterDropdown';
import { deleteDocument, uploadDocument } from '../services/api';
import { useProjects } from '../hooks/useProjects';
import { useDocuments } from '../hooks/useDocuments';
import type { Project } from '../types/api';
import './RAGManagement.css';

const STATUS_FILTERS = ['전체 상태', '업로드됨', '분석완료'];

const RAGManagement = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState(STATUS_FILTERS[0]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [uploading, setUploading] = useState(false);

  const {
    projects,
    loading: projectsLoading,
    error: projectsError,
    setProjects,
  } = useProjects(20);

  const {
    documents,
    loading: documentsLoading,
    error: documentsError,
    refetch: refetchDocuments,
    setDocuments,
  } = useDocuments(selectedProjectId);

  const error = projectsError || documentsError;

  useEffect(() => {
    if (projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  useEffect(() => {
    const handleProjectCreated = (event: Event) => {
      const newProject = (event as CustomEvent<Project>).detail;
      setProjects((prev) => {
        const exists = prev.some((project) => project.id === newProject.id);
        const updated = exists
          ? prev.map((project) => (project.id === newProject.id ? newProject : project))
          : [newProject, ...prev];
        return updated.slice(0, 20);
      });
      setSelectedProjectId(newProject.id);
    };

    window.addEventListener('project-created', handleProjectCreated as EventListener);
    return () => {
      window.removeEventListener('project-created', handleProjectCreated as EventListener);
    };
  }, [setProjects]);

  const filteredDocuments = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    return documents.filter((doc) => {
      const matchesQuery = normalizedQuery
        ? doc.name.toLowerCase().includes(normalizedQuery)
        : true;
      const matchesStatus =
        statusFilter === '전체 상태' ? true : doc.status === statusFilter;
      return matchesQuery && matchesStatus;
    });
  }, [documents, searchQuery, statusFilter]);

  const stats = useMemo(() => {
    const completed = documents.filter((doc) => doc.status === '분석완료').length;
    const processing = documents.length - completed;
    const chunkTotal = documents.reduce((total, doc) => total + (doc.chunkCount ?? 0), 0);

    return [
      {
        title: '전체 문서',
        value: documents.length,
        trend: selectedProjectId ? `프로젝트 ID ${selectedProjectId}` : '',
        icon: '📄',
        iconColor: 'rgba(200, 200, 200, 0.3)',
      },
      {
        title: '완료된 문서',
        value: completed,
        trend: '',
        icon: '✅',
        iconColor: 'rgba(144, 238, 144, 0.3)',
      },
      {
        title: '처리 중',
        value: processing,
        trend: '',
        icon: '⏳',
        iconColor: 'rgba(255, 255, 224, 0.5)',
      },
      {
        title: '총 청크',
        value: chunkTotal,
        trend: '',
        icon: '📊',
        iconColor: 'rgba(173, 216, 230, 0.4)',
      },
    ];
  }, [documents, selectedProjectId]);

  const handleFileSelect = async (file: File) => {
    if (!selectedProjectId) {
      alert('먼저 프로젝트를 선택하세요.');
      return;
    }

    setUploading(true);

    try {
      // 실제 API 호출
      const uploaded = await uploadDocument(selectedProjectId, file);
      setDocuments((prev) => [uploaded, ...prev]);

      // 시연용: 더미 데이터 반환 (주석 처리)
      // await new Promise((resolve) => setTimeout(resolve, 1500)); // 업로드 시뮬레이션 (1.5초)
      // const mockDocument = {
      //   id: Date.now(),
      //   projectId: selectedProjectId,
      //   name: file.name || '건축법규_매뉴얼_2024.pdf',
      //   fileType: file.type.includes('pdf') ? 'pdf' : file.name.split('.').pop() || 'pdf',
      //   filePath: `/uploads/documents/${Date.now()}-${file.name}`,
      //   origin: '업로드',
      //   status: '분할완료' as const,
      //   chunkSize: 1000,
      //   chunkOverlap: 200,
      //   chunkCount: 18,
      //   hasAnalysisReport: true,
      //   createdAt: new Date().toISOString(),
      //   updatedAt: new Date().toISOString(),
      // };
      // setDocuments((prev) => [mockDocument, ...prev]);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : '문서를 업로드하지 못했습니다.';
      alert(message);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('문서를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await deleteDocument(id);
      setDocuments((prev) => prev.filter((doc) => doc.id !== id));
    } catch (err) {
      const message =
        err instanceof Error ? err.message : '문서를 삭제하지 못했습니다.';
      alert(message);
    }
  };

  const handleView = (id: number) => {
    alert(`문서 ID ${id} 미리보기 기능은 추후 제공 예정입니다.`);
  };

  const handleDownload = (id: number) => {
    alert(`문서 ID ${id} 다운로드 기능은 추후 제공 예정입니다.`);
  };

  return (
    <div className="rag-management-layout">
      <Sidebar />
      <main className="rag-management-main">
        <Header title="문서 관리" />
        <div className="rag-management-content">
          {error && (
            <div className="rag-error">
              <span>{error}</span>
              {selectedProjectId && (
                <button
                  type="button"
                  className="rag-retry-button"
                  onClick={refetchDocuments}
                >
                  다시 시도
                </button>
              )}
            </div>
          )}

          <section className="rag-stats-section">
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

          <div className="project-selector">
            <div>
              <label htmlFor="projectSelect">프로젝트 선택</label>
              <select
                id="projectSelect"
                value={selectedProjectId ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setSelectedProjectId(value ? Number(value) : null);
                }}
                disabled={projectsLoading}
              >
                {!projects.length && <option value="">프로젝트 없음</option>}
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              className="refresh-button"
              disabled={!selectedProjectId || documentsLoading}
              onClick={refetchDocuments}
            >
              새로고침
            </button>
          </div>

          <FileUpload onFileSelect={handleFileSelect} isUploading={uploading} />

          <section className="rag-search-filter-section">
            <SearchBar
              value={searchQuery}
              onChange={setSearchQuery}
              placeholder="문서 검색"
            />
            <FilterDropdown
              value={statusFilter}
              options={STATUS_FILTERS}
              onChange={setStatusFilter}
            />
          </section>

          <DocumentTable
            documents={filteredDocuments}
            loading={documentsLoading}
            onView={handleView}
            onDownload={handleDownload}
            onDelete={handleDelete}
            emptyMessage={
              selectedProjectId
                ? '등록된 문서가 없습니다.'
                : '프로젝트를 선택하면 문서를 볼 수 있습니다.'
            }
          />
        </div>
      </main>
    </div>
  );
};

export default RAGManagement;

