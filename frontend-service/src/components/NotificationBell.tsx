import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useNotifications } from '../context/NotificationContext';
import { useEffect, useState } from 'react';

export function NotificationBell() {
  const { unreadCount } = useNotifications();
  const [prevCount, setPrevCount] = useState(unreadCount);
  const [shouldPulse, setShouldPulse] = useState(false);

  useEffect(() => {
    if (unreadCount > prevCount) {
      setShouldPulse(true);
      const timer = setTimeout(() => setShouldPulse(false), 1000);
      return () => clearTimeout(timer);
    }
    setPrevCount(unreadCount);
  }, [unreadCount, prevCount]);

  return (
    <Link
      to="/notifications"
      className="relative p-2 rounded-full hover:bg-gray-100 transition-colors"
      title={`${unreadCount} unread notifications`}
    >
      <motion.svg
        className="w-6 h-6 text-gray-600"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        animate={shouldPulse ? {
          scale: [1, 1.2, 1],
          rotate: [0, -15, 15, -15, 0],
        } : {}}
        transition={{ duration: 0.6 }}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </motion.svg>

      <AnimatePresence mode="wait">
        {unreadCount > 0 && (
          <motion.span
            key={unreadCount}
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.8, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 500, damping: 25 }}
            className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-600 rounded-full min-w-[20px]"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </motion.span>
        )}
      </AnimatePresence>
    </Link>
  );
}
