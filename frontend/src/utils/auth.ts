import { User } from '../state/atoms';

export const saveAuthData = (user: User, uniqueKey: string) => {
  localStorage.setItem('user', JSON.stringify(user));
  localStorage.setItem('uniqueKey', uniqueKey);
};

export const getAuthData = (): { user: User | null; uniqueKey: string | null } => {
  const userStr = localStorage.getItem('user');
  const uniqueKey = localStorage.getItem('uniqueKey');
  
  const user = userStr ? JSON.parse(userStr) : null;
  
  return { user, uniqueKey };
};

export const clearAuthData = () => {
  localStorage.removeItem('user');
  localStorage.removeItem('uniqueKey');
};

export const isAuthenticated = (): boolean => {
  const { user, uniqueKey } = getAuthData();
  return !!(user && uniqueKey);
};

export const getUser = (): User | null => {
  const { user } = getAuthData();
  return user;
};

export const getUniqueKey = (): string | null => {
  const { uniqueKey } = getAuthData();
  return uniqueKey;
};

export const isAdmin = (): boolean => {
  const user = getUser();
  return user?.roles?.includes('ADMIN') || false;
};
