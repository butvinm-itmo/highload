import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { InterpretationDto } from '../types';
import { useAuth } from '../context/AuthContext';
import { interpretationsApi } from '../api';
import { EditInterpretationModal } from './EditInterpretationModal';
import { ImageLightbox } from './ImageLightbox';
import { AuthenticatedImage } from './AuthenticatedImage';
import { getErrorMessage } from '../utils/errorHandling';

interface InterpretationListProps {
  interpretations: InterpretationDto[];
  spreadId: string;
}

export function InterpretationList({ interpretations, spreadId }: InterpretationListProps) {
  const { user, hasRole } = useAuth();
  const [editingInterpretation, setEditingInterpretation] = useState<InterpretationDto | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const deleteMutation = useMutation({
    mutationFn: (interpretationId: string) =>
      interpretationsApi.deleteInterpretation(spreadId, interpretationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spread', spreadId] });
      setDeletingId(null);
    },
    onError: (error) => {
      alert(getErrorMessage(error));
      setDeletingId(null);
    },
  });

  const canEditOrDelete = (interpretation: InterpretationDto) => {
    return hasRole('ADMIN') || user?.id === interpretation.author.id;
  };

  const handleDelete = (interpretationId: string) => {
    if (confirm('Are you sure you want to delete this interpretation?')) {
      setDeletingId(interpretationId);
      deleteMutation.mutate(interpretationId);
    }
  };

  if (interpretations.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500 dark:text-gray-400">
        No interpretations yet. Be the first to share your insights!
      </div>
    );
  }

  return (
    <>
      <div className="space-y-4">
        {interpretations.map((interpretation) => {
          const formattedDate = new Date(interpretation.createdAt).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });

          return (
            <div key={interpretation.id} className="mystical-card p-6">
              <div className="flex justify-between items-start mb-4">
                <div className="flex items-center space-x-3">
                  <span className="font-serif font-semibold text-gray-100">{interpretation.author.username}</span>
                  <span className="text-xs font-serif text-gray-400">{formattedDate}</span>
                </div>
                {canEditOrDelete(interpretation) && (
                  <div className="flex space-x-3">
                    <button
                      onClick={() => setEditingInterpretation(interpretation)}
                      className="text-sm font-serif text-mystic-400 hover:text-mystic-300 transition-colors"
                      disabled={deletingId === interpretation.id}
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(interpretation.id)}
                      className="text-sm font-serif text-red-400 hover:text-red-300 transition-colors"
                      disabled={deletingId === interpretation.id}
                    >
                      {deletingId === interpretation.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                )}
              </div>
              <p className="text-gray-300 font-serif whitespace-pre-wrap leading-relaxed">{interpretation.text}</p>

              {interpretation.fileUrl && (
                <div className="mt-4">
                  <AuthenticatedImage
                    src={interpretation.fileUrl}
                    alt="Interpretation attachment"
                    className="max-w-md max-h-64 rounded-lg border-2 border-mystic-600 shadow-mystic cursor-pointer hover:shadow-cosmic hover:border-cosmic-500 transition-all"
                    onClick={() => setLightboxImage(interpretation.fileUrl!)}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>

      {editingInterpretation && (
        <EditInterpretationModal
          interpretation={editingInterpretation}
          spreadId={spreadId}
          isOpen={true}
          onClose={() => setEditingInterpretation(null)}
        />
      )}

      {lightboxImage && (
        <ImageLightbox
          imageUrl={lightboxImage}
          onClose={() => setLightboxImage(null)}
        />
      )}
    </>
  );
}
