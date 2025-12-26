import type { MouseEvent } from 'react';
import { useAuthenticatedImage } from '../hooks/useAuthenticatedImage';

interface AuthenticatedImageProps {
  src: string | null | undefined;
  alt: string;
  className?: string;
  onClick?: (e: MouseEvent<HTMLImageElement>) => void;
}

/**
 * Component that displays images from protected endpoints.
 * Automatically handles JWT authentication and blob URL conversion.
 */
export function AuthenticatedImage({ src, alt, className, onClick }: AuthenticatedImageProps) {
  const { blobUrl, loading, error } = useAuthenticatedImage(src);

  if (!src) {
    return null;
  }

  if (loading) {
    return (
      <div className={`flex items-center justify-center bg-gray-100 ${className || ''}`}>
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`flex items-center justify-center bg-red-50 border border-red-200 rounded ${className || ''}`}>
        <p className="text-sm text-red-600">Failed to load image</p>
      </div>
    );
  }

  if (!blobUrl) {
    return null;
  }

  return (
    <img
      src={blobUrl}
      alt={alt}
      className={className}
      onClick={onClick}
    />
  );
}
