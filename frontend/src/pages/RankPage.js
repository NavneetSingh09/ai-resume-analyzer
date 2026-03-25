import React, { useState } from 'react';
import { Trophy, FileSearch } from 'lucide-react';
import { rankCandidates } from '../api';
import ScoreBadge from '../components/ScoreBadge';

export default function RankPage() {
  const [jobDesc, setJobDesc] = useState('');
  const [topCandidates, setTopCandidates] = useState(5);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleRank = async () => {
    if (!jobDesc.trim()) return;
    setLoading(true);
    try {
      const res = await rankCandidates(jobDesc, topCandidates);
      setResults(res.data);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  return (
    <div>
      <div className="page-header">
        <h2>Rank Candidates</h2>
        <p>Rank all candidates by relevance to a job description</p>
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
          <label className="form-label">Top Candidates</label>
          <input
            className="form-input"
            type="number"
            min={1}
            max={20}
            value={topCandidates}
            onChange={(e) => setTopCandidates(Number(e.target.value))}
          />
        </div>

        <button
          className="btn btn-primary"
          onClick={handleRank}
          disabled={!jobDesc.trim() || loading}
        >
          <Trophy size={16} />
          Rank Candidates
        </button>
      </div>

      {loading && (
        <div className="loading-spinner">
          <div className="spinner" />
          Ranking candidates...
        </div>
      )}

      {results && results.length === 0 && (
        <div className="empty-state">
          <FileSearch />
          <h3>No candidates found</h3>
          <p>Upload more resumes to see rankings</p>
        </div>
      )}

      {results && results.length > 0 && (
        <div>
          {results.map((r, i) => (
            <div className="result-card" key={i}>
              <div className="result-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 14,
                      fontWeight: 700,
                      background: i === 0
                        ? 'rgba(251, 191, 36, 0.15)'
                        : i === 1
                        ? 'rgba(148, 163, 184, 0.15)'
                        : i === 2
                        ? 'rgba(180, 130, 80, 0.15)'
                        : 'rgba(107, 107, 123, 0.1)',
                      color: i === 0
                        ? '#fbbf24'
                        : i === 1
                        ? '#94a3b8'
                        : i === 2
                        ? '#b4824f'
                        : 'var(--text-muted)',
                    }}
                  >
                    {i + 1}
                  </span>
                  <span className="resume-id">{r.resumeId}</span>
                </div>
                <ScoreBadge score={r.score} />
              </div>
              <div className="result-text">{r.bestMatchingChunk}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}