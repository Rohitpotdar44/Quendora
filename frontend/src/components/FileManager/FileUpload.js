import React, { useState, useRef } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { fileAPI } from '../../services/api';
import './FileUpload.css';

const FileUpload = ({ onUploadSuccess, onCancel }) => {
  const auth = useRecoilValue(authState);
  const [file, setFile] = useState(null);
  const [title, setTitle] = useState('');
  const [uniqueKey, setUniqueKey] = useState('');
  const [isSensitive, setIsSensitive] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [uploadProgress, setUploadProgress] = useState(0);
  const fileInputRef = useRef(null);

  const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB for all files
  const MAX_VIDEO_SIZE = 10 * 1024 * 1024; // 10 MB for videos

  const handleDragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const droppedFiles = e.dataTransfer.files;
    if (droppedFiles.length > 0) {
      handleFileSelect(droppedFiles[0]);
    }
  };

  const handleFileSelect = (selectedFile) => {
    setError('');

    if (!selectedFile) {
      return;
    }

    // Check file size
    const isVideo = selectedFile.type.startsWith('video/');
    const maxSize = isVideo ? MAX_VIDEO_SIZE : MAX_FILE_SIZE;
    const maxSizeMB = isVideo ? 10 : 20;

    if (selectedFile.size > maxSize) {
      setError(`File size exceeds ${maxSizeMB} MB limit for ${isVideo ? 'videos' : 'files'}`);
      return;
    }

    setFile(selectedFile);
    
    // Auto-fill title with filename (without extension)
    if (!title) {
      const fileName = selectedFile.name;
      const nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.')) || fileName;
      setTitle(nameWithoutExt);
    }
  };

  const handleFileInputChange = (e) => {
    if (e.target.files.length > 0) {
      handleFileSelect(e.target.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!file) {
      setError('Please select a file to upload');
      return;
    }

    if (!title.trim()) {
      setError('Please provide a title for the file');
      return;
    }

    if (!uniqueKey.trim()) {
      setError('Please enter your unique key to encrypt the file');
      return;
    }

    setLoading(true);
    setUploadProgress(0);

    try {
      console.log('[FileUpload] Preparing upload...');
      console.log('[FileUpload] File:', file.name, 'Size:', file.size, 'Type:', file.type);
      console.log('[FileUpload] Title:', title);

      // Create FormData
      const formData = new FormData();
      formData.append('file', file);
      formData.append('title', title.trim());
      formData.append('secretKey', uniqueKey.trim());
      formData.append('isSensitive', isSensitive);

      // Simulate progress for better UX
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 200);

      const response = await fileAPI.uploadFile(formData);

      clearInterval(progressInterval);
      setUploadProgress(100);

      console.log('[FileUpload] ✅ Upload successful:', response.data);

      // Reset form
      setFile(null);
      setTitle('');
      setUniqueKey('');
      setIsSensitive(false);
      setUploadProgress(0);
      
      // Notify parent component
      if (onUploadSuccess) {
        onUploadSuccess(response.data);
      }
    } catch (err) {
      console.error('[FileUpload] ❌ Upload failed:', err);
      setError(err.response?.data || 'Failed to upload file. Please try again.');
      setUploadProgress(0);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setFile(null);
    setTitle('');
    setUniqueKey('');
    setIsSensitive(false);
    setError('');
    setUploadProgress(0);
    if (onCancel) {
      onCancel();
    }
  };

  const getFileIcon = () => {
    if (!file) return '📄';
    
    const type = file.type;
    if (type.startsWith('image/')) return '🖼️';
    if (type.startsWith('video/')) return '🎥';
    if (type.includes('pdf')) return '📕';
    if (type.includes('word') || type.includes('document')) return '📝';
    if (type.includes('excel') || type.includes('spreadsheet')) return '📊';
    if (type.includes('powerpoint') || type.includes('presentation')) return '📽️';
    return '📄';
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  return (
    <div className="file-upload">
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">⬆️ Upload File</h3>
          <p className="card-subtitle">Your file will be encrypted with your unique key</p>
        </div>

        <form onSubmit={handleSubmit} className="file-upload-form">
          {error && <div className="alert alert-error">{error}</div>}

          {/* Drag-drop zone */}
          <div
            className={`drop-zone ${isDragging ? 'dragging' : ''} ${file ? 'has-file' : ''}`}
            onDragEnter={handleDragEnter}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              onChange={handleFileInputChange}
              style={{ display: 'none' }}
              accept="image/*,video/*,.pdf,.doc,.docx,.txt"
            />

            {file ? (
              <div className="file-preview">
                <div className="file-icon-large">{getFileIcon()}</div>
                <div className="file-info">
                  <p className="file-name">{file.name}</p>
                  <p className="file-size">{formatFileSize(file.size)}</p>
                  <p className="file-type">{file.type || 'Unknown type'}</p>
                </div>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setFile(null);
                    setTitle('');
                  }}
                  className="btn-remove-file"
                >
                  ✕
                </button>
              </div>
            ) : (
              <div className="drop-zone-content">
                <div className="drop-icon">📁</div>
                <p className="drop-text">Drag & drop a file here</p>
                <p className="drop-subtext">or click to browse</p>
                <p className="file-limits">
                  Max size: 20 MB (10 MB for videos)
                </p>
              </div>
            )}
          </div>

          {/* Title input */}
          {file && (
            <>
              <div className="form-group">
                <label className="form-label">File Title</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="form-input"
                  placeholder="Enter a title for this file..."
                  maxLength={100}
                  required
                />
                <small className="char-count">{title.length}/100 characters</small>
              </div>

              <div className="form-group">
                <label className="form-label">🔑 Unique Key (Required for Encryption)</label>
                <input
                  type="password"
                  value={uniqueKey}
                  onChange={(e) => setUniqueKey(e.target.value)}
                  className="form-input"
                  placeholder="Enter your unique key to encrypt this file..."
                  required
                />
                <small className="form-hint">
                  ⚠️ Your file will be encrypted with this key. You'll need it to decrypt later.
                </small>
              </div>

              {/* Sensitivity checkbox */}
              <div className="form-group-checkbox">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={isSensitive}
                    onChange={(e) => setIsSensitive(e.target.checked)}
                    className="checkbox-input"
                  />
                  <span className="checkbox-text">
                    🔒 <strong>Contains sensitive information</strong>
                    <small className="checkbox-hint">
                      (If checked, title will be encrypted too - maximum security)
                    </small>
                  </span>
                </label>
              </div>

              {/* Upload progress */}
              {loading && uploadProgress > 0 && (
                <div className="upload-progress">
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{ width: `${uploadProgress}%` }}
                    ></div>
                  </div>
                  <p className="progress-text">
                    {uploadProgress < 100 ? `Uploading... ${uploadProgress}%` : 'Processing...'}
                  </p>
                </div>
              )}
            </>
          )}

          {/* Action buttons */}
          <div className="form-actions">
            <button
              type="button"
              onClick={handleCancel}
              className="btn btn-secondary"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || !file || !title.trim() || !uniqueKey.trim()}
            >
              {loading && <span className="loading-spinner"></span>}
              {loading ? 'Uploading...' : '🔒 Encrypt & Upload File'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default FileUpload;

