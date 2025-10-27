export const saveAuthData = (user, uniqueKey) => {
  localStorage.setItem('user', JSON.stringify(user));
  localStorage.setItem('uniqueKey', uniqueKey);
};

export const getAuthData = () => {
  const userStr = localStorage.getItem('user');
  const uniqueKey = localStorage.getItem('uniqueKey');
  
  const user = userStr ? JSON.parse(userStr) : null;
  
  return { user, uniqueKey };
};

export const clearAuthData = () => {
  localStorage.removeItem('user');
  localStorage.removeItem('uniqueKey');
};

export const isAuthenticated = () => {
  const { user, uniqueKey } = getAuthData();
  return !!(user && uniqueKey);
};

export const getUser = () => {
  const { user } = getAuthData();
  return user;
};

export const getUniqueKey = () => {
  const { uniqueKey } = getAuthData();
  return uniqueKey;
};

export const isAdmin = () => {
  const user = getUser();
  return user?.roles?.includes('ADMIN') || false;
};