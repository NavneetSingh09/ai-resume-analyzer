import React, { useState } from 'react';
import { Search, FileSearch } from 'lucide-react';
import { searchResumes } from '../api';
import ScoreBadge from '../components/ScoreBadge';

export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const res = await searchResumes(query);
      setResults(res.data);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleSearch();
  };

  return (
    <div>
      <div className="page-header">
        <h2>Semantic Search</h2>
        <p>Search across all resume chunks using natural language</p>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 32 }}>
        <input
          className="form-input"
          placeholder="e.g. Java developer with cloud experience"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          style={{ flex: 1 }}
        />
        <button
          className="btn btn-primary"
          onClick={handleSearch}
          disabled={!query.trim() || loading}
        >
          <Search size={16} />
          Search
        </button>
      </div>

      {loading && (
        <div className="loading-spinner">
          <div className="spinner" />
          Searching vector database...
        </div>
      )}

      {results && results.length === 0 && (
        <div className="empty-state">
          <FileSearch />
          <h3>No results found</h3>
          <p>Try a different query or upload more resumes</p>
        </div>
      )}

      {results && results.length > 0 && (
        <div>
          {results.map((r, i) => (
            <div className="result-card" key={i}>
              <div className="result-header">
                <span className="result-rank">Result #{i + 1}</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span className="distance-label">dist: {r.distance.toFixed(4)}</span>
                  <ScoreBadge score={r.score} />
                </div>
              </div>
              <div className="result-text">{r.chunkText}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}