import apiClient from './apiClient';

export const authApi = {

  register: (data) =>
    apiClient.post('/api/v1/auth/register', data),

  login: (data) =>
    apiClient.post('/api/v1/auth/login', data),

  refresh: (refreshToken) =>
    apiClient.post('/api/v1/auth/refresh', { refreshToken }),

  logout: () =>
    apiClient.post('/api/v1/auth/logout'),

  getMe: () =>
    apiClient.get('/api/v1/users/me'),
};
