import type {
  NotificationDto,
  UnreadCountResponse,
  MarkAllReadResponse,
  PaginatedResponse,
} from '../types';
import apiClient from './client';

const API_BASE_PATH = '/api/v0.0.1/notifications';

export const notificationsApi = {
  /**
   * Get all notifications for the current user
   */
  getNotifications: async (
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<NotificationDto>> => {
    const response = await apiClient.get<NotificationDto[]>(API_BASE_PATH, {
      params: { page, size },
    });
    const totalCount = parseInt(response.headers['x-total-count'] || '0', 10);
    return {
      data: response.data,
      totalCount,
    };
  },

  /**
   * Get unread notifications count
   */
  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get<UnreadCountResponse>(`${API_BASE_PATH}/unread-count`);
    return response.data.count;
  },

  /**
   * Mark a single notification as read
   */
  markAsRead: async (notificationId: string): Promise<void> => {
    await apiClient.patch(`${API_BASE_PATH}/${notificationId}/read`);
  },

  /**
   * Mark all notifications as read
   */
  markAllAsRead: async (): Promise<number> => {
    const response = await apiClient.post<MarkAllReadResponse>(`${API_BASE_PATH}/mark-all-read`);
    return response.data.markedAsRead;
  },
};
