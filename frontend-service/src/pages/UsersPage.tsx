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
                            className="px-3 py-1.5 inline-flex text-xs leading-5 font-display rounded-full border text-white font-bold"
                            style={(() => {
                              const roleName = user.role || 'UNKNOWN';
                              switch (roleName) {
                                case 'ADMIN':
                                  return {
                                    background: 'linear-gradient(to right, rgb(220, 38, 38), rgb(185, 28, 28))',
                                    borderColor: 'rgba(248, 113, 113, 0.5)',
                                    boxShadow: '0 0 15px rgba(220, 38, 38, 0.4)',
                                  };
                                case 'MEDIUM':
                                  return {
                                    background: 'linear-gradient(to right, rgb(168, 85, 247), rgb(147, 51, 234))',
                                    borderColor: 'rgba(240, 171, 252, 0.5)',
                                    boxShadow: '0 0 15px rgba(168, 85, 247, 0.4)',
                                  };
                                case 'USER':
                                  return {
                                    background: 'linear-gradient(to right, rgb(59, 130, 246), rgb(37, 99, 235))',
                                    borderColor: 'rgba(96, 165, 250, 0.5)',
                                    boxShadow: '0 0 15px rgba(59, 130, 246, 0.4)',
                                  };
                                default:
                                  return {
                                    background: 'rgb(55, 65, 81)',
                                    borderColor: 'rgba(75, 85, 99, 0.8)',
                                  };
                              }
                            })()}
                          >
                            {user.role || 'UNKNOWN'}
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
