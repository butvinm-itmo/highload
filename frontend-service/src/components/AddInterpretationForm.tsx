import { useState, useRef } from 'react';
import type { FormEvent, ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { interpretationsApi } from '../api';
import { getErrorMessage } from '../utils/errorHandling';
import { validateFile, formatFileSize } from '../utils/fileValidation';

interface AddInterpretationFormProps {
  spreadId: string;
}

export function AddInterpretationForm({ spreadId }: AddInterpretationFormProps) {
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);
  const [uploadingFile, setUploadingFile] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: async (interpretationText: string) => {
      // Step 1: Create interpretation
      const interpretation = await interpretationsApi.createInterpretation(spreadId, {
        text: interpretationText
      });

      // Step 2: If file is attached, upload it
      if (file) {
        setUploadingFile(true);
        try {
          await interpretationsApi.uploadFile(spreadId, interpretation.id, file);
        } catch (uploadError) {
          setUploadingFile(false);
          throw uploadError;
        }
        setUploadingFile(false);
      }

      return interpretation;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spread', spreadId] });
      setText('');
      setFile(null);
      setFilePreview(null);
      setError('');
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    },
    onError: (err) => {
      setError(getErrorMessage(err));
    },
  });

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    try {
      validateFile(selectedFile);
      setFile(selectedFile);
      setError('');

      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setFilePreview(reader.result as string);
      };
      reader.readAsDataURL(selectedFile);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid file');
      setFile(null);
      setFilePreview(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleRemoveFile = () => {
    setFile(null);
    setFilePreview(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!text.trim()) {
      setError('Please enter your interpretation');
      return;
    }

    createMutation.mutate(text.trim());
  };

  const isLoading = createMutation.isPending || uploadingFile;

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <h3 className="text-lg font-semibold text-gray-900 mb-3">Add Your Interpretation</h3>

      {error && (
        <div className="mb-3 p-2 bg-red-100 border border-red-400 text-red-700 rounded text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <textarea
          rows={4}
          value={text}
          onChange={(e) => setText(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
          placeholder="Share your interpretation of this spread..."
          disabled={isLoading}
        />

        <div className="mt-3">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Attach Image (optional)
          </label>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/png,image/jpeg,image/jpg"
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
            disabled={isLoading}
          />
          <p className="mt-1 text-xs text-gray-500">
            PNG or JPG, max 2MB
          </p>

          {filePreview && file && (
            <div className="mt-3 relative inline-block">
              <img
                src={filePreview}
                alt="Preview"
                className="max-w-xs max-h-48 rounded-lg border border-gray-300"
              />
              <button
                type="button"
                onClick={handleRemoveFile}
                className="absolute top-2 right-2 bg-red-600 text-white rounded-full p-1 hover:bg-red-700"
                disabled={isLoading}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              <p className="text-xs text-gray-600 mt-1">{formatFileSize(file.size)}</p>
            </div>
          )}
        </div>

        <div className="mt-3 flex justify-end">
          <button
            type="submit"
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
            disabled={isLoading}
          >
            {isLoading
              ? uploadingFile
                ? 'Uploading file...'
                : 'Adding...'
              : 'Add Interpretation'}
          </button>
        </div>
      </form>
    </div>
  );
}
