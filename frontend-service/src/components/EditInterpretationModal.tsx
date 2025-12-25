import { useState, useEffect, useRef } from 'react';
import type { FormEvent, ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { InterpretationDto } from '../types';
import { interpretationsApi } from '../api';
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

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-900">Edit Interpretation</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
            disabled={updateMutation.isPending}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <textarea
            rows={6}
            value={text}
            onChange={(e) => setText(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            placeholder="Share your interpretation..."
            disabled={isLoading}
          />

          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Attachment
            </label>

            {/* Show existing file */}
            {interpretation.fileUrl && !filePreview && (
              <div className="mb-3">
                <div className="flex items-center gap-3">
                  <img
                    src={interpretation.fileUrl}
                    alt="Current attachment"
                    className="max-w-xs max-h-32 rounded-lg border border-gray-300"
                  />
                  <button
                    type="button"
                    onClick={handleDeleteExistingFile}
                    className="text-sm text-red-600 hover:text-red-800 disabled:opacity-50"
                    disabled={isLoading}
                  >
                    {deletingFile ? 'Deleting...' : 'Delete'}
                  </button>
                </div>
              </div>
            )}

            {/* Show new file preview */}
            {filePreview && newFile && (
              <div className="mb-3 relative inline-block">
                <img
                  src={filePreview}
                  alt="New attachment"
                  className="max-w-xs max-h-32 rounded-lg border border-gray-300"
                />
                <button
                  type="button"
                  onClick={handleRemoveNewFile}
                  className="absolute top-2 right-2 bg-red-600 text-white rounded-full p-1 hover:bg-red-700"
                  disabled={isLoading}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
                <p className="text-xs text-gray-600 mt-1">{formatFileSize(newFile.size)}</p>
              </div>
            )}

            {/* File input */}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/jpg"
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
              disabled={isLoading}
            />
            <p className="mt-1 text-xs text-gray-500">
              {interpretation.fileUrl && !newFile ? 'Select a new image to replace the existing one' : 'PNG or JPG, max 2MB'}
            </p>
          </div>

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
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
}
