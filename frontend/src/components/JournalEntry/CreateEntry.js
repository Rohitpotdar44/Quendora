import React, { useState } from 'react';
import { journalAPI } from '../../services/api';
import './CreateEntry.css';

const CreateEntry = ({ onSave, onCancel }) => {
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
      // Create entry data for API
      const entryData = {
        title: formData.title.trim(),
        content: formData.content.trim()
      };

      // Call the backend API to create entry
      const response = await journalAPI.createEntry(entryData);
      
      // The backend will return the encrypted entry
      const newEntry = {
        id: response.data.id,
        title: response.data.title, // This will be encrypted
        content: response.data.content, // This will be encrypted
        localDateTime: response.data.localDateTime,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      onSave(newEntry);
      
      // Reset form
      setFormData({ title: '', content: '' });
    } catch (err) {
      console.error('Error creating entry:', err);
      setError(err.response?.data?.message || 'Failed to create entry. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-entry">
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">✍️ Create New Journal Entry</h3>
          <p className="card-subtitle">Share your thoughts, experiences, and reflections</p>
        </div>

        <form onSubmit={handleSubmit} className="create-entry-form">
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-group">
            <label className="form-label">Entry Title</label>
            <input
              type="text"
              name="title"
              value={formData.title}
              onChange={handleInputChange}
              className="form-input"
              placeholder="Give your entry a meaningful title..."
              maxLength={100}
            />
            <small className="char-count">
              {formData.title.length}/100 characters
            </small>
          </div>

          <div className="form-group">
            <label className="form-label">Your Thoughts</label>
            <textarea
              name="content"
              value={formData.content}
              onChange={handleInputChange}
              className="form-textarea"
              placeholder="Write about your day, thoughts, feelings, experiences, or anything that's on your mind..."
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
