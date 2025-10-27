import React, { useState } from 'react';
import './UniqueKeyDisplay.css';

const UniqueKeyDisplay = ({ message, onClose }) => {
  const [copied, setCopied] = useState(false);

  const extractUniqueKey = (message) => {
    const keyMatch = message.match(/Your Unique Key: ([A-F0-9]+)/);
    return keyMatch ? keyMatch[1] : null;
  };

  const uniqueKey = extractUniqueKey(message);

  const copyToClipboard = () => {
    if (uniqueKey) {
      navigator.clipboard.writeText(uniqueKey).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      });
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal unique-key-modal">
        <div className="modal-header">
          <h2 className="modal-title">🔐 Your Unique Authentication Key</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        
        <div className="unique-key-content">
          <div className="alert alert-warning">
            <strong>⚠️ Important:</strong> This key will not be shown again! Please save it securely.
          </div>
          
          <div className="message-text">
            {message.split('\n').map((line, index) => (
              <p key={index}>{line}</p>
            ))}
          </div>

          {uniqueKey && (
            <div className="unique-key-display">
              <label className="form-label">Your Unique Key:</label>
              <div className="key-container">
                <input 
                  type="text" 
                  value={uniqueKey} 
                  readOnly 
                  className="key-input"
                />
                <button 
                  onClick={copyToClipboard}
                  className="btn btn-secondary copy-btn"
                  title="Copy to clipboard"
                >
                  {copied ? '✓ Copied!' : '📋 Copy'}
                </button>
              </div>
            </div>
          )}

          <div className="alert alert-info">
            <strong>💡 How to use:</strong> Use this unique key for authentication instead of passwords in future logins.
          </div>
        </div>

        <div className="modal-actions">
          <button 
            onClick={onClose}
            className="btn btn-primary"
          >
            I've Saved My Key - Continue
          </button>
        </div>
      </div>
    </div>
  );
};

export default UniqueKeyDisplay;
