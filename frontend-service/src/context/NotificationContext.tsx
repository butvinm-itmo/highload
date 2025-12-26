import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import type { NotificationDto } from '../types';
import { notificationsApi } from '../api/notificationsApi';
import { NotificationWebSocket } from '../utils/websocket';
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

interface NotificationContextType {
  unreadCount: number;
  isConnected: boolean;
  markAsRead: (notificationId: string) => Promise<void>;
  markAllAsRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

interface NotificationProviderProps {
  children: ReactNode;
}

export function NotificationProvider({ children }: NotificationProviderProps) {
  const { token, isAuthenticated } = useAuth();
  const [ws, setWs] = useState<NotificationWebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const queryClient = useQueryClient();
  const { showInfo } = useToast();

  // Fetch unread count
  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.getUnreadCount,
    enabled: isAuthenticated,
    refetchInterval: 30000, // Refetch every 30 seconds as fallback
  });

  // Initialize WebSocket when authenticated
  useEffect(() => {
    if (!isAuthenticated || !token) {
      // Disconnect if not authenticated
      if (ws) {
        ws.disconnect();
        setWs(null);
        setIsConnected(false);
      }
      return;
    }

    // Create WebSocket instance
    const websocket = new NotificationWebSocket(() => token);

    // Handle incoming notifications
    const unsubscribeMessage = websocket.onMessage((notification: NotificationDto) => {
      console.log('Received notification:', notification);

      // Invalidate queries to refresh data
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
      queryClient.invalidateQueries({ queryKey: ['notifications', 'list'] });

      // Show toast notification
      showInfo(notification.title, notification.message);

      // Show browser notification if permission granted
      if (Notification.permission === 'granted') {
        new Notification(notification.title, {
          body: notification.message,
          icon: '/vite.svg',
          tag: notification.id,
        });
      }
    });

    const unsubscribeClose = websocket.onClose(() => {
      setIsConnected(false);
    });

    const unsubscribeError = websocket.onError((error) => {
      console.error('WebSocket error:', error);
      setIsConnected(false);
    });

    // Connect
    websocket.connect();
    setWs(websocket);

    // Check connection status periodically
    const intervalId = setInterval(() => {
      setIsConnected(websocket.isConnected());
    }, 1000);

    // Cleanup on unmount or auth change
    return () => {
      clearInterval(intervalId);
      unsubscribeMessage();
      unsubscribeClose();
      unsubscribeError();
      websocket.disconnect();
    };
  }, [isAuthenticated, token, queryClient]);

  const markAsRead = useCallback(async (notificationId: string) => {
    await notificationsApi.markAsRead(notificationId);
    queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    queryClient.invalidateQueries({ queryKey: ['notifications', 'list'] });
  }, [queryClient]);

  const markAllAsRead = useCallback(async () => {
    await notificationsApi.markAllAsRead();
    queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    queryClient.invalidateQueries({ queryKey: ['notifications', 'list'] });
  }, [queryClient]);

  const value: NotificationContextType = {
    unreadCount,
    isConnected,
    markAsRead,
    markAllAsRead,
  };

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications(): NotificationContextType {
  const context = useContext(NotificationContext);
  if (context === undefined) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
}
