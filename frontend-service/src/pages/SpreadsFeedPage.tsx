import React, { useState, useEffect, useRef } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import { Layout } from '../components/Layout';
import { SpreadCard } from '../components/SpreadCard';
import { CreateSpreadModal } from '../components/CreateSpreadModal';
import { spreadsApi } from '../api';

export function SpreadsFeedPage() {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
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
          <h1 className="text-3xl font-bold text-gray-900">Tarot Spreads</h1>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            Create Spread
          </button>
        </div>

        {isLoading && (
          <div className="flex justify-center py-12">
            <div className="text-gray-600">Loading spreads...</div>
          </div>
        )}

        {error && (
          <div className="p-4 bg-red-100 border border-red-400 text-red-700 rounded">
            Error loading spreads. Please try again later.
          </div>
        )}

        {!isLoading && !error && allSpreads.length === 0 && (
          <div className="text-center py-12">
            <h3 className="text-lg font-medium text-gray-900 mb-2">No spreads yet</h3>
            <p className="text-gray-600 mb-4">Create your first tarot spread to get started</p>
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
            >
              Create Spread
            </button>
          </div>
        )}

        <div className="grid gap-4">
          {allSpreads.map((spread) => (
            <SpreadCard key={spread.id} spread={spread} />
          ))}
        </div>

        {/* Infinite scroll trigger */}
        <div ref={observerTarget} className="py-4">
          {isFetchingNextPage && (
            <div className="flex justify-center">
              <div className="text-gray-600">Loading more spreads...</div>
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
    </Layout>
  );
}
