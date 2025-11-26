export interface DashboardSummary {
  totalDocuments: number;
  documentsDelta: number;
  inProgressProjects: number;
  completedProjects: number;
  chatSessionsCount: number;
  chatSessionsDelta: number;
}

export type ProjectStatus = '초기단계' | '승인대기' | '서류검토' | '설계진행' | string;

export interface Project {
  id: number;
  name: string;
  description: string | null;
  location: string;
  zoning: string;
  usage: string;
  totalFloorArea: number | null;
  floors: string;
  parkingSpaces: number | null;
  status: ProjectStatus;
  progress: number | null;
  createdAt: string;
  updatedAt: string;
  latestChatSessionAt: string | null;
}

export interface ProjectRequest {
  name: string;
  description?: string | null;
  location: string;
  zoning: string;
  usage: string;
  totalFloorArea: number;
  floors: string;
  parkingSpaces: number;
  status?: ProjectStatus;
}

export type ProjectUpdateRequest = Partial<ProjectRequest>;

export type DocumentStatus =
  | '업로드됨'
  | '분석중'
  | '분석완료'
  | '분할완료'
  | '삭제됨'
  | string;

export interface DocumentItem {
  id: number;
  projectId: number;
  name: string;
  fileType: string;
  filePath: string;
  origin: string;
  status: DocumentStatus;
  chunkSize: number | null;
  chunkOverlap: number | null;
  chunkCount: number;
  hasAnalysisReport: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentUploadResponse extends DocumentItem {}

export interface DocumentChunk {
  id: number;
  documentId: number;
  index: number;
  content: string;
  createdAt: string;
}

export interface ChunkSettings {
  id: number;
  chunkSize: number;
  chunkOverlap: number;
  updatedAt: string;
}

export interface ChunkSettingsRequest {
  chunkSize?: number;
  chunkOverlap?: number;
}

export interface ChunkPreviewRequest {
  text: string;
  chunkSize?: number;
  chunkOverlap?: number;
  separators?: string[];
}

export interface ChunkPreview {
  index: number;
  content: string;
  length: number;
}

export interface ChunkPreviewResponse {
  chunks: ChunkPreview[];
  totalChunks: number;
  averageLength: number;
  overlapRatio: number;
}

export interface ChatSession {
  id: number;
  projectId: number;
  quickQuestion: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  role: 'user' | 'assistant' | string;
  content: string;
  createdAt: string;
}

export interface ChatMessagePayload {
  content: string;
  systemPrompt?: string;
  model?: string;
  temperature?: number;
  maxTokens?: number;
  stream?: boolean;
}

export interface ApiIntegrationItem {
  id: number;
  name: string;
  baseUrl: string;
  method: string;
  authKey: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiIntegrationPayload {
  name?: string;
  baseUrl?: string;
  method?: string;
  authKey?: string;
  status?: string;
}

export interface ApiStatusUpdatePayload {
  status: string;
}

export interface ApiTestResponse {
  status: string;
  statusCode: number;
  message: string;
  responseBody: string | null;
}

export interface ApiLogItem {
  id: number;
  apiId: number;
  status: string;
  statusCode: number;
  message: string;
  createdAt: string;
}

