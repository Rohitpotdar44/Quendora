import React, { useState } from 'react';
import { useRecoilValue } from 'recoil';
import { authState } from '../../state/atoms';
import { fileAPI } from '../../services/api';
import ConfirmDialog from '../Common/ConfirmDialog';
import './FileCard.css';

const FileCard = ({ file, onDelete, onDecryptSuccess }) => {
  const auth = useRecoilValue(authState);
  const [showDecryptForm, setShowDecryptForm] = useState(false);
  const [uniqueKeyInput, setUniqueKeyInput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [decryptedMetadata, setDecryptedMetadata] = useState(null);
  const [decryptedTitle, setDecryptedTitle] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown date';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getFileIcon = (contentType) => {
    if (!contentType) return '📄';
    
    if (contentType.startsWith('image/')) return '🖼️';
    if (contentType.startsWith('video/')) return '🎥';
    if (contentType.includes('pdf')) return '📕';
    if (contentType.includes('word') || contentType.includes('document')) return '📝';
    if (contentType.includes('excel') || contentType.includes('spreadsheet')) return '📊';
    if (contentType.includes('powerpoint') || contentType.includes('presentation')) return '📽️';
    return '📄';
  };

  const getFileTypeLabel = (contentType) => {
    if (!contentType) return 'Unknown';
    
    if (contentType.startsWith('image/')) return 'Image';
    if (contentType.startsWith('video/')) return 'Video';
    if (contentType.includes('pdf')) return 'PDF';
    if (contentType.includes('word') || contentType.includes('document')) return 'Document';
    if (contentType.includes('excel') || contentType.includes('spreadsheet')) return 'Spreadsheet';
    if (contentType.includes('powerpoint') || contentType.includes('presentation')) return 'Presentation';
    return 'File';
  };

  const handleDecrypt = async () => {
    const keyToUse = uniqueKeyInput.trim() || auth.uniqueKey;
    
    if (!keyToUse) {
      setError('Please enter your unique key');
      return;
    }

    setLoading(true);
    setError('');
      
    try {
      console.log('[FileCard] Decrypting file:', file.id);
      
      // Decrypt the file (metadata will come from response headers)
      const fileResponse = await fileAPI.decryptFile(file.id, keyToUse);
      
      console.log('[FileCard] ✅ File decrypted successfully');
      console.log('[FileCard] All response headers:', fileResponse.headers);
      
      // Extract metadata from response headers
      const headers = fileResponse.headers;
      
      // Try custom headers (case-insensitive) - Backend sends these after restart
      let title = headers['x-decrypted-title'] || headers['X-Decrypted-Title'];
      let originalFileName = headers['x-decrypted-filename'] || headers['X-Decrypted-FileName'];
      
      console.log('[FileCard] Custom header - Title:', title);
      console.log('[FileCard] Custom header - Filename:', originalFileName);
      
      // Fallback: Extract from Content-Disposition header
      if (!originalFileName) {
        const contentDisposition = headers['content-disposition'];
        console.log('[FileCard] Content-Disposition:', contentDisposition);
        
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/i);
          if (filenameMatch && filenameMatch[1]) {
            originalFileName = filenameMatch[1].replace(/['"]/g, '').trim();
            if (!title) {
              title = originalFileName; // Use filename as title if no custom title
            }
          }
        }
      }
      
      // Final fallback
      if (!originalFileName) {
        originalFileName = `file_${file.id}`;
      }
      if (!title) {
        title = 'Decrypted File';
      }
      
      console.log('[FileCard] ✅ FINAL - Title to display:', title);
      console.log('[FileCard] ✅ FINAL - Filename:', originalFileName);

      // Create blob
      const blob = new Blob([fileResponse.data], { 
        type: file.contentType || 'application/octet-stream' 
      });
      
      const fileUrl = URL.createObjectURL(blob);
      
      // Determine file type and set metadata for preview
      let fileType = 'document';
      if (file.contentType?.startsWith('image/')) {
        fileType = 'image';
      } else if (file.contentType?.startsWith('video/')) {
        fileType = 'video';
      } else if (file.contentType?.includes('pdf')) {
        fileType = 'pdf';
      }
      
      setDecryptedMetadata({
        type: fileType,
        url: fileUrl,
        blob: blob,
        size: blob.size,
        contentType: file.contentType,
        fileName: originalFileName
      });
      
      // Set the decrypted title
      setDecryptedTitle(title || originalFileName || 'Decrypted File');
      
      setShowDecryptForm(false);
      setUniqueKeyInput('');
      
      if (onDecryptSuccess) {
        onDecryptSuccess(file.id);
      }
    } catch (err) {
      console.error('[FileCard] ❌ Decryption failed:', err);
      setError('Failed to decrypt. Please check your unique key.');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    if (!decryptedMetadata?.blob) return;
    
    const url = URL.createObjectURL(decryptedMetadata.blob);
    const link = document.createElement('a');
    link.href = url;
    const fileName = decryptedMetadata.fileName || decryptedTitle || `decrypted_file_${file.id}`;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.parentNode.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleView = () => {
    if (!decryptedMetadata?.url) return;
    
    // Open in new tab
    window.open(decryptedMetadata.url, '_blank');
  };

  const handleDelete = () => {
    setShowDeleteConfirm(true);
  };

  const confirmDelete = () => {
    setShowDeleteConfirm(false);
    onDelete(file.id);
  };

  const closePreview = () => {
    if (decryptedMetadata?.url) {
      URL.revokeObjectURL(decryptedMetadata.url);
    }
    setDecryptedMetadata(null);
    setDecryptedTitle('');
  };

  return (
    <>
      <div className="file-card">
        <div className="file-card-header">
          <div className="file-icon">{getFileIcon(file.contentType)}</div>
          <div className="file-type-badge">{getFileTypeLabel(file.contentType)}</div>
        </div>

        <div className="file-card-body">
          <h4 className="file-title">
            {decryptedMetadata 
              ? `✅ ${decryptedTitle}` 
              : file.plaintextTitle 
                ? `🔒 ${file.plaintextTitle}` 
                : '🔒 [Encrypted Title]'}
          </h4>
          <p className="file-meta">
            <span className="file-date">📅 {formatDate(file.createdAt)}</span>
          </p>
          <p className="file-meta">
            <span className="file-size">
              📦 {typeof file.displayFileSize === 'string' ? file.displayFileSize : 'Encrypted'}
              {file.isSensitive === true && <span className="sensitive-badge">🔐 Sensitive</span>}
            </span>
          </p>
        </div>

        {showDecryptForm && !decryptedMetadata && (
          <div className="decrypt-form">
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
                    handleDecrypt();
                  }
                }}
                autoFocus
              />
              {error && <div className="alert alert-error">{error}</div>}
              {loading && (
                <p className="decrypting-text">🔄 Decrypting...</p>
              )}
              <div className="decrypt-actions">
                <button 
                  onClick={() => {
                    setShowDecryptForm(false);
                    setError('');
                    setUniqueKeyInput('');
                  }}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

      {decryptedMetadata && (
        <div className="decrypted-preview">
          {decryptedMetadata.type === 'image' && (
            <div className="preview-container">
              <img src={decryptedMetadata.url} alt="Decrypted file" className="preview-image" />
            </div>
          )}
          
          {decryptedMetadata.type === 'video' && (
            <div className="preview-container">
              <video controls className="preview-video" src={decryptedMetadata.url}>
                Your browser does not support video playback.
              </video>
            </div>
          )}
          
          {decryptedMetadata.type === 'pdf' && (
            <div className="preview-info">
              <div className="preview-icon">📄</div>
              <p className="preview-text">Your PDF is ready</p>
            </div>
          )}
          
          {decryptedMetadata.type === 'document' && (
            <div className="preview-info">
              <div className="preview-icon">📝</div>
              <p className="preview-text">Your document is ready</p>
            </div>
          )}
          
          <div className="decrypted-actions">
            <button onClick={handleView} className="btn btn-primary btn-sm">
              👁️ View File
            </button>
            <button onClick={handleDownload} className="btn btn-secondary btn-sm">
              ⬇️ Download
            </button>
            <button onClick={closePreview} className="btn btn-secondary btn-sm">
              🔒 Lock Again
            </button>
          </div>
        </div>
      )}

        <div className="file-card-actions">
          {!decryptedMetadata ? (
            <button 
              onClick={() => {
                if (showDecryptForm) {
                  // Form is open, so perform decryption
                  handleDecrypt();
                } else {
                  // Form is closed, so open it
                  setShowDecryptForm(true);
                }
              }}
              className="action-btn decrypt-btn"
              title={showDecryptForm ? "Decrypt and view file" : "Decrypt file"}
              disabled={loading}
            >
              {loading ? '🔄 Decrypting...' : (showDecryptForm ? '🔓 Decrypt and View File' : '🔓 Decrypt')}
            </button>
          ) : (
            <button 
              onClick={() => {
                setShowDecryptForm(true);
                setDecryptedMetadata(null);
                setDecryptedTitle('');
              }}
              className="action-btn decrypt-btn"
              title="Decrypt again with new key"
            >
              🔓 Decrypt Again
            </button>
          )}
          <button 
            onClick={handleDelete}
            className="action-btn delete-btn"
            title="Delete file"
          >
            🗑️ Delete
          </button>
        </div>
      </div>

      <ConfirmDialog
        show={showDeleteConfirm}
        title="Delete File"
        message="Are you sure you want to delete this file? This action cannot be undone."
        onConfirm={confirmDelete}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </>
  );
};

export default FileCard;

