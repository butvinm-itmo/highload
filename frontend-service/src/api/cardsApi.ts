import { CardDto, LayoutTypeDto, PaginatedResponse } from '../types';
import apiClient from './client';

export const cardsApi = {
  /**
   * Get paginated list of cards
   */
  getCards: async (page: number = 0, size: number = 50): Promise<PaginatedResponse<CardDto>> => {
    const response = await apiClient.get<CardDto[]>('/cards', {
      params: { page, size },
    });
    const totalCount = parseInt(response.headers['x-total-count'] || '0', 10);
    return {
      data: response.data,
      totalCount,
    };
  },

  /**
   * Get random cards
   */
  getRandomCards: async (count: number): Promise<CardDto[]> => {
    const response = await apiClient.get<CardDto[]>('/cards/random', {
      params: { count },
    });
    return response.data;
  },

  /**
   * Get paginated list of layout types
   */
  getLayoutTypes: async (page: number = 0, size: number = 20): Promise<PaginatedResponse<LayoutTypeDto>> => {
    const response = await apiClient.get<LayoutTypeDto[]>('/layout-types', {
      params: { page, size },
    });
    const totalCount = parseInt(response.headers['x-total-count'] || '0', 10);
    return {
      data: response.data,
      totalCount,
    };
  },

  /**
   * Get single layout type by ID
   */
  getLayoutType: async (id: string): Promise<LayoutTypeDto> => {
    const response = await apiClient.get<LayoutTypeDto>(`/layout-types/${id}`);
    return response.data;
  },
};
