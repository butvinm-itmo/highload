import React, { useState, FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cardsApi, spreadsApi } from '../api';
import { LayoutTypeDto } from '../types';
import { getErrorMessage } from '../utils/errorHandling';

interface CreateSpreadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateSpreadModal({ isOpen, onClose }: CreateSpreadModalProps) {
  const [question, setQuestion] = useState('');
  const [selectedLayoutId, setSelectedLayoutId] = useState('');
  const [error, setError] = useState('');

  const queryClient = useQueryClient();

  const { data: layoutsData, isLoading: layoutsLoading } = useQuery({
    queryKey: ['layoutTypes'],
    queryFn: () => cardsApi.getLayoutTypes(0, 20),
  });

  const createMutation = useMutation({
    mutationFn: spreadsApi.createSpread,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spreads'] });
      handleClose();
    },
    onError: (err) => {
      setError(getErrorMessage(err));
    },
  });

  const handleClose = () => {
    setQuestion('');
    setSelectedLayoutId('');
    setError('');
    onClose();
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!question.trim()) {
      setError('Please enter a question');
      return;
    }

    if (!selectedLayoutId) {
      setError('Please select a layout type');
      return;
    }

    createMutation.mutate({
      question: question.trim(),
      layoutTypeId: selectedLayoutId,
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-900">Create New Spread</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
            disabled={createMutation.isPending}
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

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="question" className="block text-sm font-medium text-gray-700 mb-1">
              Your Question
            </label>
            <textarea
              id="question"
              rows={3}
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="What guidance do you seek from the cards?"
              disabled={createMutation.isPending}
            />
          </div>

          <div>
            <label htmlFor="layoutType" className="block text-sm font-medium text-gray-700 mb-1">
              Layout Type
            </label>
            {layoutsLoading ? (
              <div className="text-sm text-gray-500">Loading layouts...</div>
            ) : (
              <select
                id="layoutType"
                value={selectedLayoutId}
                onChange={(e) => setSelectedLayoutId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                disabled={createMutation.isPending}
              >
                <option value="">Select a layout</option>
                {layoutsData?.data.map((layout: LayoutTypeDto) => (
                  <option key={layout.id} value={layout.id}>
                    {layout.name} ({layout.cardsCount} cards)
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              disabled={createMutation.isPending}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
              disabled={createMutation.isPending || layoutsLoading}
            >
              {createMutation.isPending ? 'Creating...' : 'Create Spread'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
