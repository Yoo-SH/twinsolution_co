import apiClient from '../lib/apiClient';
import type {
  ApiIntegrationItem,
  ApiIntegrationPayload,
  ApiLogItem,
  ApiStatusUpdatePayload,
  ApiTestResponse,
  ChatMessage,
  ChatMessagePayload,
  ChunkPreviewRequest,
  ChunkPreviewResponse,
  DocumentChunk,
  ChunkSettings,
  ChunkSettingsRequest,
  DashboardSummary,
  DocumentItem,
  DocumentUploadResponse,
  Project,
  ProjectRequest,
  ProjectUpdateRequest,
} from '../types/api';

const DASHBOARD_BASE = '/api/dashboard';
const PROJECTS_BASE = '/api/projects';
const DOCUMENTS_BASE = '/api/documents';
const INTEGRATION_BASE = '/api/integration/apis';
const SETTINGS_BASE = '/api/settings';

// Dashboard
export const getDashboardSummary = () =>
  apiClient.get<DashboardSummary>(`${DASHBOARD_BASE}/summary`);

export const getRecentProjects = (limit = 5) =>
  apiClient.get<Project[]>(PROJECTS_BASE, { query: { limit } });

// Documents
export const getProjectDocuments = (projectId: number) =>
  apiClient.get<DocumentItem[]>(`${PROJECTS_BASE}/${projectId}/documents`);

export const uploadDocument = (projectId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.post<DocumentUploadResponse>(
    `${PROJECTS_BASE}/${projectId}/documents`,
    formData,
  );
};

export const deleteDocument = (documentId: number) =>
  apiClient.delete<void>(`${DOCUMENTS_BASE}/${documentId}`);

export const rebuildDocumentChunks = (documentId: number) =>
  apiClient.post<void>(`${DOCUMENTS_BASE}/${documentId}/chunks/rebuild`);

export const getDocumentChunks = (documentId: number) =>
  apiClient.get<DocumentChunk[]>(`${DOCUMENTS_BASE}/${documentId}/chunks`);

// Project-based chat
export const getProjectMessages = (projectId: number, page?: number, size?: number) =>
  apiClient.get<ChatMessage[]>(`${PROJECTS_BASE}/${projectId}/messages`, {
    query: { page, size },
  });

export const sendProjectMessage = (projectId: number, payload: ChatMessagePayload) =>
  apiClient.post<ChatMessage>(`${PROJECTS_BASE}/${projectId}/messages`, payload);

// Chunk settings
export const getChunkSettings = () =>
  apiClient.get<ChunkSettings>(`${SETTINGS_BASE}/chunk`);

export const updateChunkSettings = (payload: ChunkSettingsRequest) =>
  apiClient.put<ChunkSettings>(`${SETTINGS_BASE}/chunk`, payload);

export const previewChunks = (payload: ChunkPreviewRequest) =>
  apiClient.post<ChunkPreviewResponse>(`${SETTINGS_BASE}/chunk/preview`, payload);

// API integrations
export const getApis = () =>
  apiClient.get<ApiIntegrationItem[]>(INTEGRATION_BASE);

export const createApi = (payload: ApiIntegrationPayload) =>
  apiClient.post<ApiIntegrationItem>(INTEGRATION_BASE, payload);

export const updateApi = (apiId: number, payload: ApiIntegrationPayload) =>
  apiClient.put<ApiIntegrationItem>(`${INTEGRATION_BASE}/${apiId}`, payload);

export const updateApiStatus = (apiId: number, payload: ApiStatusUpdatePayload) =>
  apiClient.patch<ApiIntegrationItem>(`${INTEGRATION_BASE}/${apiId}/status`, payload);

export const deleteApi = (apiId: number) =>
  apiClient.delete<void>(`${INTEGRATION_BASE}/${apiId}`);

export const testApi = (apiId: number) =>
  apiClient.post<ApiTestResponse>(`${INTEGRATION_BASE}/${apiId}/test-call`);

export const getApiLogs = (apiId: number, status?: string, limit = 50) =>
  apiClient.get<ApiLogItem[]>(`${INTEGRATION_BASE}/${apiId}/logs`, {
    query: { status, limit },
  });

// Projects helpers
export const getProject = (projectId: number) =>
  apiClient.get<Project>(`${PROJECTS_BASE}/${projectId}`);

export const createProject = (payload: ProjectRequest) =>
  apiClient.post<Project>(PROJECTS_BASE, payload);

export const updateProject = (projectId: number, payload: ProjectUpdateRequest) =>
  apiClient.patch<Project>(`${PROJECTS_BASE}/${projectId}`, payload);

export const deleteProject = (projectId: number) =>
  apiClient.delete<void>(`${PROJECTS_BASE}/${projectId}`);

