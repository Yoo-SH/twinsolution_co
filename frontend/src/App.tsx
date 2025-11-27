import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import History from './pages/History';
import AIChat from './pages/AIChat';
import RAGManagement from './pages/RAGManagement';
import APIIntegration from './pages/APIIntegration';
import ChunkingSettings from './pages/ChunkingSettings';
import ProjectCreate from './pages/ProjectCreate';
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/documents" element={<AIChat />} />
        <Route path="/rag" element={<RAGManagement />} />
        <Route path="/chunking" element={<ChunkingSettings />} />
        <Route path="/api" element={<APIIntegration />} />
        <Route path="/history" element={<History />} />
        <Route path="/projects/new" element={<ProjectCreate />} />
      </Routes>
    </Router>
  );
}

export default App;

