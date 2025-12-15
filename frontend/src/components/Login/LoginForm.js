import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import { authAPI } from '../../services/api';
import { authState, showUniqueKeyModal, uniqueKeyMessage } from '../../state/atoms';
import UniqueKeyModal from './UniqueKeyModal';
import './LoginForm.css';

const LoginForm = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    userName: '',
    password: '',
    email: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const [auth, setAuth] = useRecoilState(authState);
  const [showModal, setShowModal] = useRecoilState(showUniqueKeyModal);
  const [keyMessage, setKeyMessage] = useRecoilState(uniqueKeyMessage);
  
  const navigate = useNavigate();

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      if (isLogin) {
        // Login - Check for saved unique key in localStorage
        const savedAuthData = localStorage.getItem('auth_state');
        let existingUniqueKey = null;
        
        if (savedAuthData) {
          try {
            const parsed = JSON.parse(savedAuthData);
            // Use saved key if it exists and matches this user
            if (parsed.user && parsed.user.username === formData.userName) {
              existingUniqueKey = parsed.uniqueKey;
            }
          } catch (e) {
            console.error('Error parsing saved auth:', e);
          }
        }
        
        // Send login request with uniqueKey (if available)
        const loginPayload = {
          userName: formData.userName,
          password: formData.password
        };
        
        // Include uniqueKey if we have it from localStorage
        if (existingUniqueKey) {
          loginPayload.uniqueKey = existingUniqueKey;
        }
        
        const response = await authAPI.login(loginPayload);

        const { username, email, roles, uniqueKey, uniqueKeyMissing, showUniqueKey, message } = response.data;
        const finalUniqueKey = uniqueKey || existingUniqueKey || null;
        
        // Login always succeeds - uniqueKey is optional (only needed for decryption)
        const authData = {
          isAuthenticated: true,
          user: { username, email, roles },
          uniqueKey: finalUniqueKey, // Prefer server key, fallback to saved one
          isLoading: false
        };
        
        setAuth(authData);
        
        // Update localStorage with authenticated status
        localStorage.setItem('auth_state', JSON.stringify(authData));
        
        // If backend generated a new key, show it in modal
        if (showUniqueKey && uniqueKey) {
          setKeyMessage(uniqueKey);
          setShowModal(true);
          // Don't navigate yet, let user see the key first
        } else {
          // Show info message if user logged in without key
          if (uniqueKeyMissing && !finalUniqueKey) {
            console.log('[Login] User logged in without a uniqueKey. ' + (message || 'Please use your saved key to encrypt/decrypt entries.'));
          }
          // Go to dashboard
        navigate('/dashboard');
        }
      } else {
        // Signup - Show unique key modal ONLY on first account creation
        if (!formData.email) {
          setError('Email is required for registration');
          setLoading(false);
          return;
        }

        const response = await authAPI.createUser({
          userName: formData.userName,
          password: formData.password,
          email: formData.email
        });

        const { username, email, roles, uniqueKey } = response.data;
        
        // Save the uniqueKey to localStorage immediately so it's available for login
        const authData = {
          isAuthenticated: false, // Not logged in yet
          user: { username, email, roles },
          uniqueKey: uniqueKey, // Save the key for later use
          isLoading: false
        };
        
        localStorage.setItem('auth_state', JSON.stringify(authData));
        
        // Show the unique key modal
        setKeyMessage(uniqueKey);
        setShowModal(true);
        setFormData({ userName: '', password: '', email: '' });
      }
    } catch (err) {
      setError(err.response?.data || 'An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleModalClose = () => {
    setShowModal(false);
    // If user is already authenticated (auto-generated key on login), go to dashboard
    if (auth.isAuthenticated) {
      navigate('/dashboard');
    } else {
    // After signup, switch to login tab so user can sign in
    setIsLogin(true);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1 className="login-title">🔐 My Secure Journal</h1>
          <p className="login-subtitle">
            {isLogin ? 'Welcome back! Sign in to access your journal.' : 'Create your account to start journaling securely.'}
          </p>
        </div>

        <div className="login-tabs">
          <button 
            className={`tab-button ${isLogin ? 'active' : ''}`}
            onClick={() => {
              setIsLogin(true);
              setError('');
            }}
          >
            Sign In
          </button>
          <button 
            className={`tab-button ${!isLogin ? 'active' : ''}`}
            onClick={() => {
              setIsLogin(false);
              setError('');
            }}
          >
            Sign Up
          </button>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-group">
            <label className="form-label">Username</label>
            <input
              type="text"
              name="userName"
              value={formData.userName}
              onChange={handleInputChange}
              className="form-input"
              placeholder="Enter your username"
              required
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                className="form-input"
                placeholder="Enter your email"
                required
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleInputChange}
              className="form-input"
              placeholder="Enter your password"
              required
              minLength={6}
            />
          </div>

          <button 
            type="submit" 
            className="btn btn-primary login-button"
            disabled={loading}
          >
            {loading && <span className="loading-spinner"></span>}
            {isLogin ? 'Sign In' : 'Create Account'}
          </button>
        </form>
      </div>

      {showModal && (
        <UniqueKeyModal 
          uniqueKey={keyMessage}
          onClose={handleModalClose}
        />
      )}
    </div>
  );
};

export default LoginForm;

