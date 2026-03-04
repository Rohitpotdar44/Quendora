import axios from 'axios';
import BASE_URL from '../config/apiConfig';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add authorization token
api.interceptors.request.use(
  (config) => {
    // Try to get auth from localStorage
    const authState = localStorage.getItem('auth_state');
    if (authState) {
      try {
        const parsed = JSON.parse(authState);
        // ONLY use username for authentication (NEVER send uniqueKey as Bearer token)
        // uniqueKey is only for encryption/decryption, not authentication
        const token = parsed.user?.username;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch (error) {
        console.error('Error parsing auth state:', error);
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear auth data on 401
      localStorage.removeItem('auth_state');
      localStorage.removeItem('journal_entries');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API endpoints
export const authAPI = {
  // Login user
  login: (credentials) => api.post('/public/login', credentials),

  // Create new user
  createUser: (userData) => api.post('/public/createUser', userData),

  // Validate unique key
  validateUniqueKey: (uniqueKey) => 
    api.post('/public/validateUniqueKey', { uniqueKey }),

  // Regenerate unique key
  regenerateUniqueKey: (credentials) => 
    api.post('/public/regenerateUniqueKey', credentials),

  // Create admin (if needed)
  createAdmin: (adminData) => api.post('/public/createAdmin', adminData),

  // Forgot password flow
  requestResetCode: (userName) => api.post('/public/forgot-password/request', { userName }),
  verifyResetCode: (userName, code) => api.post('/public/forgot-password/verify', { userName, code }),
  resetPassword: (userName, code, newPassword) =>
    api.post('/public/forgot-password/reset', { userName, code, newPassword }),
};

// Journal API endpoints
export const journalAPI = {
  // Create new journal entry (with encryption)
  createEntry: (entryData) => api.post('/journalCopies', entryData),
  
  // Get all journal entries for current user
  getAllEntries: () => api.get('/journalCopies'),
  
  // Get journal entry by ID
  getEntryById: (id) => api.get(`/journalCopies/id/${id}`),
  
  // Update journal entry (with encryption)
  updateEntry: (id, entryData) => api.put(`/journalCopies/id/${id}`, entryData),
  
  // Delete journal entry (requires uniqueKey in request body)
  deleteEntry: (id, uniqueKey) => api.delete(`/journalCopies/id/${id}`, { data: { uniqueKey } }),
  
  // Decrypt journal entry content
  decryptEntry: (requestData) => api.post('/journalCopies/decrypt', requestData),
};

// Admin API endpoints
export const adminAPI = {
  // Get all users (admin only)
  getAllUsers: () => api.get('/admin/all-entries'),
};

// File API endpoints
export const fileAPI = {
  // Upload file with encryption (multipart/form-data)
  uploadFile: (formData) => api.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  
  // Get all user files metadata
  getAllFiles: () => api.get('/api/files'),
  
  // Decrypt only metadata (title, filename, size)
  decryptMetadata: (fileId, secretKey) =>
    api.post(`/api/files/metadata/${fileId}`, { secretKey }),
  
  // Decrypt and download file
  decryptFile: (fileId, secretKey) => 
    api.post(`/api/files/decrypt/${fileId}`, { secretKey }, {
      responseType: 'blob'
    }),
  
  // Delete file (requires secretKey in request body)
  deleteFile: (fileId, secretKey) => api.delete(`/api/files/${fileId}`, { data: { secretKey } }),

  // AI analyze file (summary, tags, OCR/search, captions, highlights)
  analyzeFile: (fileId, secretKey) =>
    api.post(`/api/files/ai/analyze/${fileId}`, { secretKey }),

  // AI search files by extracted content (requires secretKey)
  searchFiles: (query, secretKey) => api.post('/api/files/ai/search', { query, secretKey })
};

// AI API endpoints (uses backend proxy for security)
export const aiAPI = {
  // Rewrite title and content with AI
  rewrite: (title, content) => api.post('/api/ai/rewrite', { title, content }),
};

export default api;

