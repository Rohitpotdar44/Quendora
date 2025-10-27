import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import { authState, journalEntriesState } from '../../state/atoms';
import { clearAuthData, isAdmin } from '../../utils/auth';
import { journalAPI } from '../../services/api';
import JournalEntryList from './JournalEntryList';
import CreateEntry from '../JournalEntry/CreateEntry';
import './Dashboard.css';

const Dashboard = () => {
  const [auth, setAuth] = useRecoilState(authState);
  const [entries, setEntries] = useRecoilState(journalEntriesState);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [activeTab, setActiveTab] = useState('entries');
  
  const navigate = useNavigate();

  useEffect(() => {
    // Load user data from localStorage if not in state
    if (!auth.isAuthenticated) {
      const userData = localStorage.getItem('user');
      const uniqueKey = localStorage.getItem('uniqueKey');
      
      if (userData && uniqueKey) {
        setAuth({
          isAuthenticated: true,
          user: JSON.parse(userData),
          uniqueKey,
          isLoading: false
        });
      }
    }
  }, [auth.isAuthenticated, setAuth]);

  // Load journal entries from backend
  useEffect(() => {
    const loadEntries = async () => {
      if (auth.isAuthenticated && auth.uniqueKey) {
        try {
          const response = await journalAPI.getAllEntries();
          setEntries(response.data || []);
        } catch (error) {
          console.error('Failed to load journal entries:', error);
          setEntries([]);
        }
      }
    };

    loadEntries();
  }, [auth.isAuthenticated, auth.uniqueKey, setEntries]);

  const handleLogout = () => {
    clearAuthData();
    setAuth({
      isAuthenticated: false,
      user: null,
      uniqueKey: null,
      isLoading: false
    });
    navigate('/login');
  };

  const handleCreateEntry = async (newEntry) => {
    // Add the new entry to the list immediately for better UX
    setEntries(prev => [newEntry, ...prev]);
    setShowCreateForm(false);
    
    // Refresh entries from backend to ensure consistency
    try {
      const response = await journalAPI.getAllEntries();
      setEntries(response.data || []);
    } catch (error) {
      console.error('Failed to refresh entries:', error);
    }
  };


  if (!auth.user) {
    return (
      <div className="dashboard-loading">
        <div className="loading-spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      {/* Header */}
      <header className="dashboard-header">
        <div className="header-content">
          <div className="header-left">
            <h1 className="dashboard-title">📖 My Journal</h1>
            <p className="welcome-text">Welcome back, {auth.user.username}!</p>
          </div>
          <div className="header-right">
            <div className="user-info">
              <span className="user-email">{auth.user.email}</span>
              <div className="user-roles">
                {auth.user.roles?.map(role => (
                  <span key={role} className={`role-badge ${role.toLowerCase()}`}>
                    {role}
                  </span>
                ))}
              </div>
            </div>
            <div className="header-actions">
              {isAdmin() && (
                <button 
                  onClick={() => navigate('/admin')}
                  className="btn btn-secondary admin-btn"
                >
                  👑 Admin Panel
                </button>
              )}
              <button 
                onClick={handleLogout}
                className="btn btn-danger logout-btn"
              >
                🚪 Logout
              </button>
            </div>
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
              
              <JournalEntryList entries={entries} />
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
