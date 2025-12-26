import { useState, useEffect } from 'react';
import { apiClient } from '../api/client';

/**
 * Hook to fetch and display images from protected endpoints.
 * Converts the image to a blob URL that can be used in <img> tags.
 *
 * @param imageUrl - The protected image URL (or null/undefined if no image)
 * @returns Object containing the blob URL, loading state, and error state
 */
export function useAuthenticatedImage(imageUrl: string | null | undefined) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // Reset states when imageUrl changes
    setBlobUrl(null);
    setError(null);

    // If no imageUrl provided, don't fetch
    if (!imageUrl) {
      setLoading(false);
      return;
    }

    let isCancelled = false;
    let currentBlobUrl: string | null = null;

    const fetchImage = async () => {
      setLoading(true);
      setError(null);

      try {
        // Fetch image with JWT token via apiClient
        const response = await apiClient.get(imageUrl, {
          responseType: 'blob',
        });

        if (isCancelled) return;

        // Create blob URL from response
        const blob = response.data;
        const url = URL.createObjectURL(blob);
        currentBlobUrl = url;
        setBlobUrl(url);
      } catch (err) {
        if (isCancelled) return;
        setError(err instanceof Error ? err : new Error('Failed to load image'));
      } finally {
        if (!isCancelled) {
          setLoading(false);
        }
      }
    };

    fetchImage();

    // Cleanup function: revoke blob URL and cancel any pending operations
    return () => {
      isCancelled = true;
      if (currentBlobUrl) {
        URL.revokeObjectURL(currentBlobUrl);
      }
    };
  }, [imageUrl]);

  return { blobUrl, loading, error };
}
