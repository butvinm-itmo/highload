import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { UserDto } from '../types';
import { usersApi } from '../api';
import { getErrorMessage } from '../utils/errorHandling';

interface DeleteUserModalProps {
  user: UserDto;
  isOpen: boolean;
  onClose: () => void;
}

export function DeleteUserModal({ user, isOpen, onClose }: DeleteUserModalProps) {
  const queryClient = useQueryClient();

  const deleteMutation = useMutation({
    mutationFn: () => usersApi.deleteUser(user.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      onClose();
    },
    onError: (err) => {
      alert(getErrorMessage(err));
    },
  });

  const handleDelete = () => {
    deleteMutation.mutate();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
        <div className="mb-4">
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Delete User</h2>
          <p className="text-gray-600">
            Are you sure you want to delete user <strong>{user.username}</strong>?
          </p>
          <p className="text-sm text-red-600 mt-2">
            This action will permanently delete all of the user's spreads and interpretations.
            This cannot be undone.
          </p>
        </div>

        <div className="flex space-x-3 pt-4">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
            disabled={deleteMutation.isPending}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleDelete}
            className="flex-1 px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
            disabled={deleteMutation.isPending}
          >
            {deleteMutation.isPending ? 'Deleting...' : 'Delete User'}
          </button>
        </div>
      </div>
    </div>
  );
}
