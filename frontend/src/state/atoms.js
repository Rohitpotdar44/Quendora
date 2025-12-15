import { atom } from 'recoil';

// Recoil persistence effect
const localStorageEffect = (key) => ({ setSelf, onSet }) => {
  const savedValue = localStorage.getItem(key);
  if (savedValue != null) {
    setSelf(JSON.parse(savedValue));
  }

  onSet((newValue, _, isReset) => {
    if (isReset) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, JSON.stringify(newValue));
    }
  });
};

// Authentication state - persists across refresh
export const authState = atom({
  key: 'authState',
  default: {
    isAuthenticated: false,
    user: null,
    uniqueKey: null,
    isLoading: false,
  },
  effects: [localStorageEffect('auth_state')],
});

// Journal entries state - persists across refresh
export const journalEntriesState = atom({
  key: 'journalEntriesState',
  default: [],
  effects: [localStorageEffect('journal_entries')],
});

// UI state atoms (don't persist)
export const showUniqueKeyModal = atom({
  key: 'showUniqueKeyModal',
  default: false,
});

export const uniqueKeyMessage = atom({
  key: 'uniqueKeyMessage',
  default: '',
});

