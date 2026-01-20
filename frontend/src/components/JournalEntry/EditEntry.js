import React, { useState } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import './CreateEntry.css';

const EditEntry = ({ entry, onSave, onCancel }) => {
  const auth = useRecoilValue(authState);
  const [formData, setFormData] = useState({
    title: entry.title || '',
    content: entry.content || '',
    uniqueKey: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.title.trim() || !formData.content.trim()) {
      setError('Both title and content are required');
      return;
    }

    if (!formData.uniqueKey.trim()) {
      setError('Please enter your unique key to re-encrypt this entry');
      return;
    }

    setLoading(true);
    
    try {
      console.log('[EditEntry] Updating entry:', entry.id);
      console.log('[EditEntry] New Title:', formData.title.trim());
      console.log('[EditEntry] New Content length:', formData.content.trim().length);
      
      const requestData = {
        entry: {
          title: formData.title.trim(),
          content: formData.content.trim()
        },
        uniqueKey: formData.uniqueKey.trim()
      };

      console.log('[EditEntry] Sending to backend:', requestData);
      
      const response = await journalAPI.updateEntry(entry.id, requestData);
      
      console.log('[EditEntry] ✅ Backend response:', response.data);
      
      onSave({
        title: formData.title.trim(),
        content: formData.content.trim()
      });
    } catch (err) {
      console.error('[EditEntry] ❌ Full error:', err);
      console.error('[EditEntry] ❌ Error response:', err.response);
      setError(err.response?.data || 'Failed to update entry. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-entry">
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">✏️ Edit Entry</h3>
          <p className="card-subtitle">Your changes will be re-encrypted with your unique key</p>
        </div>

        <form onSubmit={handleSubmit} className="create-entry-form">
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-group">
            <label className="form-label">Title</label>
            <input
              type="text"
              name="title"
              value={formData.title}
              onChange={handleInputChange}
              className="form-input"
              placeholder="Give your entry a title..."
              maxLength={100}
            />
            <small className="char-count">
              {formData.title.length}/100 characters
            </small>
          </div>

          <div className="form-group">
            <label className="form-label">Content</label>
            <textarea
              name="content"
              value={formData.content}
              onChange={handleInputChange}
              className="form-textarea"
              placeholder="Write your thoughts here..."
              rows={8}
              maxLength={2000}
            />
            <small className="char-count">
              {formData.content.length}/2000 characters
            </small>
          </div>

          <div className="form-group">
            <label className="form-label">🔑 Unique Key (Required to Re-encrypt)</label>
            <input
              type="password"
              name="uniqueKey"
              value={formData.uniqueKey}
              onChange={handleInputChange}
              className="form-input"
              placeholder="Enter your unique key to save changes..."
              required
            />
            <small className="form-hint">
              ⚠️ Your changes will be re-encrypted with this key.
            </small>
          </div>

          <div className="form-actions">
            <button 
              type="button" 
              onClick={onCancel}
              className="btn btn-secondary"
              disabled={loading}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={loading || !formData.title.trim() || !formData.content.trim() || !formData.uniqueKey.trim()}
            >
              {loading && <span className="loading-spinner"></span>}
              🔒 Re-encrypt & Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditEntry;

