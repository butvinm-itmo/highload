import { UserDto, CreateUserRequest, UpdateUserRequest, PaginatedResponse } from '../types';
import apiClient from './client';

export const usersApi = {
  /**
   * Get paginated list of users
   */
  getUsers: async (page: number = 0, size: number = 20): Promise<PaginatedResponse<UserDto>> => {
    const response = await apiClient.get<UserDto[]>('/users', {
      params: { page, size },
    });
    const totalCount = parseInt(response.headers['x-total-count'] || '0', 10);
    return {
      data: response.data,
      totalCount,
    };
  },

  /**
   * Get single user by ID
   */
  getUser: async (id: string): Promise<UserDto> => {
    const response = await apiClient.get<UserDto>(`/users/${id}`);
    return response.data;
  },

  /**
   * Create new user (ADMIN only)
   */
  createUser: async (user: CreateUserRequest): Promise<UserDto> => {
    const response = await apiClient.post<UserDto>('/users', user);
    return response.data;
  },

  /**
   * Update user (ADMIN only)
   */
  updateUser: async (id: string, user: UpdateUserRequest): Promise<UserDto> => {
    const response = await apiClient.put<UserDto>(`/users/${id}`, user);
    return response.data;
  },

  /**
   * Delete user (ADMIN only)
   */
  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },
};
