import React, { useState } from 'react';
import {
  Upload, Search, GitCompare, Brain, Trophy, Menu, X,
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
  const [menuOpen, setMenuOpen] = useState(false);
  const ActiveComponent = PAGES[activePage];

  const handleNavClick = (id) => {
    setActivePage(id);
    setMenuOpen(false);
  };

  return (
    <div className="app-layout">
      <button className="mobile-menu-btn" onClick={() => setMenuOpen(!menuOpen)}>
        {menuOpen ? <X /> : <Menu />}
      </button>

      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-logo">
          <h1>Resume<span>AI</span></h1>
          <p>Smart Analyzer</p>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              className={`nav-link ${activePage === id ? 'active' : ''}`}
              onClick={() => handleNavClick(id)}
            >
              <Icon />
              {label}
            </button>
          ))}
        </nav>
      </aside>

      {menuOpen && <div className="mobile-overlay" onClick={() => setMenuOpen(false)} />}

      <main className="main-content">
        <ActiveComponent />
      </main>
    </div>
  );
}