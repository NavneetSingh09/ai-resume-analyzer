import React, { useState } from 'react';
import {
  Upload, Search, GitCompare, Brain, Trophy,
} from 'lucide-react';
import UploadPage from './pages/UploadPage';
import SearchPage from './pages/SearchPage';
import MatchPage from './pages/MatchPage';
import AnalyzePage from './pages/AnalyzePage';
import RankPage from './pages/RankPage';
import './index.css';

const NAV_ITEMS = [
  { id: 'upload', label: 'Upload Resume', icon: Upload },
  { id: 'search', label: 'Semantic Search', icon: Search },
  { id: 'match', label: 'Job Match', icon: GitCompare },
  { id: 'analyze', label: 'RAG Analysis', icon: Brain },
  { id: 'rank', label: 'Rank Candidates', icon: Trophy },
];

const PAGES = {
  upload: UploadPage,
  search: SearchPage,
  match: MatchPage,
  analyze: AnalyzePage,
  rank: RankPage,
};

export default function App() {
  const [activePage, setActivePage] = useState('upload');
  const ActiveComponent = PAGES[activePage];

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h1>Resume<span>AI</span></h1>
          <p>Smart Analyzer</p>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              className={`nav-link ${activePage === id ? 'active' : ''}`}
              onClick={() => setActivePage(id)}
            >
              <Icon />
              {label}
            </button>
          ))}
        </nav>
      </aside>
      <main className="main-content">
        <ActiveComponent />
      </main>
    </div>
  );
}