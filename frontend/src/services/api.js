import axios from 'axios';

const API_BASE_URL = 'http://localhost:8087';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add authentication
api.interceptors.request.use(
  (config) => {
    // Try to get JWT token first, then fall back to unique key
    const token = localStorage.getItem('token');
    const uniqueKey = localStorage.getItem('uniqueKey');
    
    console.log('API Request - Token:', token);
    console.log('API Request - Unique Key:', uniqueKey);
    console.log('API Request - URL:', config.url);
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    } else if (uniqueKey) {
      // For now, use the unique key as a simple token
      config.headers.Authorization = `Bearer ${uniqueKey}`;
    }
    
    console.log('API Request - Authorization Header:', config.headers.Authorization);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('uniqueKey');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// API methods
export const authAPI = {
  // Create new user
  createUser: (userData) => api.post('/public/createUser', userData),

  // Login user
  login: (credentials) => api.post('/public/login', credentials),

  // Validate unique key
  validateUniqueKey: (uniqueKey) => 
    api.post('/public/validateUniqueKey', { uniqueKey }),

  // Regenerate unique key
  regenerateUniqueKey: (credentials) => 
    api.post('/public/regenerateUniqueKey', credentials),

  // Create admin (if needed)
  createAdmin: (adminData) => api.post('/public/createAdmin', adminData),
};

export const journalAPI = {
  // Create new journal entry
  createEntry: (entryData) => api.post('/journalCopies', entryData),
  
  // Get all journal entries for current user
  getAllEntries: () => api.get('/journalCopies'),
  
  // Get journal entry by ID
  getEntryById: (id) => api.get(`/journalCopies/id/${id}`),
  
  // Update journal entry
  updateEntry: (id, entryData) => api.put(`/journalCopies/id/${id}`, entryData),
  
  // Delete journal entry
  deleteEntry: (id) => api.delete(`/journalCopies/id/${id}`),
  
  // Decrypt journal entry content
  decryptEntry: (requestData) => api.post('/journalCopies/decrypt', requestData),
};

export const adminAPI = {
  // Get all users (admin only)
  getAllUsers: () => api.get('/admin/all-entries'),
};

export default api;