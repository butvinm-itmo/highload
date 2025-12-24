import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { interpretationsApi } from '../api';
import { getErrorMessage } from '../utils/errorHandling';

interface AddInterpretationFormProps {
  spreadId: string;
}

export function AddInterpretationForm({ spreadId }: AddInterpretationFormProps) {
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (interpretationText: string) =>
      interpretationsApi.createInterpretation(spreadId, { text: interpretationText }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spread', spreadId] });
      setText('');
      setError('');
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

    createMutation.mutate(text.trim());
  };

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
          disabled={createMutation.isPending}
        />
        <div className="mt-3 flex justify-end">
          <button
            type="submit"
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
            disabled={createMutation.isPending}
          >
            {createMutation.isPending ? 'Adding...' : 'Add Interpretation'}
          </button>
        </div>
      </form>
    </div>
  );
}
