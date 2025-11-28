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
  ChatSessionRequest,
  ChatSessionResponse,
  QuickQuestionRequest,
  GeneratedDocumentRequest,
  AnalysisReportResponse,
} from '../types/api';

const DASHBOARD_BASE = '/api/dashboard';
const PROJECTS_BASE = '/api/projects';
const DOCUMENTS_BASE = '/api/documents';
const INTEGRATION_BASE = '/api/integration/apis';
const SETTINGS_BASE = '/api/settings';
const CHAT_SESSIONS_BASE = '/api/chat-sessions';

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

// Chat Sessions
export const createChatSession = (projectId: number, payload?: ChatSessionRequest) =>
  apiClient.post<ChatSessionResponse>(`${PROJECTS_BASE}/${projectId}/chat-sessions`, payload);

export const getChatSessionsByProject = (projectId: number) =>
  apiClient.get<ChatSessionResponse[]>(`${PROJECTS_BASE}/${projectId}/chat-sessions`);

export const getChatSession = (sessionId: number) =>
  apiClient.get<ChatSessionResponse>(`${CHAT_SESSIONS_BASE}/${sessionId}`);

export const updateQuickQuestions = (sessionId: number, payload: QuickQuestionRequest) =>
  apiClient.put<ChatSessionResponse>(`${CHAT_SESSIONS_BASE}/${sessionId}/quick-questions`, payload);

export const createGeneratedDocuments = (sessionId: number, payload: GeneratedDocumentRequest) =>
  apiClient.post<DocumentItem[]>(`${CHAT_SESSIONS_BASE}/${sessionId}/generated-documents`, payload);

// Document Analysis Reports
export const createAnalysisReport = (documentId: number) =>
  apiClient.post<AnalysisReportResponse>(`${DOCUMENTS_BASE}/${documentId}/analysis`);

export const getAnalysisReports = (documentId: number) =>
  apiClient.get<AnalysisReportResponse[]>(`${DOCUMENTS_BASE}/${documentId}/analysis`);

export const getAnalysisReport = (documentId: number, reportId: number) =>
  apiClient.get<AnalysisReportResponse>(`${DOCUMENTS_BASE}/${documentId}/analysis/${reportId}`);

// Document details
export const getDocument = (documentId: number) =>
  apiClient.get<DocumentItem>(`${DOCUMENTS_BASE}/${documentId}`);

