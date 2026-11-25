import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { MOCK_CREDENTIALS } from '../data/mockAccounts.js';
import apiClient, { apiConfigured, apiErrorMessage, readAccessToken, writeAccessToken } from '../services/apiClient.js';
import { requestDataSync } from '../services/dataSync.js';

const AuthContext = createContext(null);
const MOCK_MODE = import.meta.env.VITE_USE_MOCK_DATA === 'true' && !apiConfigured;
const MOCK_SESSION = 'velnora-mock-session-v1';
const USER_SESSION = 'velnora-auth-user-v1';
const roleMap = { CUSTOMER: 'customer', SHOPKEEPER: 'shopkeeper', ADMIN: 'admin', customer: 'customer', shopkeeper: 'shopkeeper', admin: 'admin' };
function toUser(data) {
  return {
    id: data.userId ?? data.id,
    name: data.fullName ?? data.name ?? '',
    email: data.email ?? '',
    phone: data.phone ?? '',
    role: roleMap[data.role] || 'unknown',
    ...(data.shopId ? { shopId: String(data.shopId) } : {}),
  };
}
function readCachedUser() {
  try { return JSON.parse(sessionStorage.getItem(USER_SESSION) || 'null'); } catch { return null; }
}
function saveCachedUser(user) {
  try { if (user) sessionStorage.setItem(USER_SESSION, JSON.stringify(user)); else sessionStorage.removeItem(USER_SESSION); } catch { /* Session storage may be disabled. */ }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    if (MOCK_MODE) { try { return JSON.parse(sessionStorage.getItem(MOCK_SESSION) || 'null'); } catch { return null; } }
    return null;
  });
  const [loading, setLoading] = useState(Boolean(apiConfigured && readAccessToken()));
  const [authError, setAuthError] = useState('');
  const lastIdentity = useRef(null);
  const didSetIdentity = useRef(false);

  useEffect(() => {
    const identity = user ? `${user.id}:${user.role}` : null;
    if (!didSetIdentity.current) { didSetIdentity.current = true; lastIdentity.current = identity; return; }
    if (lastIdentity.current === identity) return;
    lastIdentity.current = identity;
    requestDataSync('all');
  }, [user?.id, user?.role]);

  useEffect(() => {
    // Remove credentials written by the previous mock registration flow. They are not used for real accounts.
    try { localStorage.removeItem('velnora-mock-users'); localStorage.removeItem('velnora-mock-session'); } catch { /* Storage can be unavailable. */ }
    if (!apiConfigured || !readAccessToken()) { setLoading(false); return undefined; }
    let alive = true;
    const cached = readCachedUser();
    const verify = async () => {
      try {
        if (cached?.role === 'admin') await apiClient.get('/admin/dashboard');
        else {
          const { data } = await apiClient.get('/profile');
          const profile = toUser(data);
          if (profile.role === 'unknown') throw new Error('The Velnora account role is not supported.');
          if (alive) { setUser({ ...cached, ...profile }); saveCachedUser({ ...cached, ...profile }); }
        }
        if (cached?.role === 'admin' && alive) setUser(cached);
        setAuthError('');
      } catch (error) {
        if (alive) {
          writeAccessToken(''); saveCachedUser(null); setUser(null);
          setAuthError(error.response?.status === 403 ? 'This account does not have access to that workspace.' : 'Your session has expired. Please sign in again.');
        }
      } finally { if (alive) setLoading(false); }
    };
    verify();
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    const expire = () => { writeAccessToken(''); saveCachedUser(null); setUser(null); setLoading(false); setAuthError('Your session has expired. Please sign in again.'); };
    window.addEventListener('velnora:session-expired', expire);
    return () => window.removeEventListener('velnora:session-expired', expire);
  }, []);

  const login = useCallback(async (email, password) => {
    setAuthError('');
    if (MOCK_MODE) {
      const account = MOCK_CREDENTIALS.find((item) => item.email.toLowerCase() === email.trim().toLowerCase() && item.password === password);
      if (!account) return { ok: false, error: 'That email and password do not match the enabled demo accounts.' };
      const next = toUser(account);
      try { sessionStorage.setItem(MOCK_SESSION, JSON.stringify(next)); } catch { /* Demo login remains available for this tab. */ }
      setUser(next);
      return { ok: true, user: next };
    }
    if (!apiConfigured) return { ok: false, error: 'The sign in service is not configured. Please contact the site administrator.' };
    try {
      const { data } = await apiClient.post('/auth/login', { email: email.trim(), password });
      const next = toUser(data);
      if (next.role === 'unknown') return { ok: false, error: 'This account has an unsupported Velnora role.' };
      writeAccessToken(data.accessToken);
      const account = next.role === 'admin' ? next : toUser((await apiClient.get('/profile')).data);
      if (account.role !== next.role) throw new Error('The account role could not be verified. Please sign in again.');
      saveCachedUser(account); setUser(account);
      return { ok: true, user: account };
    } catch (error) { writeAccessToken(''); saveCachedUser(null); setUser(null); return { ok: false, error: apiErrorMessage(error, 'We could not sign you in. Check your details and try again.') }; }
  }, []);

  const register = useCallback(async ({ name, email, phone = '', password, role = 'customer' }) => {
    setAuthError('');
    if (MOCK_MODE) return { ok: false, error: 'Registration is unavailable in demo mode. Use a configured Velnora account.' };
    if (!apiConfigured) return { ok: false, error: 'The registration service is not configured. Please contact the site administrator.' };
    try {
      const { data } = await apiClient.post('/auth/register', {
        fullName: name.trim(), email: email.trim(), phone: phone.trim() || null,
        password, role: role === 'shopkeeper' ? 'SHOPKEEPER' : 'CUSTOMER',
      });
      const next = toUser(data);
      if (!['customer', 'shopkeeper'].includes(next.role)) return { ok: false, error: 'This account type cannot be registered here.' };
      writeAccessToken(data.accessToken);
      const account = toUser((await apiClient.get('/profile')).data);
      if (account.role !== next.role) throw new Error('The account role could not be verified. Please sign in again.');
      saveCachedUser(account); setUser(account);
      return { ok: true, user: account };
    } catch (error) { writeAccessToken(''); saveCachedUser(null); setUser(null); return { ok: false, error: apiErrorMessage(error, 'We could not create your account. Please check the details and try again.') }; }
  }, []);

  const logout = useCallback(async () => {
    if (apiConfigured && readAccessToken()) {
      try { await apiClient.post('/account/logout'); } catch { /* Clear local credentials even if the token has expired. */ }
    }
    writeAccessToken(''); saveCachedUser(null);
    try { sessionStorage.removeItem(MOCK_SESSION); } catch { /* Storage can be unavailable. */ }
    setUser(null);
  }, []);

  const changePassword = useCallback(async (currentPassword, newPassword) => {
    if (!apiConfigured || MOCK_MODE) return { ok: false, error: 'Password updates are unavailable in demo mode.' };
    try {
      await apiClient.post('/account/password', { currentPassword, newPassword });
      writeAccessToken(''); saveCachedUser(null); setUser(null);
      return { ok: true };
    } catch (error) { return { ok: false, error: apiErrorMessage(error, 'We could not update your password.') }; }
  }, []);

  const updateProfile = useCallback(async ({ name, phone = '' }) => {
    if (!apiConfigured || MOCK_MODE) return { ok: false, error: 'Profile updates are unavailable in demo mode.' };
    try {
      const { data } = await apiClient.put('/profile', { fullName: name.trim(), phone: phone.trim() });
      const next = { ...user, ...toUser(data) };
      setUser(next); saveCachedUser(next);
      return { ok: true, user: next };
    } catch (error) { return { ok: false, error: apiErrorMessage(error, 'We could not save your profile.') }; }
  }, [user]);

  const value = useMemo(() => ({ user, users: [], loading, authError, isMock: MOCK_MODE, login, register, logout, changePassword, updateProfile }), [user, loading, authError, login, register, logout, changePassword, updateProfile]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
