import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import { authAPI } from '../../services/api';
import { authState, showUniqueKeyModal, uniqueKeyMessage } from '../../state/atoms';
import UniqueKeyModal from './UniqueKeyModal';
import Lottie from 'lottie-react';
import secureLoginAnimation from '../../lotties/Secure Login.json';
import './LoginForm.css';
import './GoogleOneTapHandler';

import { useEffect } from 'react';

const LoginForm = () => {
  useEffect(() => {
    if (window.google && window.google.accounts && window.google.accounts.id) {
      window.google.accounts.id.initialize({
        client_id: process.env.REACT_APP_GOOGLE_CLIENT_ID || '',
        callback: window.handleCredentialResponse
      });
      // Optionally: Uncomment to trigger One Tap immediately on mount:
      // window.google.accounts.id.prompt();
    }
  }, []);

  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    userName: '',
    password: '',
    email: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showForgotFlow, setShowForgotFlow] = useState(false);
  const [forgotStep, setForgotStep] = useState('email');
  const [forgotCode, setForgotCode] = useState('');
  const [forgotNewPassword, setForgotNewPassword] = useState('');
  const [forgotConfirmPassword, setForgotConfirmPassword] = useState('');
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotError, setForgotError] = useState('');
  const [forgotMessage, setForgotMessage] = useState('');
  
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

  const handleForgotRequest = async () => {
    const userNameToUse = formData.userName.trim();
    if (!userNameToUse) {
      setForgotError('Please enter your username on the sign-in form first.');
      return;
    }
    setForgotLoading(true);
    setForgotError('');
    setForgotMessage('');
    try {
      const response = await authAPI.requestResetCode(userNameToUse);
      setForgotMessage(response.data?.message || 'Reset code sent.');
      setForgotStep('code');
    } catch (err) {
      setForgotError(err.response?.data || 'Failed to send reset code.');
    } finally {
      setForgotLoading(false);
    }
  };

  const handleForgotVerify = async () => {
    setForgotLoading(true);
    setForgotError('');
    try {
      const response = await authAPI.verifyResetCode(formData.userName.trim(), forgotCode.trim());
      setForgotMessage(response.data?.message || 'Code verified.');
      setForgotStep('reset');
    } catch (err) {
      setForgotError(err.response?.data || 'Invalid code.');
    } finally {
      setForgotLoading(false);
    }
  };

  const handleForgotReset = async () => {
    setForgotLoading(true);
    setForgotError('');
    if (forgotNewPassword.trim().length < 6) {
      setForgotError('Password must be at least 6 characters.');
      setForgotLoading(false);
      return;
    }
    if (forgotNewPassword.trim() !== forgotConfirmPassword.trim()) {
      setForgotError('Passwords do not match.');
      setForgotLoading(false);
      return;
    }
    try {
      const response = await authAPI.resetPassword(
        formData.userName.trim(),
        forgotCode.trim(),
        forgotNewPassword.trim()
      );
      setForgotMessage(response.data?.message || 'Password reset successful.');
      setForgotStep('success');
    } catch (err) {
      setForgotError(err.response?.data || 'Failed to reset password.');
    } finally {
      setForgotLoading(false);
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
    <div className="login-page-container">
      {/* Left Side - Form Card */}
      <div className="login-form-section">
      <div className="login-card">
          {/* Header */}
          <div className="brand-header">
            <div className="brand-logo">
              <span className="lock-icon">🔐</span>
              <h1 className="brand-name">Quendora</h1>
            </div>
            <p className="brand-tagline">Your secure vault for documents, memories, and private thoughts</p>
            <p className="welcome-text">Welcome back! Sign in to access your journal.</p>
        </div>

          {/* Tab Switcher */}
          <div className="auth-tabs">
          <button 
              className={`auth-tab ${isLogin ? 'active' : ''}`}
            onClick={() => {
              setIsLogin(true);
              setError('');
            }}
          >
            Sign In
          </button>
          <button 
              className={`auth-tab ${!isLogin ? 'active' : ''}`}
            onClick={() => {
              setIsLogin(false);
              setError('');
            }}
          >
            Sign Up
          </button>
        </div>

          {/* Form */}
          {!showForgotFlow && (
          <form onSubmit={handleSubmit} className="auth-form">
            {error && <div className="error-alert">{error}</div>}

            {/* Username */}
            <div className="input-group">
              <label className="input-label">Username</label>
              <div className="input-field">
                <span className="field-icon">👤</span>
            <input
              type="text"
              name="userName"
              value={formData.userName}
              onChange={handleInputChange}
                  className="text-input"
              placeholder="Enter your username"
              required
            />
              </div>
          </div>

            {/* Email (Sign Up only) */}
          {!isLogin && (
              <div className="input-group">
                <label className="input-label">Email</label>
                <div className="input-field">
                  <span className="field-icon">📧</span>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                    className="text-input"
                placeholder="Enter your email"
                required
              />
                </div>
            </div>
          )}

            {/* Password */}
            <div className="input-group">
              <label className="input-label">Password</label>
              <div className="input-field password-field">
                <span className="field-icon">🔒</span>
            <input
              type={showPassword ? 'text' : 'password'}
              name="password"
              value={formData.password}
              onChange={handleInputChange}
                  className="text-input"
              placeholder="Enter your password"
              required
              minLength={6}
            />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
              {isLogin && (
                <button
                  type="button"
                  className="forgot-password-link"
                  onClick={() => {
                    setShowForgotFlow(true);
                    setForgotStep('email');
                    setForgotError('');
                    setForgotMessage('');
                  }}
                >
                  Forgot password?
                </button>
              )}
          </div>

            {/* Submit Button */}
          <button 
            type="submit" 
              className="submit-button"
            disabled={loading}
          >
              {loading ? '⏳ Processing...' : isLogin ? 'Sign In' : 'Create Account'}
            </button>
        </form>
        )}
        {showForgotFlow && (
          <div className="auth-form">
            <div className="forgot-card">
              <h3 className="forgot-title">Forgot Password</h3>
              <p className="forgot-desc">
                Click below to send a reset code to your registered email.
              </p>
              {forgotError && <div className="error-alert">{forgotError}</div>}
              {forgotMessage && <div className="success-alert">{forgotMessage}</div>}

              {forgotStep === 'email' && (
                <>
                  <button
                    type="button"
                    className="submit-button"
                    onClick={handleForgotRequest}
                    disabled={forgotLoading}
                  >
                    {forgotLoading ? 'Sending...' : 'Send Reset Code'}
                  </button>
                </>
              )}

              {forgotStep === 'code' && (
                <>
                  <div className="input-group">
                    <label className="input-label">Verification Code</label>
                    <div className="input-field">
                      <span className="field-icon">🔑</span>
                      <input
                        type="text"
                        className="text-input"
                        placeholder="Enter the code from your email"
                        value={forgotCode}
                        onChange={(e) => setForgotCode(e.target.value)}
                      />
                    </div>
                  </div>
                  <button
                    type="button"
                    className="submit-button"
                    onClick={handleForgotVerify}
                    disabled={forgotLoading || !forgotCode.trim()}
                  >
                    {forgotLoading ? 'Verifying...' : 'Verify Code'}
                  </button>
                </>
              )}

              {forgotStep === 'reset' && (
                <>
                  <div className="input-group">
                    <label className="input-label">New Password</label>
                    <div className="input-field">
                      <span className="field-icon">🔒</span>
                      <input
                        type="password"
                        className="text-input"
                        placeholder="Enter new password"
                        value={forgotNewPassword}
                        onChange={(e) => setForgotNewPassword(e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="input-group">
                    <label className="input-label">Confirm Password</label>
                    <div className="input-field">
                      <span className="field-icon">🔒</span>
                      <input
                        type="password"
                        className="text-input"
                        placeholder="Confirm new password"
                        value={forgotConfirmPassword}
                        onChange={(e) => setForgotConfirmPassword(e.target.value)}
                      />
                    </div>
                  </div>
                  <button
                    type="button"
                    className="submit-button"
                    onClick={handleForgotReset}
                    disabled={forgotLoading}
                  >
                    {forgotLoading ? 'Resetting...' : 'Reset Password'}
                  </button>
                </>
              )}

              {forgotStep === 'success' && (
                <>
                  <p className="forgot-desc">Your password has been reset. Please sign in again.</p>
                  <button
                    type="button"
                    className="submit-button"
                    onClick={() => {
                      setShowForgotFlow(false);
                      setForgotStep('email');
                      setForgotCode('');
                      setForgotNewPassword('');
                      setForgotConfirmPassword('');
                      setForgotError('');
                      setForgotMessage('');
                    }}
                  >
                    Back to Sign In
                  </button>
                </>
              )}

              <button
                type="button"
                className="btn btn-secondary"
                style={{ marginTop: '0.75rem' }}
                onClick={() => setShowForgotFlow(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        )}
        </div>
      </div>

      {/* Right Side - Lottie Animation & Feature Cards */}
      <div className="login-features-section">
        {/* Lottie Animation */}
        <div className="lottie-container">
          <Lottie 
            animationData={secureLoginAnimation} 
            loop={true}
            style={{ width: '100%', height: '100%', maxWidth: '600px' }}
          />
        </div>

        {/* Feature Cards */}
        <div className="feature-cards">
          <div className="feature-card">
            <div className="feature-icon yellow">🔒</div>
            <div className="feature-text">
              <h3>End-to-End Encryption</h3>
              <p>Your data is encrypted to keep it safe and private.</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon blue">🛡️</div>
            <div className="feature-text">
              <h3>Secure Storage</h3>
              <p>Safeguard your files with our secure storage solution.</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon yellow">🔐</div>
            <div className="feature-text">
              <h3>Private & Confidential</h3>
              <p>We ensure your data remains private and confidential.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Unique Key Modal */}
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
