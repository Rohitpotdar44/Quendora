import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { RecoilRoot } from 'recoil';
import LoginForm from './components/Login/LoginForm';
import Dashboard from './components/Dashboard/Dashboard';
import DebugPanel from './components/DebugPanel';
import './App.css';

// Protected Route Component
const ProtectedRoute = ({ children }) => {
  const authData = localStorage.getItem('auth_state');
  
  if (!authData) {
    return <Navigate to="/login" />;
  }
  
  try {
    const parsed = JSON.parse(authData);
    const isAuthenticated = parsed.isAuthenticated === true;
    return isAuthenticated ? children : <Navigate to="/login" />;
  } catch (error) {
    // Invalid auth data, clear it and redirect
    localStorage.removeItem('auth_state');
    return <Navigate to="/login" />;
  }
};

function App() {
  return (
    <RecoilRoot>
      <Router>
        <div className="App">
          <Routes>
            <Route 
              path="/login" 
              element={<LoginForm />} 
            />
            <Route 
              path="/dashboard" 
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/debug" 
              element={<DebugPanel />} 
            />
            <Route path="/" element={<Navigate to="/login" />} />
          </Routes>
        </div>
      </Router>
    </RecoilRoot>
  );
}

export default App;

