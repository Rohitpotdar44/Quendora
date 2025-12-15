import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import { authState, journalEntriesState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import CreateEntry from '../JournalEntry/CreateEntry';
import EntryCard from '../JournalEntry/EntryCard';
import './Dashboard.css';

const Dashboard = () => {
  const [auth, setAuth] = useRecoilState(authState);
  const [entries, setEntries] = useRecoilState(journalEntriesState);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [activeTab, setActiveTab] = useState('entries');
  const [loading, setLoading] = useState(true);
  
  const navigate = useNavigate();

  // Restore auth from localStorage and load entries on mount
  useEffect(() => {
    console.log('[Dashboard] Component mounted, restoring auth and loading entries...');
    
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
        
        // Load entries after auth is restored
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
          const response = await journalAPI.getAllEntries();
      console.log('[Dashboard] ✅ Loaded', response.data?.length || 0, 'entries from backend');
          setEntries(response.data || []);
        } catch (error) {
      console.error('[Dashboard] ❌ Failed to load entries:', error);
          setEntries([]);
    } finally {
      setLoading(false);
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
    
    // Wait for backend to persist, then reload
    setTimeout(async () => {
      console.log('[Dashboard] Reloading entries from backend...');
      await loadEntries();
    }, 1000); // Increased timeout to ensure backend persistence
  };

  const handleDeleteEntry = async (entryId) => {
    try {
      await journalAPI.deleteEntry(entryId);
      console.log('[Dashboard] Entry deleted, refreshing...');
      await loadEntries();
    } catch (error) {
      console.error('[Dashboard] Failed to delete entry:', error);
      alert('Failed to delete entry');
    }
  };

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
            <h1 className="dashboard-title">📖 My Secure Journal</h1>
            <p className="welcome-text">Welcome back, <strong>{auth.user.username}</strong>!</p>
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
            onClick={() => setActiveTab('entries')}
          >
            📝 My Entries
          </button>
          <button 
            className={`nav-tab ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => setActiveTab('profile')}
          >
            👤 Profile
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <main className="dashboard-main">
        <div className="main-content">
          {activeTab === 'entries' && (
            <div className="entries-section">
              <div className="section-header">
                <h2 className="section-title">Journal Entries</h2>
                <button 
                  onClick={() => setShowCreateForm(true)}
                  className="btn btn-primary create-btn"
                >
                  ✍️ New Entry
                </button>
              </div>
              
              {showCreateForm && (
                <CreateEntry 
                  onSave={handleCreateEntry}
                  onCancel={() => setShowCreateForm(false)}
                />
              )}
              
              <div className="entries-list">
                {loading ? (
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
                ) : (
                  entries.map(entry => (
                    <EntryCard 
                      key={entry.id}
                      entry={entry}
                      onDelete={handleDeleteEntry}
                    />
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === 'profile' && (
            <div className="profile-section">
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
          )}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;

