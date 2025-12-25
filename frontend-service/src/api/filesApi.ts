import axios from './client';
import type { FileUploadResponse } from '../types';

const API_BASE_PATH = '/api/v0.0.1/files';

/**
 * Upload a file to the file storage service
 * @param file - File to upload
 * @param key - Storage key for the file
 * @returns Promise with the file URL
 */
export async function uploadFile(file: File, key: string): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post<FileUploadResponse>(
    `${API_BASE_PATH}?key=${encodeURIComponent(key)}`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data.url;
}

/**
 * Delete a file from the file storage service
 * @param key - Storage key of the file to delete
 */
export async function deleteFile(key: string): Promise<void> {
  await axios.delete(`${API_BASE_PATH}?key=${encodeURIComponent(key)}`);
}

/**
 * Get the download URL for a file
 * @param key - Storage key of the file
 * @returns Full download URL
 */
export function getFileDownloadUrl(key: string): string {
  const baseUrl = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';
  return `${baseUrl}${API_BASE_PATH}/${encodeURIComponent(key)}`;
}

/**
 * Generate a unique file key for an interpretation attachment
 * @param interpretationId - ID of the interpretation
 * @param filename - Original filename
 * @returns Generated key for storage
 */
export function generateInterpretationFileKey(
  interpretationId: string,
  filename: string
): string {
  return `interpretations/${interpretationId}/${filename}`;
}
