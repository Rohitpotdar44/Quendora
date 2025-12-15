import React, { useState } from 'react';
import { journalAPI, authAPI } from '../services/api';

const DebugPanel = () => {
  const [logs, setLogs] = useState([]);
  const [testResults, setTestResults] = useState({});

  const addLog = (message, type = 'info') => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs(prev => [...prev, { timestamp, message, type }]);
  };

  const testLocalStorage = () => {
    addLog('=== Testing localStorage ===', 'header');
    
    const authState = localStorage.getItem('auth_state');
    if (authState) {
      try {
        const parsed = JSON.parse(authState);
        addLog(`✅ Auth state found: ${JSON.stringify(parsed, null, 2)}`, 'success');
        setTestResults(prev => ({ ...prev, localStorage: 'PASS' }));
      } catch (error) {
        addLog(`❌ Error parsing auth state: ${error.message}`, 'error');
        setTestResults(prev => ({ ...prev, localStorage: 'FAIL' }));
      }
    } else {
      addLog('❌ No auth_state in localStorage', 'error');
      setTestResults(prev => ({ ...prev, localStorage: 'FAIL' }));
    }
  };

  const testBackendConnection = async () => {
    addLog('=== Testing Backend Connection ===', 'header');
    
    try {
      const authData = localStorage.getItem('auth_state');
      if (!authData) {
        addLog('❌ No auth data, cannot test', 'error');
        setTestResults(prev => ({ ...prev, backend: 'FAIL' }));
        return;
      }

      const parsed = JSON.parse(authData);
      addLog(`🔑 Using uniqueKey: ${parsed.uniqueKey?.substring(0, 10)}...`, 'info');

      // Test GET request
      addLog('📡 Sending GET request to /journalCopies...', 'info');
      const response = await journalAPI.getAllEntries();
      
      addLog(`✅ Backend responded: ${response.status} ${response.statusText}`, 'success');
      addLog(`📊 Entries count: ${response.data?.length || 0}`, 'info');
      addLog(`📄 Response data: ${JSON.stringify(response.data, null, 2)}`, 'info');
      
      setTestResults(prev => ({ ...prev, backend: 'PASS' }));
    } catch (error) {
      addLog(`❌ Backend error: ${error.message}`, 'error');
      if (error.response) {
        addLog(`❌ Status: ${error.response.status}`, 'error');
        addLog(`❌ Response: ${JSON.stringify(error.response.data)}`, 'error');
      }
      setTestResults(prev => ({ ...prev, backend: 'FAIL' }));
    }
  };

  const testCreateEntry = async () => {
    addLog('=== Testing Entry Creation ===', 'header');
    
    try {
      const authData = localStorage.getItem('auth_state');
      if (!authData) {
        addLog('❌ No auth data, cannot test', 'error');
        setTestResults(prev => ({ ...prev, create: 'FAIL' }));
        return;
      }

      const parsed = JSON.parse(authData);
      const testEntry = {
        entry: {
          title: `Test Entry ${new Date().toLocaleTimeString()}`,
          content: 'This is a test entry created from debug panel'
        },
        uniqueKey: parsed.uniqueKey
      };

      addLog(`📝 Creating entry: ${testEntry.entry.title}`, 'info');
      addLog(`📤 Request payload: ${JSON.stringify(testEntry, null, 2)}`, 'info');

      const response = await journalAPI.createEntry(testEntry);
      
      addLog(`✅ Entry created: ${response.status} ${response.statusText}`, 'success');
      addLog(`📄 Created entry: ${JSON.stringify(response.data, null, 2)}`, 'success');
      
      // Wait 1 second then fetch entries
      addLog('⏳ Waiting 1 second before fetching entries...', 'info');
      setTimeout(async () => {
        const fetchResponse = await journalAPI.getAllEntries();
        addLog(`📊 After creation, entries count: ${fetchResponse.data?.length || 0}`, 'info');
        addLog(`📄 All entries: ${JSON.stringify(fetchResponse.data, null, 2)}`, 'info');
      }, 1000);
      
      setTestResults(prev => ({ ...prev, create: 'PASS' }));
    } catch (error) {
      addLog(`❌ Create error: ${error.message}`, 'error');
      if (error.response) {
        addLog(`❌ Status: ${error.response.status}`, 'error');
        addLog(`❌ Response: ${JSON.stringify(error.response.data)}`, 'error');
      }
      setTestResults(prev => ({ ...prev, create: 'FAIL' }));
    }
  };

  const runAllTests = async () => {
    setLogs([]);
    setTestResults({});
    addLog('🚀 Starting comprehensive tests...', 'header');
    
    testLocalStorage();
    await new Promise(resolve => setTimeout(resolve, 500));
    
    await testBackendConnection();
    await new Promise(resolve => setTimeout(resolve, 500));
    
    await testCreateEntry();
    
    addLog('✨ All tests completed!', 'header');
  };

  const clearLogs = () => {
    setLogs([]);
    setTestResults({});
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'monospace', backgroundColor: '#1e1e1e', color: '#d4d4d4', minHeight: '100vh' }}>
      <h1 style={{ color: '#4ec9b0' }}>🔍 Debug Panel</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <button onClick={runAllTests} style={{ padding: '10px 20px', marginRight: '10px', backgroundColor: '#0e639c', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          🚀 Run All Tests
        </button>
        <button onClick={testLocalStorage} style={{ padding: '10px 20px', marginRight: '10px', backgroundColor: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          📦 Test localStorage
        </button>
        <button onClick={testBackendConnection} style={{ padding: '10px 20px', marginRight: '10px', backgroundColor: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          📡 Test Backend
        </button>
        <button onClick={testCreateEntry} style={{ padding: '10px 20px', marginRight: '10px', backgroundColor: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          📝 Test Create
        </button>
        <button onClick={clearLogs} style={{ padding: '10px 20px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          🗑️ Clear Logs
        </button>
      </div>

      <div style={{ marginBottom: '20px', display: 'flex', gap: '10px' }}>
        {Object.entries(testResults).map(([test, result]) => (
          <div key={test} style={{ padding: '10px', borderRadius: '4px', backgroundColor: result === 'PASS' ? '#28a745' : '#dc3545', color: 'white' }}>
            {test.toUpperCase()}: {result}
          </div>
        ))}
      </div>

      <div style={{ backgroundColor: '#252526', padding: '20px', borderRadius: '4px', maxHeight: '600px', overflowY: 'auto' }}>
        {logs.length === 0 ? (
          <div style={{ color: '#858585' }}>No logs yet. Click "Run All Tests" to start debugging.</div>
        ) : (
          logs.map((log, index) => (
            <div 
              key={index} 
              style={{ 
                marginBottom: '8px', 
                paddingBottom: '8px', 
                borderBottom: log.type === 'header' ? '2px solid #4ec9b0' : '1px solid #3e3e42',
                color: log.type === 'error' ? '#f48771' : log.type === 'success' ? '#4ec9b0' : log.type === 'header' ? '#dcdcaa' : '#d4d4d4',
                fontSize: log.type === 'header' ? '16px' : '14px',
                fontWeight: log.type === 'header' ? 'bold' : 'normal'
              }}
            >
              <span style={{ color: '#858585' }}>[{log.timestamp}]</span> {log.message}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default DebugPanel;

