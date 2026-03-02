import React, { useState } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { journalAPI } from '../../services/api';
import { rewriteWithAI } from '../../services/aiService';
import './CreateEntry.css';

const CreateEntry = ({ onSave, onCancel }) => {
  const auth = useRecoilValue(authState);
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    uniqueKey: ''
  });
  const [loading, setLoading] = useState(false);
  const [rewriting, setRewriting] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleRewriteWithAI = async () => {
    if (!formData.title.trim() || !formData.content.trim()) {
      setError('Please enter both title and content to rewrite with AI');
      return;
    }

    if (formData.content.trim().length < 10) {
      setError('Content should be at least 10 characters long to rewrite');
      return;
    }

    setRewriting(true);
    setError('');

    try {
      console.log('[CreateEntry] Rewriting with AI...');
      const result = await rewriteWithAI(formData.title, formData.content);
      
      if (result && result.title && result.content) {
        setFormData({
          ...formData,
          title: result.title,
          content: result.content
        });
        console.log('[CreateEntry] ✅ Successfully rewritten with AI');
      } else {
        setError('Failed to rewrite with AI. Please try again.');
      }
    } catch (err) {
      console.error('[CreateEntry] ❌ Error rewriting with AI:', err);
      setError('Failed to rewrite with AI. Please check your OpenAI API key configuration.');
    } finally {
      setRewriting(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.title.trim() || !formData.content.trim()) {
      setError('Both title and content are required');
      return;
    }

    if (!formData.uniqueKey.trim()) {
      setError('Please enter your unique key to encrypt this entry');
      return;
    }

    setLoading(true);
    
    try {
      console.log('[CreateEntry] Preparing to send entry...');
      console.log('[CreateEntry] Title:', formData.title.trim());
      console.log('[CreateEntry] Content length:', formData.content.trim().length);
      console.log('[CreateEntry] Has uniqueKey:', !!formData.uniqueKey);
      
      const requestData = {
        entry: {
        title: formData.title.trim(),
        content: formData.content.trim()
        },
        uniqueKey: formData.uniqueKey.trim()
      };

      console.log('[CreateEntry] Sending to backend:', requestData);
      
      const response = await journalAPI.createEntry(requestData);
      
      console.log('[CreateEntry] ✅ Backend response:', response.data);
      
      onSave();
      setFormData({ title: '', content: '', uniqueKey: '' });
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

          <div className="form-group">
            <button
              type="button"
              onClick={handleRewriteWithAI}
              disabled={rewriting || loading}
              className="btn btn-ai-rewrite"
            >
              {rewriting ? (
                <>
                  <span className="loading-spinner"></span>
                  ✨ AI is Rewriting...
                </>
              ) : (
                <>
                  ✨ Rewrite with AI
                </>
              )}
            </button>
            <small className="form-hint ai-rewrite-hint">
              💡 Improve grammar, clarity, and flow while keeping your original meaning
            </small>
          </div>

          <div className="form-group">
            <label className="form-label">🔑 Unique Key (Required for Encryption)</label>
            <input
              type="password"
              name="uniqueKey"
              value={formData.uniqueKey}
              onChange={handleInputChange}
              className="form-input"
              placeholder="Enter your unique key to encrypt this entry..."
              required
            />
            <small className="form-hint">
              ⚠️ Your entry will be encrypted with this key. You'll need it to decrypt later.
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
              🔒 Encrypt & Save Entry
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateEntry;

