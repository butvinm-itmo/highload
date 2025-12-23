import {
  SpreadDto,
  SpreadSummaryDto,
  CreateSpreadRequest,
  PaginatedResponse,
  ScrollResponse
} from '../types';
import apiClient from './client';

export const spreadsApi = {
  /**
   * Get paginated list of spreads
   */
  getSpreads: async (page: number = 0, size: number = 20): Promise<PaginatedResponse<SpreadSummaryDto>> => {
    const response = await apiClient.get<SpreadSummaryDto[]>('/spreads', {
      params: { page, size },
    });
    const totalCount = parseInt(response.headers['x-total-count'] || '0', 10);
    return {
      data: response.data,
      totalCount,
    };
  },

  /**
   * Get spreads with cursor-based pagination (for infinite scroll)
   */
  getSpreadScroll: async (after?: string, size: number = 20): Promise<ScrollResponse<SpreadSummaryDto>> => {
    const params: Record<string, string | number> = { size };
    if (after) {
      params.after = after;
    }
    const response = await apiClient.get<SpreadSummaryDto[]>('/spreads/scroll', { params });
    const afterCursor = response.headers['x-after'];
    return {
      data: response.data,
      afterCursor,
    };
  },

  /**
   * Get single spread with full details
   */
  getSpread: async (id: string): Promise<SpreadDto> => {
    const response = await apiClient.get<SpreadDto>(`/spreads/${id}`);
    return response.data;
  },

  /**
   * Create new spread
   */
  createSpread: async (spread: CreateSpreadRequest): Promise<SpreadDto> => {
    const response = await apiClient.post<SpreadDto>('/spreads', spread);
    return response.data;
  },

  /**
   * Delete spread (author or ADMIN only)
   */
  deleteSpread: async (id: string): Promise<void> => {
    await apiClient.delete(`/spreads/${id}`);
  },
};
