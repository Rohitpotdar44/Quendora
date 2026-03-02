import React, { useState, useEffect } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { fileAPI } from '../../services/api';
import FileCard from './FileCard';
import FileUpload from './FileUpload';
import './FileBrowser.css';

const FileBrowser = () => {
  const auth = useRecoilValue(authState);
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [showUploadForm, setShowUploadForm] = useState(false);
  const [filterType, setFilterType] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchKeyInput, setSearchKeyInput] = useState('');
  const [aiSearchIds, setAiSearchIds] = useState(null);
  const [aiSearchLoading, setAiSearchLoading] = useState(false);
  const [aiSearchError, setAiSearchError] = useState('');

  useEffect(() => {
    loadFiles();
  }, []);

  const loadFiles = async () => {
    try {
      setIsRefreshing(true);
      setError('');
      console.log('[FileBrowser] Loading files...');
      
      const response = await fileAPI.getAllFiles();
      console.log('[FileBrowser] ✅ Loaded', response.data?.length || 0, 'files');
      
      setFiles(response.data || []);
    } catch (err) {
      console.error('[FileBrowser] ❌ Failed to load files:', err);
      setError('Failed to load files. Please try again.');
      setFiles([]);
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  };

  const handleUploadSuccess = (data) => {
    console.log('[FileBrowser] File uploaded successfully:', data);
    setShowUploadForm(false);
    // Reload files to show the new upload
    loadFiles();
  };

  const handleDelete = async (fileId, secretKey) => {
    try {
      console.log('[FileBrowser] Deleting file:', fileId);
      await fileAPI.deleteFile(fileId, secretKey);
      console.log('[FileBrowser] ✅ File deleted');
      
      // Remove from local state
      setFiles(files.filter(f => f.id !== fileId));
    } catch (err) {
      console.error('[FileBrowser] ❌ Failed to delete file:', err);
      const errorMessage = err.response?.data || 'Failed to delete file. Please try again.';
      alert(errorMessage);
      throw err; // Re-throw so FileCard can handle it
    }
  };

  const handleDecryptSuccess = (fileId) => {
    console.log('[FileBrowser] File decrypted successfully:', fileId);
  };

  // Filter files by type
  const getFilteredFiles = () => {
    let filtered = files;

    // Filter by type
    if (filterType !== 'all') {
      filtered = filtered.filter(file => {
        const contentType = file.contentType || '';
        switch (filterType) {
          case 'images':
            return contentType.startsWith('image/');
          case 'videos':
            return contentType.startsWith('video/');
          case 'documents':
            return contentType.includes('pdf') || 
                   contentType.includes('document') || 
                   contentType.includes('word') ||
                   contentType.includes('text');
          default:
            return true;
        }
      });
    }

    // Sort by date (newest first)
    filtered.sort((a, b) => {
      const dateA = new Date(a.createdAt);
      const dateB = new Date(b.createdAt);
      return dateB - dateA;
    });

    const searchTerm = searchQuery.trim().toLowerCase();
    if (searchTerm) {
      filtered = filtered.filter(file => {
        const title = (file.plaintextTitle || file.originalFileName || file.aiSuggestedName || '').toLowerCase();
        return title.includes(searchTerm);
      });
    }

    return filtered;
  };

  const filteredFiles = getFilteredFiles();

  const getFileTypeCount = (type) => {
    if (type === 'all') return files.length;
    
    return files.filter(file => {
      const contentType = file.contentType || '';
      switch (type) {
        case 'images':
          return contentType.startsWith('image/');
        case 'videos':
          return contentType.startsWith('video/');
        case 'documents':
          return contentType.includes('pdf') || 
                 contentType.includes('document') || 
                 contentType.includes('word') ||
                 contentType.includes('text');
        default:
          return false;
      }
    }).length;
  };

  const handleAiSearch = async () => {
    if (!searchQuery.trim()) {
      setAiSearchIds(null);
      setAiSearchError('');
      return;
    }

    setAiSearchLoading(true);
    setAiSearchError('');
    try {
      const keyToUse = searchKeyInput.trim() || auth.uniqueKey;
      if (!keyToUse) {
        setAiSearchError('Please enter your unique key to search.');
        setAiSearchLoading(false);
        return;
      }
      const response = await fileAPI.searchFiles(searchQuery.trim(), keyToUse);
      const ids = (response.data || []).map(file => file.id);
      setAiSearchIds(ids);
    } catch (err) {
      console.error('[FileBrowser] AI search failed:', err);
      setAiSearchError('AI search failed. Please try again.');
      setAiSearchIds([]);
    } finally {
      setAiSearchLoading(false);
    }
  };

  const clearAiSearch = () => {
    setSearchQuery('');
    setAiSearchIds(null);
    setAiSearchError('');
  };

  return (
    <div className="file-browser">
      {/* Header */}
      <div className="browser-header">
        <div className="header-left">
          <p className="section-subtitle">
            All your encrypted files in one place
          </p>
        </div>
        <button 
          onClick={() => setShowUploadForm(!showUploadForm)}
          className="btn btn-primary upload-toggle-btn"
        >
          {showUploadForm ? '✕ Cancel Upload' : '⬆️ Upload File'}
        </button>
      </div>

      {/* Upload form */}
      {showUploadForm && (
        <FileUpload 
          onUploadSuccess={handleUploadSuccess}
          onCancel={() => setShowUploadForm(false)}
        />
      )}

      {/* Filters */}
      <div className="browser-filters">
        <div className="filter-tabs">
          <button 
            className={`filter-tab ${filterType === 'all' ? 'active' : ''}`}
            onClick={() => setFilterType('all')}
          >
            All Files ({getFileTypeCount('all')})
          </button>
          <button 
            className={`filter-tab ${filterType === 'images' ? 'active' : ''}`}
            onClick={() => setFilterType('images')}
          >
            🖼️ Images ({getFileTypeCount('images')})
          </button>
          <button 
            className={`filter-tab ${filterType === 'videos' ? 'active' : ''}`}
            onClick={() => setFilterType('videos')}
          >
            🎥 Videos ({getFileTypeCount('videos')})
          </button>
          <button 
            className={`filter-tab ${filterType === 'documents' ? 'active' : ''}`}
            onClick={() => setFilterType('documents')}
          >
            📝 Documents ({getFileTypeCount('documents')})
          </button>
        </div>
        <div className="filter-search">
          <input
            type="text"
            className="search-input"
            placeholder="Search file titles or content with AI"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <div className="ai-search-controls">
            <button
              type="button"
              className="btn btn-sm btn-ai-search"
              onClick={handleAiSearch}
              disabled={aiSearchLoading}
            >
              {aiSearchLoading ? 'Searching…' : '🔍 AI Search'}
            </button>
            {aiSearchIds && (
              <button
                type="button"
                className="btn btn-sm btn-secondary"
                onClick={clearAiSearch}
              >
                Clear
              </button>
            )}
          </div>
        </div>
      </div>

      {aiSearchError && (
        <div className="alert alert-error">
          {aiSearchError}
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="alert alert-error">
          {error}
          <button onClick={loadFiles} className="btn btn-sm">Retry</button>
        </div>
      )}

      {/* Files grid */}
      <div className="files-container">
        {(loading || isRefreshing) && filteredFiles.length === 0 ? (
          <div className="empty-state syncing-state">
            <div className="empty-icon">📁</div>
            <p className="syncing-indicator">
              <strong>Syncing files...</strong>
            </p>
          </div>
        ) : loading ? (
          <div className="loading-state">
            <div className="loading-spinner-large"></div>
            <p>Loading your files...</p>
          </div>
        ) : filteredFiles.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📁</div>
            <h3>No Files Yet</h3>
            <p>
              {filterType === 'all' 
                ? 'Upload your first encrypted file to get started!'
                : `No ${filterType} found. Try uploading some!`
              }
            </p>
            <button 
              onClick={() => setShowUploadForm(true)}
              className="btn btn-primary"
            >
              ⬆️ Upload Your First File
            </button>
          </div>
        ) : (
          <div className="files-grid">
            {filteredFiles.map(file => (
              <FileCard
                key={file.id}
                file={file}
                onDelete={handleDelete}
                onDecryptSuccess={handleDecryptSuccess}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default FileBrowser;

