import './DocumentTable.css';

interface Document {
  id: number;
  name: string;
  type: string;
  size: string;
  category: string;
  status: 'completed' | 'processing';
  chunks: string;
  uploadDate: string;
}

interface DocumentTableProps {
  documents: Document[];
  onView: (id: number) => void;
  onDownload: (id: number) => void;
  onDelete: (id: number) => void;
}

const DocumentTable = ({ documents, onView, onDownload, onDelete }: DocumentTableProps) => {
  const getCategoryColor = (category: string) => {
    switch (category) {
      case '법령':
        return 'category-law';
      case '가이드':
        return 'category-guide';
      case '양식':
        return 'category-form';
      default:
        return '';
    }
  };

  const getStatusColor = (status: string) => {
    return status === 'completed' ? 'status-completed' : 'status-processing';
  };

  const getStatusText = (status: string) => {
    return status === 'completed' ? '완료된 문서' : '처리 중';
  };

  return (
    <div className="document-table-container">
      <div className="document-table">
        <div className="table-header">
          <div className="header-cell">문서명</div>
          <div className="header-cell">타입</div>
          <div className="header-cell">크기</div>
          <div className="header-cell">카테고리</div>
          <div className="header-cell">상태</div>
          <div className="header-cell">청크</div>
          <div className="header-cell">업로드 일시</div>
          <div className="header-cell">작업</div>
        </div>

        {documents.map((doc) => (
          <div key={doc.id} className="table-row">
            <div className="table-cell doc-name">
              <span className="doc-icon">📄</span>
              {doc.name}
            </div>
            <div className="table-cell">{doc.type}</div>
            <div className="table-cell">{doc.size}</div>
            <div className="table-cell">
              <span className={`category-badge ${getCategoryColor(doc.category)}`}>
                {doc.category}
              </span>
            </div>
            <div className="table-cell">
              <span className={`status-badge ${getStatusColor(doc.status)}`}>
                {getStatusText(doc.status)}
              </span>
            </div>
            <div className="table-cell">{doc.chunks}</div>
            <div className="table-cell">{doc.uploadDate}</div>
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
        ))}
      </div>
    </div>
  );
};

export default DocumentTable;

