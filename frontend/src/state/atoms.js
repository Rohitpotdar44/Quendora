import { atom } from 'recoil';

// Authentication state atom
export const authState = atom({
  key: 'authState',
  default: {
    isAuthenticated: false,
    user: null,
    uniqueKey: null,
    isLoading: false,
  },
});

// UI state atoms
export const showUniqueKeyModal = atom({
  key: 'showUniqueKeyModal',
  default: false,
});

export const uniqueKeyMessage = atom({
  key: 'uniqueKeyMessage',
  default: '',
});

// Journal entries state (for future use)
export const journalEntriesState = atom({
  key: 'journalEntriesState',
  default: [],
});