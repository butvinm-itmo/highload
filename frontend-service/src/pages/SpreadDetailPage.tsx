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
          <div className="text-gray-600 dark:text-gray-400">Loading spread...</div>
        </div>
      </Layout>
    );
  }

  if (error || !spread) {
    return (
      <Layout>
        <div className="p-4 bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 rounded">
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
      <div className="max-w-5xl mx-auto space-y-8 animate-fade-in">
        {/* Header */}
        <div className="mystical-card p-8">
          <div className="flex justify-between items-start mb-6">
            <div className="flex-1">
              <h1 className="text-4xl font-display font-bold text-gray-100 mb-4 gold-accent">{spread.question}</h1>
              <div className="flex items-center space-x-6 text-sm text-gray-400 font-serif">
                <span className="flex items-center group">
                  <svg className="w-5 h-5 mr-2 text-mystic-400 group-hover:text-mystic-300 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  <span className="group-hover:text-gray-300 transition-colors">{spread.author.username}</span>
                </span>
                <span className="text-mystic-500 italic">{formattedDate}</span>
                <span className="px-4 py-1.5 text-xs font-accent font-semibold text-gold-300 rounded-full shadow-gold" style={{ background: 'linear-gradient(to right, rgba(76, 29, 149, 0.8), rgba(112, 26, 117, 0.8))', borderWidth: '1px', borderColor: 'rgba(245, 158, 11, 0.3)' }}>
                  {spread.layoutType.name}
                </span>
              </div>
            </div>
            {canDelete && (
              <button
                onClick={handleDelete}
                className="ml-4 px-4 py-2 text-sm font-serif text-red-300 border-2 border-red-600/50 rounded-lg hover:bg-red-900/30 hover:border-red-500 transition-all duration-300 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            )}
          </div>
        </div>

        {/* Cards */}
        <div className="mystical-card p-8">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-3xl font-display font-semibold text-gray-100 tracking-wide">Cards</h2>
            <div className="h-px flex-1 ml-6 bg-gradient-to-r from-mystic-600 via-cosmic-600 to-transparent"></div>
          </div>
          <div className="flex flex-wrap gap-8 justify-center py-8 relative overflow-hidden" style={{ minHeight: '400px' }}>
            {sortedCards.map((spreadCard, index) => (
              <TarotCard
                key={spreadCard.id}
                card={spreadCard.card}
                isReversed={spreadCard.isReversed}
                position={spreadCard.positionInSpread}
                size="medium"
                delay={index * 0.2}
                spreadFromDeck={true}
                totalCards={sortedCards.length}
              />
            ))}
          </div>
        </div>

        {/* Interpretations */}
        <div className="mystical-card p-8">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-3xl font-display font-semibold text-gray-100 tracking-wide">
              Interpretations <span className="text-cosmic-400 font-accent">({spread.interpretations.length})</span>
            </h2>
            <div className="h-px flex-1 ml-6 bg-gradient-to-r from-cosmic-600 via-mystic-600 to-transparent"></div>
          </div>

          {canAddInterpretation && !hasUserInterpretation && (
            <div className="mb-8">
              <AddInterpretationForm spreadId={spread.id} />
            </div>
          )}

          {canAddInterpretation && hasUserInterpretation && (
            <div className="mb-6 p-4 rounded-lg border backdrop-blur-sm font-serif" style={{ backgroundColor: 'rgba(59, 130, 246, 0.15)', borderColor: 'rgba(96, 165, 250, 0.4)' }}>
              <div className="flex items-center text-blue-300">
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                You have already added an interpretation for this spread.
              </div>
            </div>
          )}

          <InterpretationList interpretations={spread.interpretations} spreadId={spread.id} />
        </div>
      </div>
    </Layout>
  );
}
