import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Layout } from '../components/Layout';
import { usersApi } from '../api';
import type { UserDto } from '../types';
import { CreateUserModal } from '../components/CreateUserModal';
import { EditUserModal } from '../components/EditUserModal';
import { DeleteUserModal } from '../components/DeleteUserModal';

export function UsersPage() {
  const [page, setPage] = useState(0);
  const pageSize = 20;
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserDto | null>(null);
  const [deletingUser, setDeletingUser] = useState<UserDto | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['users', page],
    queryFn: () => usersApi.getUsers(page, pageSize),
  });

  const totalPages = data ? Math.ceil(data.totalCount / pageSize) : 0;

  const getRoleBadgeColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-gradient-to-r from-red-500 to-red-600 text-white shadow-lg border';
      case 'MEDIUM':
        return 'bg-gradient-to-r from-cosmic-500 to-mystic-500 text-white shadow-mystic border';
      case 'USER':
        return 'bg-gradient-to-r from-mystic-500 to-blue-500 text-white shadow-md border';
      default:
        return 'bg-void-700 text-gray-300 border border-gray-600';
    }
  };

  const getRoleBadgeBorderColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'rgba(248, 113, 113, 0.3)';
      case 'MEDIUM':
        return 'rgba(240, 171, 252, 0.3)';
      case 'USER':
        return 'rgba(167, 139, 250, 0.3)';
      default:
        return 'rgba(75, 85, 99, 1)';
    }
  };

  return (
    <Layout>
      <div className="space-y-8">
        <div className="flex justify-between items-center">
          <h1 className="text-4xl font-display font-bold text-gray-100">User Management</h1>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="px-6 py-3 bg-gradient-to-r from-mystic-600 to-cosmic-600 text-white rounded-lg shadow-mystic hover:from-mystic-500 hover:to-cosmic-500 hover:shadow-cosmic focus:outline-none focus:ring-2 focus:ring-mystic-500 transition-all duration-300 font-serif font-medium"
          >
            Create User
          </button>
        </div>

        {isLoading && (
          <div className="flex justify-center py-12">
            <div className="text-gray-600 dark:text-gray-400">Loading users...</div>
          </div>
        )}

        {error && (
          <div className="p-4 bg-red-100 border border-red-400 text-red-700 rounded">
            Error loading users. Please try again later.
          </div>
        )}

        {!isLoading && !error && data && (
          <>
            <div className="mystical-card overflow-hidden">
              <table className="min-w-full">
                <thead>
                  <tr className="border-b" style={{ borderColor: 'rgba(91, 33, 182, 0.3)' }}>
                    <th className="px-6 py-4 text-left text-xs font-display font-semibold text-gray-400 uppercase tracking-wider">
                      Username
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-display font-semibold text-gray-400 uppercase tracking-wider">
                      Role
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-display font-semibold text-gray-400 uppercase tracking-wider">
                      Created At
                    </th>
                    <th className="px-6 py-4 text-right text-xs font-display font-semibold text-gray-400 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y" style={{ borderColor: 'rgba(91, 33, 182, 0.2)' }}>
                  {data.data.map((user) => {
                    const formattedDate = new Date(user.createdAt).toLocaleDateString('en-US', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    });

                    return (
                      <tr key={user.id} className="hover:transition-colors" style={{ '--hover-bg': 'rgba(76, 29, 149, 0.2)' } as React.CSSProperties} onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(76, 29, 149, 0.2)'} onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-base font-serif font-medium text-gray-200">{user.username}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-3 py-1.5 inline-flex text-xs leading-5 font-display font-semibold rounded-full ${getRoleBadgeColor(
                              user.role.name
                            )}`}
                            style={{ borderColor: getRoleBadgeBorderColor(user.role.name) }}
                          >
                            {user.role.name}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-serif text-gray-400">
                          {formattedDate}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-serif font-medium space-x-3">
                          <button
                            onClick={() => setEditingUser(user)}
                            className="text-mystic-400 hover:text-mystic-300 transition-colors"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => setDeletingUser(user)}
                            className="text-red-400 hover:text-red-300 transition-colors"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex justify-between items-center mystical-card p-4">
                <div className="text-sm font-serif text-gray-300">
                  Showing page {page + 1} of {totalPages} ({data.totalCount} total users)
                </div>
                <div className="flex space-x-3">
                  <button
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    className="px-4 py-2 border-2 rounded-lg text-sm font-serif text-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-void-800/50 transition-all"
                    style={{ borderColor: 'rgba(91, 33, 182, 0.5)' }}
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page >= totalPages - 1}
                    className="px-4 py-2 border-2 rounded-lg text-sm font-serif text-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-void-800/50 transition-all"
                    style={{ borderColor: 'rgba(91, 33, 182, 0.5)' }}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <CreateUserModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      {editingUser && (
        <EditUserModal
          user={editingUser}
          isOpen={true}
          onClose={() => setEditingUser(null)}
        />
      )}

      {deletingUser && (
        <DeleteUserModal
          user={deletingUser}
          isOpen={true}
          onClose={() => setDeletingUser(null)}
        />
      )}
    </Layout>
  );
}
