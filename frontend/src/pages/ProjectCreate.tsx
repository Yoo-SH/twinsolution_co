import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import { createProject } from '../services/api';
import type { Project, ProjectRequest, ProjectStatus } from '../types/api';
import './ProjectCreate.css';

const statusOptions: ProjectStatus[] = ['초기단계', '승인대기', '서류검토', '설계진행'];

const defaultForm: ProjectRequest = {
  name: '',
  description: '',
  location: '',
  zoning: '',
  usage: '',
  totalFloorArea: 0,
  floors: '',
  parkingSpaces: 0,
  status: '초기단계',
};

const ProjectCreate = () => {
  const [form, setForm] = useState<ProjectRequest>(defaultForm);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;

    if (name === 'totalFloorArea') {
      setForm((prev) => ({ ...prev, totalFloorArea: Number(value) }));
      return;
    }

    if (name === 'parkingSpaces') {
      setForm((prev) => ({ ...prev, parkingSpaces: Number(value) }));
      return;
    }

    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const createdProject = await createProject(form);
      window.dispatchEvent(
        new CustomEvent<Project>('project-created', { detail: createdProject }),
      );
      setSuccessMessage('프로젝트가 성공적으로 생성되었습니다.');
      setForm(defaultForm);
    } catch (err) {
      const message = err instanceof Error ? err.message : '프로젝트를 생성하지 못했습니다.';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="project-create-layout">
      <Sidebar />
      <main className="project-create-main">
        <Header title="프로젝트 생성" />
        <div className="project-create-content">
          {error && <div className="project-create-alert error">{error}</div>}
          {successMessage && (
            <div className="project-create-alert success">
              <span>{successMessage}</span>
              <button type="button" onClick={() => navigate('/documents')}>
                서류작성 AI로 이동
              </button>
            </div>
          )}

          <form className="project-create-form" onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="name">프로젝트 이름 *</label>
              <input
                id="name"
                name="name"
                value={form.name}
                onChange={handleChange}
                required
                placeholder="예: 서울시 강남구 오피스텔 신축 공사"
              />
            </div>

            <div className="form-group">
              <label htmlFor="description">프로젝트 설명</label>
              <textarea
                id="description"
                name="description"
                rows={3}
                value={form.description ?? ''}
                onChange={handleChange}
                placeholder="프로젝트에 대한 간략한 설명을 입력하세요."
              />
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="location">위치 *</label>
                <input
                  id="location"
                  name="location"
                  value={form.location}
                  onChange={handleChange}
                  required
                  placeholder="예: 서울특별시 강남구 역삼동 123-45"
                />
              </div>
              <div className="form-group">
                <label htmlFor="zoning">지역/지구 *</label>
                <input
                  id="zoning"
                  name="zoning"
                  value={form.zoning}
                  onChange={handleChange}
                  required
                  placeholder="예: 준주거지역"
                />
              </div>
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="usage">용도 *</label>
                <input
                  id="usage"
                  name="usage"
                  value={form.usage}
                  onChange={handleChange}
                  required
                  placeholder="예: 오피스텔"
                />
              </div>
              <div className="form-group">
                <label htmlFor="floors">층수 *</label>
                <input
                  id="floors"
                  name="floors"
                  value={form.floors}
                  onChange={handleChange}
                  required
                  placeholder="예: 지하 2층, 지상 15층"
                />
              </div>
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="totalFloorArea">연면적 (㎡) *</label>
                <input
                  id="totalFloorArea"
                  name="totalFloorArea"
                  type="number"
                  min={0}
                  step="0.1"
                  value={form.totalFloorArea}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="parkingSpaces">주차 대수 *</label>
                <input
                  id="parkingSpaces"
                  name="parkingSpaces"
                  type="number"
                  min={0}
                  value={form.parkingSpaces}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="status">프로젝트 상태</label>
              <select
                id="status"
                name="status"
                value={form.status}
                onChange={handleChange}
              >
                {statusOptions.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-actions">
              <button type="button" className="secondary" onClick={() => navigate('/')}>
                취소
              </button>
              <button type="submit" className="primary" disabled={submitting}>
                {submitting ? '생성 중...' : '프로젝트 생성'}
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
};

export default ProjectCreate;

