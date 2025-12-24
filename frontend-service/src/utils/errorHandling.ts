import { AxiosError } from 'axios';
import type { ApiError } from '../types';

/**
 * Extract error message from API error response
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const apiError = error.response?.data as ApiError & { fieldErrors?: Record<string, string> };

    // If there are field-specific errors, format them nicely
    if (apiError?.fieldErrors) {
      const fieldMessages = Object.entries(apiError.fieldErrors)
        .map(([field, message]) => `${field}: ${message}`)
        .join('; ');
      return fieldMessages || apiError?.message || 'Validation failed';
    }

    return apiError?.message || error.message || 'An unexpected error occurred';
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred';
}

/**
 * Check if error is authorization error
 */
export function isAuthError(error: unknown): boolean {
  if (error instanceof AxiosError) {
    return error.response?.status === 401 || error.response?.status === 403;
  }
  return false;
}

/**
 * Check if error is validation error
 */
export function isValidationError(error: unknown): boolean {
  if (error instanceof AxiosError) {
    return error.response?.status === 400;
  }
  return false;
}
