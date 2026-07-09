import React, { createContext, useState, useEffect, useContext } from 'react';
import { apiFetch, setTokens, clearTokens } from '../api/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (token) {
      loadUser();
    } else {
      setLoading(false);
    }
  }, []);

  const loadUser = async () => {
    try {
      const resp = await apiFetch('/users/me');
      if (resp.ok) {
        const data = await resp.json();
        setUser(data);
        setRoles(data.roles || []);
        return data;
      } else {
        clearTokens();
      }
    } catch (e) {
      clearTokens();
    } finally {
      setLoading(false);
    }
  };

  const refreshUser = async () => {
    try {
      const resp = await apiFetch('/users/me');
      if (resp.ok) {
        const data = await resp.json();
        setUser(data);
        setRoles(data.roles || []);
        return data;
      }
    } catch (e) {
      // ignore
    }
  };

  const login = async (email, password) => {
    const resp = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    if (!resp.ok) {
      const err = await resp.json();
      throw new Error(err.message || 'Ошибка входа');
    }
    const data = await resp.json();
    setTokens(data.access_token, data.refresh_token);
    setUser(data.user);
    setRoles(data.user.roles || []);
    return data;
  };

  const register = async (email, password, fullName) => {
    const resp = await apiFetch('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, full_name: fullName || null }),
    });
    if (!resp.ok) {
      const err = await resp.json();
      throw new Error(err.message || 'Ошибка регистрации');
    }
    const data = await resp.json();
    setTokens(data.access_token, data.refresh_token);
    setUser(data.user);
    setRoles(data.user.roles || []);
    return data;
  };

  const logout = () => {
    clearTokens();
    setUser(null);
    setRoles([]);
  };

  const value = {
    user,
    roles,
    loading,
    login,
    register,
    logout,
    refreshUser,
    isAdmin: roles.includes('ADMIN'),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);