import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminAPI } from '../../services/api';
import './AdminPanel.css';

const AdminPanel = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllUsers();
      setUsers(response.data);
      setError('');
    } catch (err) {
      setError('Failed to fetch users. Please try again.');
      console.error('Error fetching users:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="admin-panel">
      {/* Header */}
      <header className="admin-header">
        <div className="header-content">
          <div className="header-left">
            <h1 className="admin-title">👑 Admin Panel</h1>
            <p className="admin-subtitle">Manage users and system settings</p>
          </div>
          <div className="header-right">
            <button 
              onClick={() => navigate('/dashboard')}
              className="btn btn-secondary"
            >
              ← Back to Dashboard
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="admin-main">
        <div className="main-content">
          {/* Users Section */}
          <section className="admin-section">
            <div className="section-header">
              <h2 className="section-title">👥 User Management</h2>
              <button 
                onClick={fetchUsers}
                className="btn btn-secondary refresh-btn"
                disabled={loading}
              >
                {loading ? '🔄 Loading...' : '🔄 Refresh'}
              </button>
            </div>

            {error && (
              <div className="alert alert-error">
                {error}
              </div>
            )}

            {loading ? (
              <div className="loading-container">
                <div className="loading-spinner"></div>
                <p>Loading users...</p>
              </div>
            ) : (
              <div className="users-container">
                {users.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-icon">👥</div>
                    <h3 className="empty-title">No Users Found</h3>
                    <p className="empty-description">
                      No users are currently registered in the system.
                    </p>
                  </div>
                ) : (
                  <div className="users-grid">
                    {users.map((user, index) => (
                      <div key={user.id || index} className="user-card">
                        <div className="user-header">
                          <div className="user-avatar">
                            {user.userName?.charAt(0)?.toUpperCase() || '?'}
                          </div>
                          <div className="user-info">
                            <h3 className="user-name">{user.userName}</h3>
                            <p className="user-email">{user.email || 'No email'}</p>
                          </div>
                        </div>
                        
                        <div className="user-details">
                          <div className="detail-row">
                            <span className="detail-label">Roles:</span>
                            <div className="roles-list">
                              {user.roles?.map(role => (
                                <span key={role} className={`role-badge ${role.toLowerCase()}`}>
                                  {role}
                                </span>
                              )) || <span className="no-roles">No roles</span>}
                            </div>
                          </div>
                          
                          <div className="detail-row">
                            <span className="detail-label">Unique Key:</span>
                            <code className="unique-key">
                              {user.uniqueKey ? 
                                `${user.uniqueKey.substring(0, 8)}...` : 
                                'Not generated'
                              }
                            </code>
                          </div>
                          
                          <div className="detail-row">
                            <span className="detail-label">Journal Entries:</span>
                            <span className="entry-count">
                              {user.allEntries?.length || 0} entries
                            </span>
                          </div>
                        </div>
                        
                        <div className="user-actions">
                          <button 
                            className="btn btn-secondary btn-sm"
                            onClick={() => console.log('View user details:', user)}
                          >
                            View Details
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </section>

          {/* System Stats */}
          <section className="admin-section">
            <div className="section-header">
              <h2 className="section-title">📊 System Statistics</h2>
            </div>
            
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-icon">👥</div>
                <div className="stat-info">
                  <h3 className="stat-number">{users.length}</h3>
                  <p className="stat-label">Total Users</p>
                </div>
              </div>
              
              <div className="stat-card">
                <div className="stat-icon">👑</div>
                <div className="stat-info">
                  <h3 className="stat-number">
                    {users.filter(user => user.roles?.includes('ADMIN')).length}
                  </h3>
                  <p className="stat-label">Admin Users</p>
                </div>
              </div>
              
              <div className="stat-card">
                <div className="stat-icon">📝</div>
                <div className="stat-info">
                  <h3 className="stat-number">
                    {users.reduce((total, user) => total + (user.allEntries?.length || 0), 0)}
                  </h3>
                  <p className="stat-label">Total Entries</p>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
};

export default AdminPanel;
