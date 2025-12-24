import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { UserDto } from '../types';
import { usersApi } from '../api';
import { getErrorMessage } from '../utils/errorHandling';

interface EditUserModalProps {
  user: UserDto;
  isOpen: boolean;
  onClose: () => void;
}

export function EditUserModal({ user, isOpen, onClose }: EditUserModalProps) {
  const [username, setUsername] = useState(user.username);
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'USER' | 'MEDIUM' | 'ADMIN'>(user.role.name);
  const [error, setError] = useState('');

  const queryClient = useQueryClient();

  useEffect(() => {
    setUsername(user.username);
    setRole(user.role.name);
    setPassword('');
  }, [user]);

  const updateMutation = useMutation({
    mutationFn: () =>
      usersApi.updateUser(user.id, {
        username: username !== user.username ? username.trim() : undefined,
        password: password ? password : undefined,
        role: role !== user.role.name ? role : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      handleClose();
    },
    onError: (err: unknown) => {
      setError(getErrorMessage(err));
    },
  });

  const handleClose = () => {
    setUsername(user.username);
    setPassword('');
    setRole(user.role.name);
    setError('');
    onClose();
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!username.trim()) {
      setError('Username is required');
      return;
    }

    updateMutation.mutate();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-900">Edit User</h2>
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

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="edit-username" className="block text-sm font-medium text-gray-700 mb-1">
              Username
            </label>
            <input
              id="edit-username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
              disabled={updateMutation.isPending}
            />
          </div>

          <div>
            <label htmlFor="edit-password" className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              id="edit-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Leave empty to keep current password"
              disabled={updateMutation.isPending}
            />
            <p className="mt-1 text-xs text-gray-500">
              Leave empty to keep current password
            </p>
          </div>

          <div>
            <label htmlFor="edit-role" className="block text-sm font-medium text-gray-700 mb-1">
              Role
            </label>
            <select
              id="edit-role"
              value={role}
              onChange={(e) => setRole(e.target.value as 'USER' | 'MEDIUM' | 'ADMIN')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
              disabled={updateMutation.isPending}
            >
              <option value="USER">USER</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>

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
