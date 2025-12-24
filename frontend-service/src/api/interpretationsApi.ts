import type {
  InterpretationDto,
  CreateInterpretationRequest,
  UpdateInterpretationRequest,
  PaginatedResponse
} from '../types';
import apiClient from './client';

export const interpretationsApi = {
  /**
   * Get all interpretations for a spread
   */
  getInterpretations: async (
    spreadId: string,
    page: number = 0,
    size: number = 50
  ): Promise<PaginatedResponse<InterpretationDto>> => {
    const response = await apiClient.get<InterpretationDto[]>(
      `/spreads/${spreadId}/interpretations`,
      { params: { page, size } }
    );
    const totalCount = parseInt(response.headers['x-total-count'] || '0', 10);
    return {
      data: response.data,
      totalCount,
    };
  },

  /**
   * Create interpretation for a spread (MEDIUM/ADMIN only)
   */
  createInterpretation: async (
    spreadId: string,
    interpretation: CreateInterpretationRequest
  ): Promise<InterpretationDto> => {
    const response = await apiClient.post<InterpretationDto>(
      `/spreads/${spreadId}/interpretations`,
      interpretation
    );
    return response.data;
  },

  /**
   * Update interpretation (author or ADMIN only)
   */
  updateInterpretation: async (
    spreadId: string,
    interpretationId: string,
    interpretation: UpdateInterpretationRequest
  ): Promise<InterpretationDto> => {
    const response = await apiClient.put<InterpretationDto>(
      `/spreads/${spreadId}/interpretations/${interpretationId}`,
      interpretation
    );
    return response.data;
  },

  /**
   * Delete interpretation (author or ADMIN only)
   */
  deleteInterpretation: async (spreadId: string, interpretationId: string): Promise<void> => {
    await apiClient.delete(`/spreads/${spreadId}/interpretations/${interpretationId}`);
  },
};
