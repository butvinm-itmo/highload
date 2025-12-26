import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { notificationsApi } from '../api/notificationsApi';
import { useNotifications } from '../context/NotificationContext';
import { Loading } from '../components/Loading';
import { EmptyState } from '../components/EmptyState';
import type { NotificationDto } from '../types';

export function NotificationsPage() {
  const [page, setPage] = useState(0);
  const pageSize = 20;
  const { markAsRead, markAllAsRead } = useNotifications();

  const { data, isLoading, error } = useQuery({
    queryKey: ['notifications', 'list', page],
    queryFn: () => notificationsApi.getNotifications(page, pageSize),
  });

  const handleMarkAsRead = async (notification: NotificationDto) => {
    if (!notification.isRead) {
      await markAsRead(notification.id);
    }
  };

  const handleMarkAllAsRead = async () => {
    if (confirm('Mark all notifications as read?')) {
      await markAllAsRead();
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'NEW_SPREAD':
        return (
          <svg className="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
          </svg>
        );
      case 'NEW_INTERPRETATION':
        return (
          <svg className="w-6 h-6 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
          </svg>
        );
      default:
        return (
          <svg className="w-6 h-6 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        );
    }
  };

  const getNotificationLink = (notification: NotificationDto): string => {
    // For SPREAD notifications, referenceId is the spread ID
    if (notification.referenceType === 'SPREAD') {
      return `/spreads/${notification.referenceId}`;
    }
    // For INTERPRETATION notifications, referenceId should be the spread ID
    // (backend stores spread ID in referenceId for interpretation events)
    if (notification.referenceType === 'INTERPRETATION') {
      return `/spreads/${notification.referenceId}`;
    }
    return '#';
  };

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
          Failed to load notifications. Please try again later.
        </div>
      </div>
    );
  }

  const notifications = data?.data || [];
  const totalCount = data?.totalCount || 0;
  const totalPages = Math.ceil(totalCount / pageSize);
  const hasUnread = notifications.some(n => !n.isRead);

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Notifications</h1>
        {hasUnread && (
          <button
            onClick={handleMarkAllAsRead}
            className="text-sm text-indigo-600 hover:text-indigo-800 font-medium"
          >
            Mark all as read
          </button>
        )}
      </div>

      {notifications.length === 0 ? (
        <EmptyState
          icon="bell"
          title="No notifications"
          message="You don't have any notifications yet"
        />
      ) : (
        <>
          <div className="space-y-2">
            {notifications.map((notification) => {
              const formattedDate = new Date(notification.createdAt).toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              });

              return (
                <Link
                  key={notification.id}
                  to={getNotificationLink(notification)}
                  onClick={() => handleMarkAsRead(notification)}
                  className={`block p-4 rounded-lg border transition-colors ${
                    notification.isRead
                      ? 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 hover:bg-gray-50'
                      : 'bg-indigo-50 border-indigo-200 hover:bg-indigo-100'
                  }`}
                >
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0 mt-1">
                      {getNotificationIcon(notification.type)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className={`text-sm font-medium ${notification.isRead ? 'text-gray-900 dark:text-gray-100' : 'text-indigo-900'}`}>
                          {notification.title}
                        </p>
                        <span className="text-xs text-gray-500 dark:text-gray-400 ml-2 flex-shrink-0">
                          {formattedDate}
                        </span>
                      </div>
                      <p className={`text-sm mt-1 ${notification.isRead ? 'text-gray-600 dark:text-gray-400' : 'text-indigo-700'}`}>
                        {notification.message}
                      </p>
                    </div>
                    {!notification.isRead && (
                      <div className="flex-shrink-0">
                        <span className="inline-block w-2 h-2 bg-indigo-600 rounded-full"></span>
                      </div>
                    )}
                  </div>
                </Link>
              );
            })}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-6 flex justify-center items-center space-x-2">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="text-sm text-gray-700 dark:text-gray-300">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
