import React, { useState } from 'react';
import './UniqueKeyModal.css';

const UniqueKeyModal = ({ uniqueKey, onClose }) => {
  const [copied, setCopied] = useState(false);

  const copyToClipboard = () => {
    navigator.clipboard.writeText(uniqueKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content unique-key-modal">
        <div className="modal-header">
          <h2>🔑 Your Unique Encryption Key</h2>
        </div>
        
        <div className="modal-body">
          <div className="warning-box">
            <p><strong>⚠️ IMPORTANT: Save this key securely!</strong></p>
            <p>This key encrypts all your journal entries. You'll need it to decrypt and view your entries.</p>
            <p><strong>We cannot recover this key if you lose it!</strong></p>
          </div>

          <div className="unique-key-display">
            <code>{uniqueKey}</code>
          </div>

          <button 
            onClick={copyToClipboard}
            className="btn btn-primary copy-button"
          >
            {copied ? '✅ Copied!' : '📋 Copy Key'}
          </button>

          <div className="instructions">
            <p><strong>What to do with this key:</strong></p>
            <ul>
              <li>Save it in a password manager</li>
              <li>Write it down in a secure place</li>
              <li>You'll be asked for this key when unlocking encrypted entries</li>
            </ul>
          </div>
        </div>

        <div className="modal-footer">
          <p className="footer-note">After saving your key, you'll be able to sign in to your account.</p>
          <button onClick={onClose} className="btn btn-primary">
            I've Saved My Key - Continue to Login
          </button>
        </div>
      </div>
    </div>
  );
};

export default UniqueKeyModal;

