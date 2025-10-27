import React from 'react';
import EntryCard from '../JournalEntry/EntryCard';
import './JournalEntryList.css';

const JournalEntryList = ({ entries }) => {
  if (!entries || entries.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">📝</div>
        <h3 className="empty-title">No Journal Entries Yet</h3>
        <p className="empty-description">
          Start your journaling journey by creating your first entry. 
          Click the "New Entry" button above to get started!
        </p>
        <div className="empty-tips">
          <h4>💡 Tips for Great Journal Entries:</h4>
          <ul>
            <li>Write about your daily experiences and reflections</li>
            <li>Include your thoughts, feelings, and observations</li>
            <li>Be honest and authentic with yourself</li>
            <li>Don't worry about perfect grammar or structure</li>
          </ul>
        </div>
      </div>
    );
  }

  return (
    <div className="journal-entry-list">
      <div className="entries-grid">
        {entries.map((entry, index) => (
          <EntryCard 
            key={entry.id || index} 
            entry={entry} 
          />
        ))}
      </div>
    </div>
  );
};

export default JournalEntryList;
