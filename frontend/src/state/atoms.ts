import { atom } from 'recoil';

export interface User {
  username: string;
  email: string;
  roles: string[];
  uniqueKey?: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  uniqueKey: string | null;
  isLoading: boolean;
}

// Authentication state atom
export const authState = atom<AuthState>({
  key: 'authState',
  default: {
    isAuthenticated: false,
    user: null,
    uniqueKey: null,
    isLoading: false,
  },
});

// UI state atoms
export const showUniqueKeyModal = atom<boolean>({
  key: 'showUniqueKeyModal',
  default: false,
});

export const uniqueKeyMessage = atom<string>({
  key: 'uniqueKeyMessage',
  default: '',
});

// Journal entries state (for future use)
export interface JournalEntry {
  id: string;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export const journalEntriesState = atom<JournalEntry[]>({
  key: 'journalEntriesState',
  default: [],
});
