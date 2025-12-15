import React, { useState } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import './EntryCard.css';

const EntryCard = ({ entry, onDelete }) => {
  const auth = useRecoilValue(authState);
  const [isDecrypted, setIsDecrypted] = useState(false);
  const [decryptedContent, setDecryptedContent] = useState({ title: '', content: '' });
  const [showUnlockForm, setShowUnlockForm] = useState(false);
  const [uniqueKeyInput, setUniqueKeyInput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown date';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isEncrypted = (text) => {
    if (!text) return false;
    try {
      // Check if looks like Base64
      return btoa(atob(text)) === text && text.length > 20;
    } catch {
      return false;
    }
  };

  const handleUnlock = async () => {
    const keyToUse = uniqueKeyInput.trim() || auth.uniqueKey;
    
    if (!keyToUse) {
      setError('Please enter your unique key');
      return;
    }

    setLoading(true);
    setError('');
      
    try {
      console.log('[EntryCard] Decrypting entry...');
      
      const response = await journalAPI.decryptEntry({
        entryData: {
          title: entry.title,
          content: entry.content
        },
        uniqueKey: keyToUse
      });
      
      console.log('[EntryCard] ✅ Decrypted successfully');
      
      setDecryptedContent({
        title: response.data.title,
        content: response.data.content
      });
      
      setIsDecrypted(true);
      setShowUnlockForm(false);
      setUniqueKeyInput('');
    } catch (err) {
      console.error('[EntryCard] ❌ Decryption failed:', err);
      setError('Failed to decrypt. Please check your unique key.');
    } finally {
      setLoading(false);
    }
  };

  const handleLock = () => {
    console.log('[EntryCard] Locking entry...');
    setIsDecrypted(false);
    setDecryptedContent({ title: '', content: '' });
    setUniqueKeyInput('');
    setError('');
    console.log('[EntryCard] ✅ Entry locked');
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this entry? This action cannot be undone.')) {
      onDelete(entry.id);
    }
  };

  const locked = isEncrypted(entry.title) || isEncrypted(entry.content);
  const displayTitle = isDecrypted ? decryptedContent.title : (locked ? '🔒 [Encrypted Entry]' : entry.title);
  const displayContent = isDecrypted ? decryptedContent.content : entry.content;

  return (
    <div className="entry-card">
      <div className="entry-header">
        <h3 className="entry-title">{displayTitle}</h3>
        <div className="entry-actions">
          {locked && !isDecrypted && (
            <button 
              onClick={() => setShowUnlockForm(!showUnlockForm)}
              className="action-btn unlock-btn"
              title="Unlock entry"
            >
              🔓
            </button>
          )}
          {locked && isDecrypted && (
          <button 
              onClick={handleLock}
              className="action-btn lock-btn"
              title="Lock entry again"
          >
              🔒
          </button>
          )}
          <button 
            onClick={handleDelete}
            className="action-btn delete-btn"
            title="Delete entry"
          >
            🗑️
          </button>
        </div>
      </div>

      {showUnlockForm && !isDecrypted && (
        <div className="unlock-form">
          <div className="form-group">
            <label className="form-label">🔑 Enter Unique Key:</label>
            <input
              type="password"
              value={uniqueKeyInput}
              onChange={(e) => setUniqueKeyInput(e.target.value)}
              className="form-input"
              placeholder="Leave empty to use saved key"
            />
            {error && <div className="alert alert-error">{error}</div>}
            <div className="unlock-actions">
              <button 
                onClick={handleUnlock}
                className="btn btn-primary btn-sm"
                disabled={loading}
              >
                {loading ? '🔄 Unlocking...' : '🔓 Unlock'}
              </button>
              <button 
                onClick={() => setShowUnlockForm(false)}
                className="btn btn-secondary btn-sm"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="entry-content">
        {locked && !isDecrypted ? (
          <div className="encrypted-preview">
            <p className="encrypted-text">🔒 This entry is encrypted</p>
            <div className="encrypted-hash">
              <code>{displayContent.substring(0, 60)}...</code>
            </div>
            <p className="hint-text">Click the unlock button above to view the content</p>
          </div>
        ) : (
          <p className="entry-text">{displayContent}</p>
        )}
      </div>

      <div className="entry-footer">
          <span className="entry-date">
          📅 {formatDate(entry.localDateTime)}
          </span>
        {locked && (
          <span className="encryption-badge">🔒 Encrypted</span>
          )}
        {isDecrypted && (
          <span className="unlocked-badge">🔓 Unlocked</span>
          )}
      </div>
    </div>
  );
};

export default EntryCard;

