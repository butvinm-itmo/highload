import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import type { FormEvent, ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { InterpretationDto } from '../types';
import { interpretationsApi } from '../api';
import { AuthenticatedImage } from './AuthenticatedImage';
import { getErrorMessage } from '../utils/errorHandling';
import { validateFile, formatFileSize } from '../utils/fileValidation';

interface EditInterpretationModalProps {
  interpretation: InterpretationDto;
  spreadId: string;
  isOpen: boolean;
  onClose: () => void;
}

export function EditInterpretationModal({
  interpretation,
  spreadId,
  isOpen,
  onClose,
}: EditInterpretationModalProps) {
  const [text, setText] = useState(interpretation.text);
  const [error, setError] = useState('');
  const [newFile, setNewFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);
  const [uploadingFile, setUploadingFile] = useState(false);
  const [deletingFile, setDeletingFile] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  useEffect(() => {
    setText(interpretation.text);
    setNewFile(null);
    setFilePreview(null);
  }, [interpretation.text, interpretation.id]);

  const updateMutation = useMutation({
    mutationFn: async (updatedText: string) => {
      // Step 1: Update text
      await interpretationsApi.updateInterpretation(spreadId, interpretation.id, { text: updatedText });

      // Step 2: Handle file if new file selected
      if (newFile) {
        setUploadingFile(true);
        try {
          await interpretationsApi.uploadFile(spreadId, interpretation.id, newFile);
        } catch (uploadError) {
          setUploadingFile(false);
          throw uploadError;
        }
        setUploadingFile(false);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spread', spreadId] });
      onClose();
    },
    onError: (err) => {
      setError(getErrorMessage(err));
    },
  });

  const deleteFileMutation = useMutation({
    mutationFn: () => interpretationsApi.deleteFile(spreadId, interpretation.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spread', spreadId] });
      setDeletingFile(false);
      onClose(); // Close modal after successful deletion
    },
    onError: (err) => {
      setError(getErrorMessage(err));
      setDeletingFile(false);
    },
  });

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    try {
      validateFile(selectedFile);
      setNewFile(selectedFile);
      setError('');

      const reader = new FileReader();
      reader.onloadend = () => {
        setFilePreview(reader.result as string);
      };
      reader.readAsDataURL(selectedFile);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid file');
      setNewFile(null);
      setFilePreview(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleRemoveNewFile = () => {
    setNewFile(null);
    setFilePreview(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleDeleteExistingFile = () => {
    if (confirm('Are you sure you want to delete this attachment?')) {
      setDeletingFile(true);
      deleteFileMutation.mutate();
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!text.trim()) {
      setError('Please enter your interpretation');
      return;
    }

    updateMutation.mutate(text.trim());
  };

  const handleClose = () => {
    setText(interpretation.text);
    setNewFile(null);
    setFilePreview(null);
    setError('');
    onClose();
  };

  const isLoading = updateMutation.isPending || uploadingFile || deletingFile;

  if (!isOpen) return null;

  const modalContent = (
    <div className="fixed inset-0 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.75)' }}>
      <div className="mystical-card max-w-2xl w-full p-8 animate-fade-in">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-3xl font-display font-bold text-gray-100">Edit Interpretation</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-mystic-300 transition-colors"
            disabled={updateMutation.isPending}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg border backdrop-blur-sm font-serif" style={{ backgroundColor: 'rgba(153, 27, 27, 0.2)', borderColor: 'rgba(239, 68, 68, 0.5)' }}>
            <div className="text-red-300">{error}</div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <textarea
            rows={6}
            value={text}
            onChange={(e) => setText(e.target.value)}
            className="w-full px-4 py-3 border rounded-lg backdrop-blur-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif text-gray-900"
            style={{ backgroundColor: 'rgba(229, 231, 235, 0.95)', borderColor: 'rgba(91, 33, 182, 0.3)' }}
            placeholder="Share your interpretation..."
            disabled={isLoading}
          />

          <div className="mt-5">
            <label className="block text-sm font-serif font-medium text-gray-300 mb-2">
              Attachment
            </label>

            {/* Show existing file */}
            {interpretation.fileUrl && !filePreview && (
              <div className="mb-4">
                <div className="flex items-center gap-4">
                  <AuthenticatedImage
                    src={interpretation.fileUrl}
                    alt="Current attachment"
                    className="max-w-xs max-h-32 rounded-lg border-2 border-mystic-600 shadow-mystic"
                  />
                  <button
                    type="button"
                    onClick={handleDeleteExistingFile}
                    className="px-4 py-2 text-sm font-serif text-red-300 border-2 rounded-lg hover:bg-red-900/30 transition-all duration-300 disabled:opacity-50"
                    style={{ borderColor: 'rgba(239, 68, 68, 0.5)' }}
                    disabled={isLoading}
                  >
                    {deletingFile ? 'Deleting...' : 'Delete'}
                  </button>
                </div>
              </div>
            )}

            {/* Show new file preview */}
            {filePreview && newFile && (
              <div className="mb-4 relative inline-block">
                <img
                  src={filePreview}
                  alt="New attachment"
                  className="max-w-xs max-h-32 rounded-lg border-2 border-mystic-600 shadow-mystic"
                />
                <button
                  type="button"
                  onClick={handleRemoveNewFile}
                  className="absolute top-2 right-2 bg-red-600 text-white rounded-full p-1.5 hover:bg-red-700 shadow-lg transition-all"
                  disabled={isLoading}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
                <p className="text-xs font-serif text-gray-400 mt-2">{formatFileSize(newFile.size)}</p>
              </div>
            )}

            {/* File input */}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/jpg"
              onChange={handleFileChange}
              className="block w-full text-sm font-serif text-gray-400 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:text-white file:transition-all file:cursor-pointer"
              disabled={isLoading}
            />
            <p className="mt-2 text-xs font-serif text-gray-500 italic">
              {interpretation.fileUrl && !newFile ? 'Select a new image to replace the existing one' : 'PNG or JPG, max 2MB'}
            </p>
          </div>

          <div className="flex space-x-4 pt-6">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-5 py-3 border-2 rounded-lg text-base font-serif font-medium text-gray-300 hover:bg-void-800/50 focus:outline-none focus:ring-2 focus:ring-mystic-500 transition-all disabled:opacity-50"
              style={{ borderColor: 'rgba(91, 33, 182, 0.5)' }}
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-5 py-3 rounded-lg shadow-mystic text-base font-serif font-medium text-white bg-gradient-to-r from-mystic-600 to-cosmic-600 hover:from-mystic-500 hover:to-cosmic-500 hover:shadow-cosmic focus:outline-none focus:ring-2 focus:ring-mystic-500 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed transition-all duration-300"
              disabled={isLoading}
            >
              {isLoading
                ? uploadingFile
                  ? 'Uploading file...'
                  : deletingFile
                  ? 'Deleting file...'
                  : 'Saving...'
                : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
}
