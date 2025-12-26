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
    <div className="mystical-card p-6">
      <h3 className="text-xl font-display font-semibold text-gray-100 mb-4">Add Your Interpretation</h3>

      {error && (
        <div className="mb-4 p-3 rounded-lg border backdrop-blur-sm font-serif" style={{ backgroundColor: 'rgba(153, 27, 27, 0.2)', borderColor: 'rgba(239, 68, 68, 0.5)' }}>
          <div className="text-red-300">{error}</div>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <textarea
          rows={4}
          value={text}
          onChange={(e) => setText(e.target.value)}
          className="w-full px-4 py-3 border rounded-lg backdrop-blur-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif text-gray-900"
          style={{ backgroundColor: 'rgba(229, 231, 235, 0.95)', borderColor: 'rgba(91, 33, 182, 0.3)' }}
          placeholder="Share your interpretation of this spread..."
          disabled={isLoading}
        />

        <div className="mt-4">
          <label className="block text-sm font-serif font-medium text-gray-300 mb-2">
            Attach Image (optional)
          </label>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/png,image/jpeg,image/jpg"
            onChange={handleFileChange}
            className="block w-full text-sm font-serif text-gray-400 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:text-white hover:file:from-mystic-500 hover:file:to-cosmic-500 file:transition-all file:cursor-pointer"
            style={{ '--file-bg': 'linear-gradient(to right, rgba(124, 58, 237, 1), rgba(192, 38, 211, 1))' } as any}
            disabled={isLoading}
          />
          <p className="mt-2 text-xs font-serif text-gray-500 italic">
            PNG or JPG, max 2MB
          </p>

          {filePreview && file && (
            <div className="mt-4 relative inline-block">
              <img
                src={filePreview}
                alt="Preview"
                className="max-w-xs max-h-48 rounded-lg border-2 border-mystic-600 shadow-mystic"
              />
              <button
                type="button"
                onClick={handleRemoveFile}
                className="absolute top-2 right-2 bg-red-600 text-white rounded-full p-1.5 hover:bg-red-700 shadow-lg transition-all"
                disabled={isLoading}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              <p className="text-xs font-serif text-gray-400 mt-2">{formatFileSize(file.size)}</p>
            </div>
          )}
        </div>

        <div className="mt-5 flex justify-end">
          <button
            type="submit"
            className="px-6 py-3 bg-gradient-to-r from-mystic-600 to-cosmic-600 text-white rounded-lg shadow-mystic hover:from-mystic-500 hover:to-cosmic-500 hover:shadow-cosmic focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-void-950 focus:ring-mystic-500 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed transition-all duration-300 font-serif font-medium"
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
