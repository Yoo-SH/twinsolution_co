import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import StatCard from '../components/StatCard';
import FileUpload from '../components/FileUpload';
import DocumentTable from '../components/DocumentTable';
import SearchBar from '../components/SearchBar';
import FilterDropdown from '../components/FilterDropdown';
import './RAGManagement.css';

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

const RAGManagement = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterValue, setFilterValue] = useState('전체 카테고리');

  const stats = [
    {
      title: '전체문서',
      value: 4,
      trend: '',
      icon: '📄',
      iconColor: 'rgba(200, 200, 200, 0.3)',
    },
    {
      title: '완료된 문서',
      value: 3,
      trend: '',
      icon: '✅',
      iconColor: 'rgba(144, 238, 144, 0.3)',
    },
    {
      title: '처리 중',
      value: 1,
      trend: '',
      icon: '⏳',
      iconColor: 'rgba(255, 255, 224, 0.5)',
    },
    {
      title: '총 청크',
      value: 235,
      trend: '',
      icon: '📊',
      iconColor: 'rgba(173, 216, 230, 0.4)',
    },
  ];

  const documents: Document[] = [
    {
      id: 1,
      name: '건축법_전문.pdf',
      type: 'PDF',
      size: '2.4 MB',
      category: '법령',
      status: 'completed',
      chunks: '145개',
      uploadDate: '2024-11-10 14:30',
    },
    {
      id: 2,
      name: '준주거지역_건축가이드.hwp',
      type: 'HWP',
      size: '2.4 MB',
      category: '가이드',
      status: 'completed',
      chunks: '78개',
      uploadDate: '2024-11-09 11:20',
    },
    {
      id: 3,
      name: '건축허가신청서_양식.pdf',
      type: 'PDF',
      size: '2.4 MB',
      category: '양식',
      status: 'completed',
      chunks: '12개',
      uploadDate: '2024-11-08 16:45',
    },
    {
      id: 4,
      name: '에너지절약설계기준.pdf',
      type: 'PDF',
      size: '2.4 MB',
      category: '법령',
      status: 'processing',
      chunks: '-',
      uploadDate: '2024-11-07 09:15',
    },
  ];

  const filterOptions = ['전체 카테고리', '법령', '가이드', '양식'];

  const handleFileSelect = (file: File) => {
    console.log('Selected file:', file.name);
    // TODO: 파일 업로드 로직
  };

  const handleView = (id: number) => {
    console.log('View document:', id);
  };

  const handleDownload = (id: number) => {
    console.log('Download document:', id);
  };

  const handleDelete = (id: number) => {
    console.log('Delete document:', id);
  };

  return (
    <div className="rag-management-layout">
      <Sidebar />
      <main className="rag-management-main">
        <Header title="문서 관리" />
        <div className="rag-management-content">
          {/* Statistics Section */}
          <section className="rag-stats-section">
            {stats.map((stat, index) => (
              <StatCard
                key={index}
                title={stat.title}
                value={stat.value}
                trend={stat.trend}
                icon={stat.icon}
                iconColor={stat.iconColor}
              />
            ))}
          </section>

          {/* File Upload Section */}
          <FileUpload onFileSelect={handleFileSelect} />

          {/* Search and Filter Section */}
          <section className="rag-search-filter-section">
            <SearchBar
              value={searchQuery}
              onChange={setSearchQuery}
              placeholder="문서 검색"
            />
            <FilterDropdown
              value={filterValue}
              options={filterOptions}
              onChange={setFilterValue}
            />
          </section>

          {/* Documents Table */}
          <DocumentTable
            documents={documents}
            onView={handleView}
            onDownload={handleDownload}
            onDelete={handleDelete}
          />
        </div>
      </main>
    </div>
  );
};

export default RAGManagement;

