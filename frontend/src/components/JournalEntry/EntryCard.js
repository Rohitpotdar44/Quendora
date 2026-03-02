import React, { useState } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import EditEntry from './EditEntry';
import ConfirmDialog from '../Common/ConfirmDialog';
import './EntryCard.css';

const EntryCard = ({ entry, onDelete, onEdit }) => {
  const auth = useRecoilValue(authState);
  const [isDecrypted, setIsDecrypted] = useState(false);
  const [decryptedContent, setDecryptedContent] = useState({ title: '', content: '' });
  const [showUnlockForm, setShowUnlockForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [uniqueKeyInput, setUniqueKeyInput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteKeyInput, setDeleteKeyInput] = useState('');
  const [deleteError, setDeleteError] = useState('');

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
    setDeleteKeyInput('');
    setDeleteError('');
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    const keyToUse = deleteKeyInput.trim() || auth.uniqueKey;
    
    if (!keyToUse) {
      setDeleteError('Please enter your unique key to delete this entry');
      return;
    }

    setDeleteError('');
    setShowDeleteConfirm(false);
    
    try {
      await onDelete(entry.id, keyToUse);
      setDeleteKeyInput('');
    } catch (err) {
      setDeleteError('Failed to delete entry. Please check your unique key.');
      console.error('[EntryCard] Delete error:', err);
    }
  };

  const handleEdit = () => {
    // Can only edit if decrypted
    if (isDecrypted) {
      setShowEditForm(true);
    } else {
      setError('Please decrypt the entry first to edit it');
    }
  };

  const handleEditSave = (updatedContent) => {
    setShowEditForm(false);
    if (updatedContent) {
      setDecryptedContent({
        title: updatedContent.title,
        content: updatedContent.content
      });
      setIsDecrypted(true);
    } else {
      setIsDecrypted(false);
      setDecryptedContent({ title: '', content: '' });
    }
    if (onEdit) {
      onEdit();
    }
  };

  const locked = isEncrypted(entry.title) || isEncrypted(entry.content);
  const displayTitle = isDecrypted ? decryptedContent.title : (locked ? '🔒 [Encrypted Entry]' : entry.title);
  const displayContent = isDecrypted ? decryptedContent.content : entry.content;

  if (showEditForm && isDecrypted) {
    return (
      <EditEntry 
        entry={{
          ...entry,
          title: decryptedContent.title,
          content: decryptedContent.content
        }}
        onSave={handleEditSave}
        onCancel={() => setShowEditForm(false)}
      />
    );
  }

  return (
    <div className="entry-card">
      <div className="entry-header">
        <h3 className="entry-title">{displayTitle}</h3>
        <div className="entry-actions">
          {locked && !isDecrypted && (
            <button 
              onClick={() => setShowUnlockForm(!showUnlockForm)}
              className="btn-unlock-fancy"
              title="Click to unlock and view entry"
            >
              <span className="unlock-icon">🔓</span>
              <span className="unlock-text">Unlock Entry</span>
            </button>
          )}
          {locked && isDecrypted && (
          <button 
              onClick={handleLock}
              className="action-btn lock-btn"
              title="Lock entry again"
          >
              🔒 Lock
          </button>
          )}
          {isDecrypted && (
            <button 
              onClick={handleEdit}
              className="action-btn edit-btn"
              title="Edit entry"
            >
              ✏️ Edit
          </button>
          )}
          <button 
            onClick={handleDelete}
            className="action-btn delete-btn"
            title="Delete entry"
          >
            🗑️ Delete
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
              placeholder="Enter your unique key and press Enter"
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  handleUnlock();
                }
              }}
              autoFocus
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
        {locked && !isDecrypted && (
          <span className="encryption-badge">🔒 Encrypted</span>
          )}
        {isDecrypted && (
          <span className="unlocked-badge">🔓 Unlocked</span>
          )}
      </div>

      {showDeleteConfirm && (
        <div className="confirm-overlay">
          <div className="confirm-dialog">
            <div className="confirm-header">
              <h3>Delete Entry</h3>
            </div>
            <div className="confirm-body">
              <p>Are you sure you want to delete this entry? This action cannot be undone.</p>
              <div style={{ marginTop: '1rem' }}>
                <label htmlFor="delete-key" style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500 }}>
                  Enter your unique key to confirm deletion:
                </label>
                <input
                  id="delete-key"
                  type="password"
                  value={deleteKeyInput}
                  onChange={(e) => {
                    setDeleteKeyInput(e.target.value);
                    setDeleteError('');
                  }}
                  placeholder={auth.uniqueKey ? 'Enter key (or use saved key)' : 'Enter your unique key'}
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    border: deleteError ? '2px solid #e74c3c' : '2px solid #ddd',
                    borderRadius: '4px',
                    fontSize: '0.9rem'
                  }}
                />
                {deleteError && (
                  <p style={{ color: '#e74c3c', fontSize: '0.85rem', marginTop: '0.5rem' }}>{deleteError}</p>
                )}
              </div>
            </div>
            <div className="confirm-actions">
              <button onClick={() => {
                setShowDeleteConfirm(false);
                setDeleteKeyInput('');
                setDeleteError('');
              }} className="btn btn-secondary">
                Cancel
              </button>
              <button onClick={confirmDelete} className="btn btn-danger">
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EntryCard;

