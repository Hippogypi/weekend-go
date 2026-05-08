import { ApiClient } from './apiClient';

export const apiClient = new ApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  accessTokenProvider: () => localStorage.getItem('weekend-go.access-token')
});

export { ApiClient, ApiError } from './apiClient';
export type { ApiClientOptions } from './apiClient';
