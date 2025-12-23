import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../components/Layout';
import { TarotCard } from '../components/TarotCard';
import { InterpretationList } from '../components/InterpretationList';
import { AddInterpretationForm } from '../components/AddInterpretationForm';
import { spreadsApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorHandling';

export function SpreadDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();

  const { data: spread, isLoading, error } = useQuery({
    queryKey: ['spread', id],
    queryFn: () => spreadsApi.getSpread(id!),
    enabled: !!id,
  });

  const deleteMutation = useMutation({
    mutationFn: () => spreadsApi.deleteSpread(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spreads'] });
      navigate('/');
    },
    onError: (err) => {
      alert(getErrorMessage(err));
    },
  });

  const handleDelete = () => {
    if (confirm('Are you sure you want to delete this spread?')) {
      deleteMutation.mutate();
    }
  };

  const canDelete = spread && (hasRole('ADMIN') || user?.id === spread.author.id);
  const canAddInterpretation = hasRole('MEDIUM');
  const hasUserInterpretation = spread?.interpretations.some(
    (interp) => interp.author.id === user?.id
  );

  if (isLoading) {
    return (
      <Layout>
        <div className="flex justify-center py-12">
          <div className="text-gray-600">Loading spread...</div>
        </div>
      </Layout>
    );
  }

  if (error || !spread) {
    return (
      <Layout>
        <div className="p-4 bg-red-100 border border-red-400 text-red-700 rounded">
          Error loading spread. Please try again later.
        </div>
      </Layout>
    );
  }

  const formattedDate = new Date(spread.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const sortedCards = [...spread.cards].sort((a, b) => a.positionInSpread - b.positionInSpread);

  return (
    <Layout>
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-start mb-4">
            <div className="flex-1">
              <h1 className="text-3xl font-bold text-gray-900 mb-2">{spread.question}</h1>
              <div className="flex items-center space-x-4 text-sm text-gray-600">
                <span className="flex items-center">
                  <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  {spread.author.username}
                </span>
                <span>{formattedDate}</span>
                <span className="px-2 py-1 text-xs font-medium text-indigo-600 bg-indigo-100 rounded">
                  {spread.layoutType.name}
                </span>
              </div>
            </div>
            {canDelete && (
              <button
                onClick={handleDelete}
                className="ml-4 px-3 py-1 text-sm text-red-600 border border-red-600 rounded hover:bg-red-50"
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            )}
          </div>
        </div>

        {/* Cards */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Cards</h2>
          <div className="flex flex-wrap gap-6 justify-center">
            {sortedCards.map((spreadCard) => (
              <TarotCard
                key={spreadCard.id}
                card={spreadCard.card}
                isReversed={spreadCard.isReversed}
                position={spreadCard.positionInSpread}
                size="medium"
              />
            ))}
          </div>
        </div>

        {/* Interpretations */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Interpretations ({spread.interpretations.length})
          </h2>

          {canAddInterpretation && !hasUserInterpretation && (
            <div className="mb-6">
              <AddInterpretationForm spreadId={spread.id} />
            </div>
          )}

          {canAddInterpretation && hasUserInterpretation && (
            <div className="mb-4 p-3 bg-blue-100 border border-blue-400 text-blue-700 rounded text-sm">
              You have already added an interpretation for this spread.
            </div>
          )}

          <InterpretationList interpretations={spread.interpretations} spreadId={spread.id} />
        </div>
      </div>
    </Layout>
  );
}
