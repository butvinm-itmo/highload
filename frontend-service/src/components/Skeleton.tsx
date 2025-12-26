import { motion } from 'framer-motion';

interface SkeletonProps {
  className?: string;
  variant?: 'text' | 'circular' | 'rectangular' | 'card';
  width?: string | number;
  height?: string | number;
}

export function Skeleton({ className = '', variant = 'text', width, height }: SkeletonProps) {
  const variantClasses = {
    text: 'h-4 rounded',
    circular: 'rounded-full',
    rectangular: 'rounded-md',
    card: 'h-48 rounded-lg',
  };

  const style = {
    width: width || (variant === 'circular' ? '40px' : '100%'),
    height: height || (variant === 'text' ? '1rem' : variant === 'circular' ? '40px' : variant === 'card' ? '12rem' : '100%'),
  };

  return (
    <motion.div
      className={`bg-gradient-to-r from-gray-200 via-gray-300 to-gray-200 dark:from-gray-700 dark:via-gray-600 dark:to-gray-700 bg-[length:200%_100%] ${variantClasses[variant]} ${className}`}
      style={style}
      animate={{
        backgroundPosition: ['0% 0%', '100% 0%', '0% 0%'],
      }}
      transition={{
        duration: 1.5,
        repeat: Infinity,
        ease: 'linear',
      }}
    />
  );
}

export function SpreadCardSkeleton() {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
      <div className="flex justify-between items-start mb-3">
        <Skeleton className="flex-1" width="70%" height="1.5rem" />
        <Skeleton width="80px" height="1.5rem" className="ml-4" />
      </div>
      <div className="flex items-center space-x-4">
        <Skeleton width="100px" />
        <Skeleton width="80px" />
        <Skeleton width="120px" />
      </div>
      <Skeleton width="150px" className="mt-3" />
    </div>
  );
}

export function TarotCardSkeleton() {
  return (
    <div className="flex flex-col items-center">
      <Skeleton variant="card" width="8rem" height="12rem" />
      <Skeleton width="60px" className="mt-2" />
    </div>
  );
}

export function InterpretationSkeleton() {
  return (
    <div className="border-t border-gray-200 pt-4 mt-4">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center space-x-2">
          <Skeleton variant="circular" width="32px" height="32px" />
          <div>
            <Skeleton width="120px" />
            <Skeleton width="100px" height="0.75rem" className="mt-1" />
          </div>
        </div>
      </div>
      <Skeleton width="100%" className="mt-3" />
      <Skeleton width="90%" className="mt-2" />
      <Skeleton width="80%" className="mt-2" />
    </div>
  );
}

export function UserRowSkeleton() {
  return (
    <tr>
      <td className="px-6 py-4">
        <Skeleton width="180px" />
      </td>
      <td className="px-6 py-4">
        <Skeleton width="100px" />
      </td>
      <td className="px-6 py-4">
        <Skeleton width="150px" />
      </td>
      <td className="px-6 py-4">
        <div className="flex gap-2">
          <Skeleton width="60px" height="2rem" />
          <Skeleton width="60px" height="2rem" />
        </div>
      </td>
    </tr>
  );
}

export function NotificationSkeleton() {
  return (
    <div className="p-4 border-b border-gray-200">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <Skeleton width="200px" height="1.125rem" />
          <Skeleton width="100%" className="mt-2" />
          <Skeleton width="80%" className="mt-1" />
          <Skeleton width="120px" height="0.75rem" className="mt-2" />
        </div>
        <Skeleton variant="circular" width="12px" height="12px" className="ml-4" />
      </div>
    </div>
  );
}
