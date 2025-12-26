import { useEffect, useState } from 'react';
import { preloadCardImages } from '../config/cardImages';

/**
 * Hook to preload all tarot card images for better performance
 * Preloading happens in the background and doesn't block rendering
 */
export function useCardImagePreload() {
  const [isPreloading, setIsPreloading] = useState(true);
  const [preloadError, setPreloadError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    preloadCardImages()
      .then(() => {
        if (!cancelled) {
          setIsPreloading(false);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setPreloadError(error);
          setIsPreloading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { isPreloading, preloadError };
}
