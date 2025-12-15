import React, { useState } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import './CreateEntry.css';

const CreateEntry = ({ onSave, onCancel }) => {
  const auth = useRecoilValue(authState);
  const [formData, setFormData] = useState({
    title: '',
    content: ''
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

    setLoading(true);
    
    try {
      // Get fresh auth from localStorage
      const authData = localStorage.getItem('auth_state');
      if (!authData) {
        setError('Authentication data not found. Please log in again.');
        setLoading(false);
        return;
      }

      const parsedAuth = JSON.parse(authData);
      const uniqueKey = parsedAuth.uniqueKey;

      if (!uniqueKey) {
        setError('Unique key not found. Please log in again.');
        setLoading(false);
        return;
      }

      console.log('[CreateEntry] Preparing to send entry...');
      console.log('[CreateEntry] Title:', formData.title.trim());
      console.log('[CreateEntry] Content length:', formData.content.trim().length);
      console.log('[CreateEntry] Has uniqueKey:', !!uniqueKey);
      
      const requestData = {
        entry: {
        title: formData.title.trim(),
        content: formData.content.trim()
        },
        uniqueKey: uniqueKey
      };

      console.log('[CreateEntry] Sending to backend:', requestData);
      
      const response = await journalAPI.createEntry(requestData);
      
      console.log('[CreateEntry] ✅ Backend response:', response.data);
      
      onSave();
      setFormData({ title: '', content: '' });
    } catch (err) {
      console.error('[CreateEntry] ❌ Full error:', err);
      console.error('[CreateEntry] ❌ Error response:', err.response);
      setError(err.response?.data || 'Failed to create entry. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-entry">
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">✍️ Create New Entry</h3>
          <p className="card-subtitle">Your entry will be encrypted with your unique key</p>
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
              disabled={loading || !formData.title.trim() || !formData.content.trim()}
            >
              {loading && <span className="loading-spinner"></span>}
              Save Entry
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateEntry;

