const API_BASE = process.env.REACT_APP_API_URL || '';

let accessToken = localStorage.getItem('access_token');
let refreshToken = localStorage.getItem('refresh_token');

export const setTokens = (at, rt) => {
  accessToken = at;
  refreshToken = rt;
  localStorage.setItem('access_token', at);
  localStorage.setItem('refresh_token', rt);
};

export const clearTokens = () => {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
};

export const apiFetch = async (path, options = {}) => {
  const url = `${API_BASE}${path}`;
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  let response = await fetch(url, { ...options, headers });
  
  if (response.status === 401) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      const newHeaders = {
        ...headers,
        Authorization: `Bearer ${accessToken}`,
      };
      response = await fetch(url, { ...options, headers: newHeaders });
      return response;
    } else {
      clearTokens();
      window.location.href = '/login';
      throw new Error('Сессия истекла, войдите заново.');
    }
  }
  return response;
};

async function refreshAccessToken() {
  if (!refreshToken) return false;
  try {
    const resp = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });
    if (resp.ok) {
      const data = await resp.json();
      setTokens(data.access_token, data.refresh_token);
      return true;
    }
  } catch (e) { /* ignore */ }
  return false;
}