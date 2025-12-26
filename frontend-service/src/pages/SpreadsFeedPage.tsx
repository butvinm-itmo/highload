import { useState, useEffect, useRef } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import { Layout } from '../components/Layout';
import { SpreadCard } from '../components/SpreadCard';
import { CreateSpreadModal } from '../components/CreateSpreadModal';
import { CreateUserModal } from '../components/CreateUserModal';
import { EmptyState } from '../components/EmptyState';
import { SpreadCardSkeleton } from '../components/Skeleton';
import { spreadsApi } from '../api';
import { useAuth } from '../context/AuthContext';

export function SpreadsFeedPage() {
  const { hasRole } = useAuth();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isCreateUserModalOpen, setIsCreateUserModalOpen] = useState(false);
  const observerTarget = useRef<HTMLDivElement>(null);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    error,
  } = useInfiniteQuery({
    queryKey: ['spreads', 'scroll'],
    queryFn: ({ pageParam }) => spreadsApi.getSpreadScroll(pageParam, 20),
    getNextPageParam: (lastPage) => lastPage.afterCursor,
    initialPageParam: undefined as string | undefined,
  });

  // Infinite scroll observer
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.1 }
    );

    const currentTarget = observerTarget.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const allSpreads = data?.pages.flatMap((page) => page.data) || [];

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Tarot Spreads</h1>
          <div className="flex space-x-3">
            {hasRole('ADMIN') && (
              <button
                onClick={() => setIsCreateUserModalOpen(true)}
                className="px-4 py-2 bg-green-600 dark:bg-green-500 text-white rounded-md hover:bg-green-700 dark:hover:bg-green-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
              >
                Create User
              </button>
            )}
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Create Spread
            </button>
          </div>
        </div>

        {isLoading && (
          <div className="grid gap-4">
            {[...Array(5)].map((_, i) => (
              <SpreadCardSkeleton key={i} />
            ))}
          </div>
        )}

        {error && (
          <div className="p-4 bg-red-100 border border-red-400 text-red-700 rounded">
            Error loading spreads. Please try again later.
          </div>
        )}

        {!isLoading && !error && allSpreads.length === 0 && (
          <EmptyState
            title="No spreads yet"
            message="Create your first tarot spread to get started"
            actionLabel="Create Spread"
            onAction={() => setIsCreateModalOpen(true)}
            icon="spreads"
          />
        )}

        <div className="grid gap-4">
          {allSpreads.map((spread, index) => (
            <SpreadCard key={spread.id} spread={spread} delay={Math.min(index * 0.05, 0.5)} />
          ))}
        </div>

        {/* Infinite scroll trigger */}
        <div ref={observerTarget} className="py-4">
          {isFetchingNextPage && (
            <div className="grid gap-4">
              {[...Array(2)].map((_, i) => (
                <SpreadCardSkeleton key={i} />
              ))}
            </div>
          )}
          {!hasNextPage && allSpreads.length > 0 && (
            <div className="text-center text-gray-500 text-sm">
              You've reached the end
            </div>
          )}
        </div>
      </div>

      <CreateSpreadModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      <CreateUserModal
        isOpen={isCreateUserModalOpen}
        onClose={() => setIsCreateUserModalOpen(false)}
      />
    </Layout>
  );
}
