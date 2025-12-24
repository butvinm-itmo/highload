import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { InterpretationDto } from '../types';
import { interpretationsApi } from '../api';
import { getErrorMessage } from '../utils/errorHandling';

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
  const queryClient = useQueryClient();

  useEffect(() => {
    setText(interpretation.text);
  }, [interpretation.text]);

  const updateMutation = useMutation({
    mutationFn: (updatedText: string) =>
      interpretationsApi.updateInterpretation(spreadId, interpretation.id, { text: updatedText }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spread', spreadId] });
      onClose();
    },
    onError: (err) => {
      setError(getErrorMessage(err));
    },
  });

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
    setError('');
    onClose();
  };

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
            disabled={updateMutation.isPending}
          />

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              disabled={updateMutation.isPending}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
              disabled={updateMutation.isPending}
            >
              {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
