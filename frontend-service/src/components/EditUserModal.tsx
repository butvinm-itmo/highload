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
  const [role, setRole] = useState<'USER' | 'MEDIUM' | 'ADMIN'>(user.role);
  const [error, setError] = useState('');

  const queryClient = useQueryClient();

  useEffect(() => {
    setUsername(user.username);
    setRole(user.role);
    setPassword('');
  }, [user]);

  const updateMutation = useMutation({
    mutationFn: () =>
      usersApi.updateUser(user.id, {
        username: username !== user.username ? username.trim() : undefined,
        password: password ? password : undefined,
        role: role !== user.role ? role : undefined,
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
    setRole(user.role);
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
    <div className="fixed inset-0 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.75)' }}>
      <div className="mystical-card max-w-md w-full p-8 animate-fade-in">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-3xl font-display font-bold text-gray-100">Edit User</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-mystic-300 transition-colors"
            disabled={updateMutation.isPending}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg border backdrop-blur-sm font-serif" style={{ backgroundColor: 'rgba(153, 27, 27, 0.2)', borderColor: 'rgba(239, 68, 68, 0.5)' }}>
            <div className="text-red-300">{error}</div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label htmlFor="edit-username" className="block text-sm font-serif font-medium text-gray-300 mb-2">
              Username
            </label>
            <input
              id="edit-username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-4 py-3 border rounded-lg backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif text-gray-900"
              style={{ backgroundColor: 'rgba(229, 231, 235, 0.95)', borderColor: 'rgba(91, 33, 182, 0.3)' }}
              disabled={updateMutation.isPending}
            />
          </div>

          <div>
            <label htmlFor="edit-password" className="block text-sm font-serif font-medium text-gray-300 mb-2">
              Password
            </label>
            <input
              id="edit-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-3 border rounded-lg backdrop-blur-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif text-gray-900"
              style={{ backgroundColor: 'rgba(229, 231, 235, 0.95)', borderColor: 'rgba(91, 33, 182, 0.3)' }}
              placeholder="Leave empty to keep current password"
              disabled={updateMutation.isPending}
            />
            <p className="mt-2 text-xs font-serif text-gray-500 italic">
              Leave empty to keep current password
            </p>
          </div>

          <div>
            <label htmlFor="edit-role" className="block text-sm font-serif font-medium text-gray-300 mb-2">
              Role
            </label>
            <select
              id="edit-role"
              value={role}
              onChange={(e) => setRole(e.target.value as 'USER' | 'MEDIUM' | 'ADMIN')}
              className="w-full px-4 py-3 border rounded-lg backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif text-gray-900"
              style={{ backgroundColor: 'rgba(229, 231, 235, 0.95)', borderColor: 'rgba(91, 33, 182, 0.3)' }}
              disabled={updateMutation.isPending}
            >
              <option value="USER">USER</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>

          <div className="flex space-x-4 pt-6">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-5 py-3 border-2 rounded-lg text-base font-serif font-medium text-gray-300 hover:bg-void-800/50 focus:outline-none focus:ring-2 focus:ring-mystic-500 transition-all disabled:opacity-50"
              style={{ borderColor: 'rgba(91, 33, 182, 0.5)' }}
              disabled={updateMutation.isPending}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-5 py-3 rounded-lg shadow-mystic text-base font-serif font-medium text-white bg-gradient-to-r from-mystic-600 to-cosmic-600 hover:from-mystic-500 hover:to-cosmic-500 hover:shadow-cosmic focus:outline-none focus:ring-2 focus:ring-mystic-500 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed transition-all duration-300"
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
