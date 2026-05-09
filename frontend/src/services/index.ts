import { ApiClient } from './apiClient';
import { sessionStore } from './session';
import { createWeekendGoApi } from './weekendGoApi';

export const apiClient = new ApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  accessTokenProvider: () => sessionStore.token.value
});

export const weekendGoApi = createWeekendGoApi({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  accessTokenProvider: () => sessionStore.token.value
});

export { ApiClient, ApiError } from './apiClient';
export type { ApiClientOptions } from './apiClient';
export { sessionStore, createSessionStore } from './session';
export type { AuthSession, UserProfile as SessionUserProfile } from './session';
export { WeekendGoApi, createWeekendGoApi } from './weekendGoApi';
export type * from './weekendGoApi';
