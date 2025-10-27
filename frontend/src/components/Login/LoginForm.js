import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import { authAPI } from '../../services/api';
import { authState, showUniqueKeyModal, uniqueKeyMessage } from '../../state/atoms';
import { saveAuthData } from '../../utils/auth';
import UniqueKeyDisplay from './UniqueKeyDisplay';
import Toast from '../UI/Toast';
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
  const [success, setSuccess] = useState('');
  const [toast, setToast] = useState(null);
  
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
    setSuccess('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      if (isLogin) {
        // Login Logic
        const response = await authAPI.login({
          userName: formData.userName,
          password: formData.password
        });

        const { username, email, roles, uniqueKey, showUniqueKey, uniqueKeyMessage } = response.data;
        
        const user = { username, email, roles };
        saveAuthData(user, uniqueKey);
        
        // Store the unique key for API authentication
        localStorage.setItem('uniqueKey', uniqueKey);
        
        setAuth({
          isAuthenticated: true,
          user,
          uniqueKey,
          isLoading: false
        });

        // Show unique key in toast message for first login
        if (showUniqueKey) {
          setToast({
            message: `🔐 Your Unique Key: ${uniqueKey}\n\nSave this key securely! You'll need it to decrypt your journal entries.`,
            type: 'warning',
            duration: 15000
          });
        }
        
        navigate('/dashboard');
      } else {
        // Registration Logic
        if (!formData.email) {
          setError('Email is required for registration');
          return;
        }

        const response = await authAPI.createUser({
          userName: formData.userName,
          password: formData.password,
          email: formData.email
        });

        setSuccess('Account created successfully! Your unique key has been generated.');
        setKeyMessage(`${response.data.note}\n\nYour Unique Key: ${response.data.uniqueKey}`);
        setShowModal(true);
        
        // Reset form
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
    if (isLogin) {
      navigate('/dashboard');
    } else {
      setIsLogin(true); // Switch to login after successful registration
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1 className="login-title">My Journal App</h1>
          <p className="login-subtitle">
            {isLogin ? 'Welcome back! Please sign in to your account.' : 'Create your account to get started.'}
          </p>
        </div>

        <div className="login-tabs">
          <button 
            className={`tab-button ${isLogin ? 'active' : ''}`}
            onClick={() => setIsLogin(true)}
          >
            Sign In
          </button>
          <button 
            className={`tab-button ${!isLogin ? 'active' : ''}`}
            onClick={() => setIsLogin(false)}
          >
            Sign Up
          </button>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

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

        <div className="login-footer">
          <p>
            {isLogin ? "Don't have an account? " : "Already have an account? "}
            <button 
              type="button"
              className="link-button"
              onClick={() => setIsLogin(!isLogin)}
            >
              {isLogin ? 'Sign up here' : 'Sign in here'}
            </button>
          </p>
        </div>
      </div>

      {showModal && (
        <UniqueKeyDisplay 
          message={keyMessage}
          onClose={handleModalClose}
        />
      )}
      
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          duration={toast.duration}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default LoginForm;
