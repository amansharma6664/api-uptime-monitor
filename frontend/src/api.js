import axios from 'axios';

// Local: Vite proxy /api ko backend (8080) tak bhej deta hai.
// Deploy (Vercel/Netlify): VITE_API_URL = backend ka URL (jaise https://uptime-api.onrender.com)
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');
const api = axios.create({ baseURL: `${API_BASE}/api` });

// Har request me JWT token jodo
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Token expire / invalid ho to login page par bhej do (login request ko chhod kar)
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url || '';
    if (err.response?.status === 401 && !url.startsWith('/auth/login')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const errorMessage = (err) =>
  err.response?.data?.message || err.message || 'Something went wrong';

export default api;
