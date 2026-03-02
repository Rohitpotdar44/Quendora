import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import { authState, journalEntriesState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import CreateEntry from '../JournalEntry/CreateEntry';
import EntryCard from '../JournalEntry/EntryCard';
import FileBrowser from '../FileManager/FileBrowser';
import './Dashboard.css';

const Dashboard = () => {
  const [auth, setAuth] = useRecoilState(authState);
  const [entries, setEntries] = useRecoilState(journalEntriesState);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [activeTab, setActiveTab] = useState(() => {
    // Restore active tab from localStorage
    return localStorage.getItem('dashboard_active_tab') || 'entries';
  });
  const [loading, setLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOrder, setSortOrder] = useState('newest');
  
  const navigate = useNavigate();

  // Restore auth from localStorage and load entries on mount
  useEffect(() => {
    console.log('[Dashboard] Component mounted, restoring auth and loading entries...');
    
    // If auth is already in state, don't block the UI
    if (auth?.isAuthenticated && auth.user) {
      loadEntriesFromBackend(auth.uniqueKey);
      return;
    }

    // Check localStorage for auth data
    const authData = localStorage.getItem('auth_state');
    
    if (authData) {
      try {
        const parsedAuth = JSON.parse(authData);
        console.log('[Dashboard] Restored auth from localStorage:', {
          isAuthenticated: parsedAuth.isAuthenticated,
          username: parsedAuth.user?.username,
          hasUniqueKey: !!parsedAuth.uniqueKey
        });
        
        // Update Recoil state with localStorage data
        setAuth(parsedAuth);
        
        // Load entries after auth is restored (non-blocking)
        if (parsedAuth.isAuthenticated) {
          loadEntriesFromBackend(parsedAuth.uniqueKey);
        } else {
          console.log('[Dashboard] No valid auth, skipping entry load');
          setLoading(false);
      }
      } catch (error) {
        console.error('[Dashboard] Error parsing auth from localStorage:', error);
        setLoading(false);
      }
    } else {
      console.log('[Dashboard] No auth in localStorage, redirecting to login');
      setLoading(false);
      navigate('/login');
    }
  }, []); // Run only once on mount

  const loadEntriesFromBackend = async (uniqueKey) => {
    try {
      console.log('[Dashboard] Loading entries from backend...');
      setIsRefreshing(true);
          const response = await journalAPI.getAllEntries();
      console.log('[Dashboard] ✅ Loaded', response.data?.length || 0, 'entries from backend');
          setEntries(response.data || []);
        } catch (error) {
      console.error('[Dashboard] ❌ Failed to load entries:', error);
          setEntries([]);
    } finally {
      setLoading(false);
      setIsRefreshing(false);
      }
    };

  const loadEntries = async () => {
    const authData = localStorage.getItem('auth_state');
    if (!authData) {
      setLoading(false);
      return;
    }

    try {
      const parsedAuth = JSON.parse(authData);
      if (!parsedAuth.isAuthenticated) {
        setLoading(false);
        return;
      }

      await loadEntriesFromBackend(parsedAuth.uniqueKey);
    } catch (error) {
      console.error('[Dashboard] Error in loadEntries:', error);
      setLoading(false);
    }
  };

  const handleLogout = () => {
    console.log('[Dashboard] Logging out...');
    localStorage.clear();
    setAuth({
      isAuthenticated: false,
      user: null,
      uniqueKey: null,
      isLoading: false
    });
    setEntries([]);
    navigate('/login');
  };

  const handleCreateEntry = async () => {
    console.log('[Dashboard] ✅ Entry created successfully, refreshing list...');
    setShowCreateForm(false);
    
    // Immediately reload entries
      console.log('[Dashboard] Reloading entries from backend...');
      await loadEntries();
  };

  const handleDeleteEntry = async (entryId, uniqueKey) => {
    try {
      await journalAPI.deleteEntry(entryId, uniqueKey);
      console.log('[Dashboard] Entry deleted, refreshing...');
      await loadEntries();
    } catch (error) {
      console.error('[Dashboard] Failed to delete entry:', error);
      const errorMessage = error.response?.data || 'Failed to delete entry';
      alert(errorMessage);
      throw error; // Re-throw so EntryCard can handle it
    }
  };

  // Filter and sort entries
  const getFilteredAndSortedEntries = () => {
    let filtered = [...entries];

    // Filter by search query (searches in title and content - case insensitive)
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(entry => {
        const title = (entry.title || '').toLowerCase();
        const content = (entry.content || '').toLowerCase();
        return title.includes(query) || content.includes(query);
      });
    }

    // Sort entries
    filtered.sort((a, b) => {
      const dateA = new Date(a.localDateTime);
      const dateB = new Date(b.localDateTime);
      
      if (sortOrder === 'newest') {
        return dateB - dateA;
      } else if (sortOrder === 'oldest') {
        return dateA - dateB;
      } else if (sortOrder === 'title') {
        return (a.title || '').localeCompare(b.title || '');
      }
      return 0;
    });

    return filtered;
  };

  const filteredEntries = getFilteredAndSortedEntries();

  if (!auth.user) {
    return (
      <div className="dashboard-loading">
        <div className="loading-spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      {/* Header */}
      <header className="dashboard-header">
        <div className="header-content">
          <div className="header-left">
            <h1 className="dashboard-title">🔐 Quendora</h1>
            <p className="dashboard-subtitle">Your secure vault for documents, memories, and private thoughts</p>
            <p className="welcome-text">Welcome back, <strong>{auth.user.username}</strong> !</p>
          </div>
          <div className="header-right">
            <div className="user-info">
              <span className="user-email">{auth.user.email}</span>
                {auth.user.roles?.map(role => (
                  <span key={role} className={`role-badge ${role.toLowerCase()}`}>
                    {role}
                  </span>
                ))}
              </div>
              <button 
                onClick={handleLogout}
                className="btn btn-danger logout-btn"
              >
              Logout
              </button>
          </div>
        </div>
      </header>

      {/* Navigation Tabs */}
      <nav className="dashboard-nav">
        <div className="nav-content">
          <button 
            className={`nav-tab ${activeTab === 'entries' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('entries');
              localStorage.setItem('dashboard_active_tab', 'entries');
            }}
          >
            📝 My Entries
          </button>
          <button 
            className={`nav-tab ${activeTab === 'files' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('files');
              localStorage.setItem('dashboard_active_tab', 'files');
            }}
          >
            📁 My Files
          </button>
          <button 
            className={`nav-tab ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('profile');
              localStorage.setItem('dashboard_active_tab', 'profile');
            }}
          >
            👤 Profile
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <main className="dashboard-main">
        <div className="main-content">
          <div
            className={`entries-section section-panel ${
              activeTab === 'entries' ? 'active' : ''
            }`}
          >
              <div className="section-header">
                <h2 className="section-title">Journal Entries</h2>
              </div>

              {showCreateForm && (
                <CreateEntry 
                  onSave={handleCreateEntry}
                  onCancel={() => setShowCreateForm(false)}
                />
              )}

              {/* Search and Filter Controls */}
              {entries.length > 0 && !loading && (
                <div className="entries-controls">
                   <button 
                  onClick={() => setShowCreateForm(true)}
                  className="btn btn-primary create-btn"
                >
                  ✍️ New Entry
                </button>
                  {/* <div className="search-box">
                    <input
                      type="text"
                      placeholder="🔍 Search entries..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="search-input"
                    />
                    {searchQuery && (
                      <button 
                        onClick={() => setSearchQuery('')}
                        className="clear-search"
                        title="Clear search"
                      >
                        ✕
                      </button>
                    )}
                  </div> */}
                  <div className="sort-controls">
                    <label htmlFor="sort-select">Sort by:</label>
                    <select 
                      id="sort-select"
                      value={sortOrder}
                      onChange={(e) => setSortOrder(e.target.value)}
                      className="sort-select"
                    >
                      <option value="newest">Newest First</option>
                      <option value="oldest">Oldest First</option>
                      <option value="title">Title (A-Z)</option>
                    </select>
                  </div>
                </div>
              )}
              
              <div className="entries-list">
                {(loading || isRefreshing) && entries.length === 0 ? (
                  <div className="empty-state syncing-state">
                    <div className="empty-icon">📝</div>
                    <p className="syncing-indicator">
                      <strong>Syncing entries...</strong>
                    </p>
                  </div>
                ) : loading ? (
                  <div className="loading-state">
                    <div className="loading-spinner"></div>
                    <p>Loading your entries...</p>
                  </div>
                ) : entries.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-icon">📝</div>
                    <h3>No Entries Yet</h3>
                    <p>Start writing your first journal entry!</p>
                    <button 
                      onClick={() => setShowCreateForm(true)}
                      className="btn btn-primary"
                    >
                      Create Your First Entry
                    </button>
                  </div>
                ) : filteredEntries.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-icon">🔍</div>
                    <h3>No Matching Entries</h3>
                    <p>Try a different search term or clear the search.</p>
                    <button 
                      onClick={() => setSearchQuery('')}
                      className="btn btn-secondary"
                    >
                      Clear Search
                    </button>
                  </div>
                ) : (
                  filteredEntries.map(entry => (
                    <EntryCard 
                      key={entry.id}
                      entry={entry}
                      onDelete={handleDeleteEntry}
                      onEdit={loadEntries}
                    />
                  ))
                )}
              </div>
            </div>

          <div
            className={`files-section section-panel ${
              activeTab === 'files' ? 'active' : ''
            }`}
          >
            <FileBrowser />
          </div>

          <div
            className={`profile-section section-panel ${
              activeTab === 'profile' ? 'active' : ''
            }`}
          >
            <div className="card">
              <div className="card-header">
                <h2 className="card-title">👤 Profile Information</h2>
              </div>
              <div className="profile-info">
                <div className="info-row">
                  <label>Username:</label>
                  <span>{auth.user.username}</span>
                </div>
                <div className="info-row">
                  <label>Email:</label>
                  <span>{auth.user.email}</span>
                </div>
                <div className="info-row">
                  <label>Roles:</label>
                  <div className="roles-list">
                    {auth.user.roles?.map(role => (
                      <span key={role} className={`role-badge ${role.toLowerCase()}`}>
                        {role}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="info-row">
                  <label>Total Entries:</label>
                  <span>{entries.length}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;

