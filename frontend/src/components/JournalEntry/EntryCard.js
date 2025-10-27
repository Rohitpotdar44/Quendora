import React, { useState } from 'react';
import { journalAPI } from '../../services/api';
import EditEntry from './EditEntry';
import './EntryCard.css';

const EntryCard = ({ entry }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [isDecrypted, setIsDecrypted] = useState(false);
  const [decryptedContent, setDecryptedContent] = useState({ title: '', content: '' });
  const [secretKey, setSecretKey] = useState('');
  const [showDecryptForm, setShowDecryptForm] = useState(false);
  const [decryptError, setDecryptError] = useState('');

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const truncateContent = (content, maxLength = 150) => {
    if (content.length <= maxLength) return content;
    return content.substring(0, maxLength) + '...';
  };

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleSaveEdit = (updatedEntry) => {
    // TODO: Update the entry in the state/backend
    console.log('Updated entry:', updatedEntry);
    setIsEditing(false);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this entry? This action cannot be undone.')) {
      // TODO: Delete the entry from state/backend
      console.log('Delete entry:', entry.id);
    }
  };

  const handleDecrypt = async () => {
    if (!secretKey.trim()) {
      setDecryptError('Please enter a secret key');
      return;
    }

    try {
      setDecryptError('');
      
      console.log('Decrypting with key:', secretKey);
      console.log('Entry data:', { title: entry.title, content: entry.content });
      
      // Call the backend decryption API
      const requestData = {
        entryData: {
          title: entry.title,
          content: entry.content
        },
        secretKey: secretKey
      };
      
      console.log('Sending request data:', requestData);
      
      const response = await journalAPI.decryptEntry(requestData);
      
      console.log('Decryption response:', response.data);
      
      // Update the decrypted content
      setDecryptedContent({
        title: response.data.title,
        content: response.data.content
      });
      
      setIsDecrypted(true);
      setShowDecryptForm(false);
      
    } catch (err) {
      console.error('Decryption error:', err);
      console.error('Error response:', err.response);
      console.error('Error status:', err.response?.status);
      console.error('Error data:', err.response?.data);
      console.error('Full error:', err);
      setDecryptError(err.response?.data || 'Failed to decrypt entry. Please check your secret key.');
    }
  };

  const isEncrypted = (text) => {
    // Simple check if text looks like Base64 encoded (encrypted)
    try {
      return btoa(atob(text)) === text && text.length > 20;
    } catch {
      return false;
    }
  };

  const displayTitle = isDecrypted ? decryptedContent.title : entry.title;
  const displayContent = isDecrypted ? decryptedContent.content : entry.content;
  const isContentEncrypted = isEncrypted(entry.title) || isEncrypted(entry.content);

  if (isEditing) {
    return (
      <EditEntry 
        entry={entry}
        onSave={handleSaveEdit}
        onCancel={handleCancelEdit}
      />
    );
  }

  return (
    <div className="entry-card">
      <div className="entry-header">
        <h3 className="entry-title">
          {isContentEncrypted ? '🔒 [Encrypted Entry]' : displayTitle}
        </h3>
        <div className="entry-actions">
          {isContentEncrypted && !isDecrypted && (
            <button 
              onClick={() => setShowDecryptForm(!showDecryptForm)}
              className="action-btn decrypt-btn"
              title="Decrypt entry"
            >
              🔓
            </button>
          )}
          <button 
            onClick={handleEdit}
            className="action-btn edit-btn"
            title="Edit entry"
          >
            ✏️
          </button>
          <button 
            onClick={handleDelete}
            className="action-btn delete-btn"
            title="Delete entry"
          >
            🗑️
          </button>
        </div>
      </div>

      {showDecryptForm && !isDecrypted && (
        <div className="decrypt-form">
          <div className="form-group">
            <label className="form-label">Secret Key:</label>
            <input
              type="password"
              value={secretKey}
              onChange={(e) => setSecretKey(e.target.value)}
              className="form-input"
              placeholder="Enter your secret key to decrypt (PdRgUkXp2s5v8y/B)"
            />
            {decryptError && <div className="alert alert-error">{decryptError}</div>}
            <div className="decrypt-actions">
              <button 
                onClick={() => {
                  console.log('Current auth state:', localStorage.getItem('uniqueKey'));
                  console.log('Entry data being sent:', { title: entry.title, content: entry.content });
                }}
                className="btn btn-info btn-sm"
              >
                Debug Info
              </button>
              <button 
                onClick={handleDecrypt}
                className="btn btn-primary btn-sm"
              >
                Decrypt Entry
              </button>
              <button 
                onClick={() => setShowDecryptForm(false)}
                className="btn btn-secondary btn-sm"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="entry-content">
        <p className="entry-text">
          {isContentEncrypted && !isDecrypted ? (
            <span className="encrypted-text">
              🔒 This entry is encrypted. Use the decrypt button above to view the content.
            </span>
          ) : (
            isExpanded ? displayContent : truncateContent(displayContent)
          )}
        </p>
        {!isContentEncrypted && displayContent.length > 150 && (
          <button 
            onClick={() => setIsExpanded(!isExpanded)}
            className="expand-btn"
          >
            {isExpanded ? 'Show Less' : 'Read More'}
          </button>
        )}
      </div>

      <div className="entry-footer">
        <div className="entry-meta">
          <span className="entry-date">
            📅 Created: {formatDate(entry.createdAt || entry.localDateTime)}
          </span>
          {entry.updatedAt !== entry.createdAt && (
            <span className="entry-updated">
              ✏️ Updated: {formatDate(entry.updatedAt)}
            </span>
          )}
          {isContentEncrypted && (
            <span className="encryption-status">
              🔒 Encrypted
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

export default EntryCard;
