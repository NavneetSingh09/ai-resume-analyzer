import React, { useState } from 'react';
import { GitCompare, FileSearch } from 'lucide-react';
import { matchResumes } from '../api';
import ScoreBadge from '../components/ScoreBadge';

export default function MatchPage() {
  const [jobDesc, setJobDesc] = useState('');
  const [limit, setLimit] = useState(5);
  const [topChunks, setTopChunks] = useState(20);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleMatch = async () => {
    if (!jobDesc.trim()) return;
    setLoading(true);
    try {
      const res = await matchResumes(jobDesc, limit, topChunks);
      setResults(res.data);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  return (
    <div>
      <div className="page-header">
        <h2>Job Match</h2>
        <p>Find the best matching resumes for a job description</p>
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

        <div style={{ display: 'flex', gap: 16, marginBottom: 20 }}>
          <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
            <label className="form-label">Max Results</label>
            <input
              className="form-input"
              type="number"
              min={1}
              max={20}
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value))}
            />
          </div>
          <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
            <label className="form-label">Chunk Pool Size</label>
            <input
              className="form-input"
              type="number"
              min={5}
              max={100}
              value={topChunks}
              onChange={(e) => setTopChunks(Number(e.target.value))}
            />
          </div>
        </div>

        <button
          className="btn btn-primary"
          onClick={handleMatch}
          disabled={!jobDesc.trim() || loading}
        >
          <GitCompare size={16} />
          Find Matches
        </button>
      </div>

      {loading && (
        <div className="loading-spinner">
          <div className="spinner" />
          Matching resumes to job description...
        </div>
      )}

      {results && results.length === 0 && (
        <div className="empty-state">
          <FileSearch />
          <h3>No matches found</h3>
          <p>Try a broader job description or upload more resumes</p>
        </div>
      )}

      {results && results.length > 0 && (
        <div>
          {results.map((r, i) => (
            <div className="result-card" key={i}>
              <div className="result-header">
                <div>
                  <span className="result-rank">Match #{i + 1}</span>
                  <span className="resume-id" style={{ marginLeft: 12 }}>
                    {r.resumeId}
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span className="distance-label">dist: {r.distance}</span>
                  <ScoreBadge score={r.score} />
                </div>
              </div>
              <div className="result-text">{r.bestMatchingChunk}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}