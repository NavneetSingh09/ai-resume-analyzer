import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/resumes',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const uploadResume = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const saveResumeText = (content) => {
  return api.post('', { content });
};

export const getAllResumes = () => {
  return api.get('');
};

export const searchResumes = (query) => {
  return api.post('/search', { query });
};

export const matchResumes = (jobDescription, limit = 5, topChunks = 20) => {
  return api.post('/match', { jobDescription, limit, topChunks });
};

export const analyzeResume = (resumeText, jobDescription) => {
  return api.post('/analyze', { resumeText, jobDescription });
};

export const analyzeRAG = (jobDescription, topChunks = 10) => {
  return api.post('/analyze-rag', { jobDescription, topChunks });
};

export const rankCandidates = (jobDescription, topCandidates = 5) => {
  return api.post('/jobs/rank', { jobDescription, topCandidates });
};

export default api;