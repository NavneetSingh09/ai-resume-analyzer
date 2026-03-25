import React, { useState } from 'react';
import { Brain } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { analyzeRAG } from '../api';

export default function AnalyzePage() {
  const [jobDesc, setJobDesc] = useState('');
  const [topChunks, setTopChunks] = useState(10);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleAnalyze = async () => {
    if (!jobDesc.trim()) return;
    setLoading(true);
    setResult(null);
    setError(null);
    try {
      const res = await analyzeRAG(jobDesc, topChunks);
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Analysis failed. Please try again.');
    }
    setLoading(false);
  };

  return (
    <div>
      <div className="page-header">
        <h2>RAG Analysis</h2>
        <p>Get an AI-powered evaluation of how resumes match a job description</p>
      </div>

      <div className="card">
        <div className="form-group">
          <label className="form-label">Job Description</label>
          <textarea
            className="form-textarea"
            rows={5}
            placeholder="Paste the job description here..."
            value={jobDesc}
            onChange={(e) => setJobDesc(e.target.value)}
          />
        </div>

        <div className="form-group" style={{ maxWidth: 200 }}>
          <label className="form-label">Top Chunks</label>
          <input
            className="form-input"
            type="number"
            min={1}
            max={50}
            value={topChunks}
            onChange={(e) => setTopChunks(Number(e.target.value))}
          />
        </div>

        <button
          className="btn btn-primary"
          onClick={handleAnalyze}
          disabled={!jobDesc.trim() || loading}
        >
          <Brain size={16} />
          Analyze with AI
        </button>
      </div>

      {loading && (
        <div className="loading-spinner">
          <div className="spinner" />
          AI is analyzing resumes against the job description...
        </div>
      )}

      {error && (
        <div className="error-msg">
          {error}
        </div>
      )}

      {result && (
        <div className="rag-response">
          <ReactMarkdown>{result}</ReactMarkdown>
        </div>
      )}
    </div>
  );
}