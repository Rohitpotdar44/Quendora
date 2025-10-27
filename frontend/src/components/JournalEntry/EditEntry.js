import React, { useState } from 'react';
import './EditEntry.css';

const EditEntry = ({ entry, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    title: entry.title,
    content: entry.content
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
      // For now, we'll create a mock updated entry
      const updatedEntry = {
        ...entry,
        title: formData.title.trim(),
        content: formData.content.trim(),
        updatedAt: new Date().toISOString()
      };

      // TODO: Replace with actual API call when journal endpoints are ready
      // const response = await journalAPI.updateEntry(entry.id, formData);
      
      onSave(updatedEntry);
    } catch (err) {
      setError('Failed to update entry. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="edit-entry">
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">✏️ Edit Journal Entry</h3>
          <p className="card-subtitle">Update your thoughts and reflections</p>
        </div>

        <form onSubmit={handleSubmit} className="edit-entry-form">
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
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditEntry;
