import type { LoginRequest, AuthTokenResponse } from '../types';
import apiClient from './client';

export const authApi = {
  /**
   * Login with username and password
   */
  login: async (credentials: LoginRequest): Promise<AuthTokenResponse> => {
    const response = await apiClient.post<AuthTokenResponse>('/auth/login', credentials);
    return response.data;
  },

  /**
   * Register a new user
   */
  register: async (credentials: LoginRequest): Promise<AuthTokenResponse> => {
    const response = await apiClient.post<AuthTokenResponse>('/auth/register', credentials);
    return response.data;
  },
};
