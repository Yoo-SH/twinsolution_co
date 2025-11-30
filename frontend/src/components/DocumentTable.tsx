import type { DocumentItem } from '../types/api';
import './DocumentTable.css';

interface DocumentTableProps {
  documents: DocumentItem[];
  onView: (id: number) => void;
  onDownload: (id: number) => void;
  onDelete: (id: number) => void;
  loading?: boolean;
  emptyMessage?: string;
}

const formatDateTime = (value?: string | null) => {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return value;
  }
};

const formatOrigin = (origin: string) => {
  if (origin === 'uploaded') return '업로드';
  if (origin === 'generated') return '생성';
  return origin;
};

const getStatusMeta = (status: string) => {
  if (status === '분석완료' || status === '분할완료') {
    return { className: 'status-completed', text: status === '분석완료' ? '완료된 문서' : '분할완료' };
  }
  if (status === '분석중' || status === '업로드됨') {
    return { className: 'status-processing', text: '처리 중' };
  }
  return { className: 'status-processing', text: status || '처리 중' };
};

const DocumentTable = ({
  documents,
  onView,
  onDownload,
  onDelete,
  loading = false,
  emptyMessage = '등록된 문서가 없습니다.',
}: DocumentTableProps) => {
  if (loading) {
    return (
      <div className="document-table-container">
        <div className="document-table loading">문서를 불러오는 중입니다...</div>
      </div>
    );
  }

  if (!documents.length) {
    return (
      <div className="document-table-container">
        <div className="document-table empty-state">{emptyMessage}</div>
      </div>
    );
  }

  return (
    <div className="document-table-container">
      <div className="document-table">
        <div className="table-header">
          <div className="header-cell">문서명</div>
          <div className="header-cell">타입</div>
          <div className="header-cell">크기</div>
          <div className="header-cell">출처</div>
          <div className="header-cell">상태</div>
          <div className="header-cell">청크</div>
          <div className="header-cell">업로드 일시</div>
          <div className="header-cell">작업</div>
        </div>

        {documents.map((doc) => {
          const statusMeta = getStatusMeta(doc.status);

          return (
            <div key={doc.id} className="table-row">
              <div className="table-cell doc-name">
                <span className="doc-icon">📄</span>
                {doc.name}
              </div>
              <div className="table-cell">{doc.fileType}</div>
              <div className="table-cell">-</div>
              <div className="table-cell">
                <span className="category-badge">{formatOrigin(doc.origin)}</span>
              </div>
              <div className="table-cell">
                <span className={`status-badge ${statusMeta.className}`}>
                  {statusMeta.text}
                </span>
              </div>
              <div className="table-cell">
                {doc.chunkCount > 0 ? `${doc.chunkCount.toLocaleString()}개` : '-'}
              </div>
              <div className="table-cell">{formatDateTime(doc.createdAt)}</div>
              <div className="table-cell actions">
                <button
                  className="action-icon"
                  onClick={() => onView(doc.id)}
                  title="미리보기"
                >
                  👁️
                </button>
                <button
                  className="action-icon"
                  onClick={() => onDownload(doc.id)}
                  title="다운로드"
                >
                  ⬇️
                </button>
                <button
                  className="action-icon delete"
                  onClick={() => onDelete(doc.id)}
                  title="삭제"
                >
                  🗑️
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default DocumentTable;

