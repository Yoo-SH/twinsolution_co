import { useState } from 'react';
import './FileUpload.css';

interface FileUploadProps {
  onFileSelect: (file: File) => void;
}

const FileUpload = ({ onFileSelect }: FileUploadProps) => {
  const [isDragging, setIsDragging] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      onFileSelect(files[0]);
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      onFileSelect(files[0]);
    }
  };

  return (
    <div
      className={`file-upload-area ${isDragging ? 'dragging' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="upload-icon">⬆️</div>
      <p className="upload-text">문서를 드래그하여 업로드하거나 클릭하세요</p>
      <p className="upload-info">PDF, HWP, DOCX, TXT 파일을 지원합니다 (최대 50MB)</p>
      <label className="file-select-btn">
        파일 선택
        <input
          type="file"
          accept=".pdf,.hwp,.docx,.txt"
          onChange={handleFileInput}
          style={{ display: 'none' }}
        />
      </label>
    </div>
  );
};

export default FileUpload;

