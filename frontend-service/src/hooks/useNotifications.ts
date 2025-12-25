import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '../api/notificationsApi';
import type { NotificationDto } from '../types';

/**
 * Fetch notifications with pagination
 */
export function useNotifications(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['notifications', page, size],
    queryFn: () => notificationsApi.getNotifications(page, size),
  });
}

/**
 * Fetch unread notifications count
 * Automatically refetches every 30 seconds
 */
export function useUnreadCount() {
  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationsApi.getUnreadCount(),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
}

/**
 * Mark a notification as read
 * Uses optimistic updates for instant UI feedback
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: string) => notificationsApi.markAsRead(notificationId),
    onMutate: async (notificationId) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['notifications'] });

      // Snapshot the previous value
      const previousNotifications = queryClient.getQueryData(['notifications']);

      // Optimistically update all notification queries
      queryClient.setQueriesData(
        { queryKey: ['notifications'] },
        (old: any) => {
          if (!old || !old.data) return old;
          return {
            ...old,
            data: old.data.map((notification: NotificationDto) =>
              notification.id === notificationId
                ? { ...notification, isRead: true }
                : notification
            ),
          };
        }
      );

      // Optimistically decrement unread count
      queryClient.setQueryData(['notifications', 'unread-count'], (old: number = 0) =>
        Math.max(0, old - 1)
      );

      return { previousNotifications };
    },
    onError: (_err, _notificationId, context) => {
      // Revert optimistic update on error
      if (context?.previousNotifications) {
        queryClient.setQueryData(['notifications'], context.previousNotifications);
      }
    },
    onSettled: () => {
      // Always refetch after mutation
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}

/**
 * Mark all notifications as read
 */
export function useMarkAllAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationsApi.markAllAsRead(),
    onSuccess: () => {
      // Invalidate all notification queries
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}
