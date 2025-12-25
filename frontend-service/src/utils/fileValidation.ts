// File validation constants
export const ALLOWED_FILE_TYPES = ['image/png', 'image/jpeg', 'image/jpg'];
export const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB in bytes

// Validation errors
export class FileValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FileValidationError';
  }
}

/**
 * Validates a file for upload
 * @param file - File to validate
 * @throws FileValidationError if validation fails
 */
export function validateFile(file: File): void {
  // Check file type
  if (!ALLOWED_FILE_TYPES.includes(file.type)) {
    throw new FileValidationError(
      'Invalid file type. Only PNG and JPG images are allowed.'
    );
  }

  // Check file size
  if (file.size > MAX_FILE_SIZE) {
    throw new FileValidationError(
      `File size exceeds ${MAX_FILE_SIZE / 1024 / 1024}MB limit.`
    );
  }
}

/**
 * Formats file size in human-readable format
 * @param bytes - Size in bytes
 * @returns Formatted string (e.g., "1.5 MB")
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Gets file extension from filename
 * @param filename - Name of the file
 * @returns File extension (e.g., "png")
 */
export function getFileExtension(filename: string): string {
  return filename.split('.').pop()?.toLowerCase() || '';
}
