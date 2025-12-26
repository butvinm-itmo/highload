import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { NotificationBell } from './NotificationBell';

interface LayoutProps {
  children: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getRoleBadgeColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-gradient-to-r from-red-600 to-red-700 text-white shadow-cosmic';
      case 'MEDIUM':
        return 'bg-gradient-to-r from-cosmic-600 to-mystic-600 text-white shadow-mystic';
      case 'USER':
        return 'bg-gradient-to-r from-mystic-600 to-mystic-700 text-white shadow-md';
      default:
        return 'bg-void-700 text-gray-300';
    }
  };

  return (
    <div className="min-h-screen transition-colors duration-300 relative">
      <nav className="backdrop-blur-md border-b shadow-mystic transition-colors duration-300 sticky top-0 z-50" style={{ backgroundColor: 'rgba(17, 24, 39, 0.8)', borderBottomColor: 'rgba(91, 33, 182, 0.3)' }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <Link to="/" className="flex items-center group">
                <span className="text-3xl font-display font-bold bg-gradient-to-r from-mystic-400 via-cosmic-400 to-mystic-400 bg-clip-text text-transparent animate-shimmer" style={{backgroundSize: '200% auto'}}>
                  Tarology
                </span>
                <span className="ml-2 text-xs font-accent text-gold-400 opacity-0 group-hover:opacity-100 transition-opacity">
                  unveil your destiny
                </span>
              </Link>
              <div className="hidden sm:ml-12 sm:flex sm:space-x-8">
                <Link
                  to="/"
                  className="inline-flex items-center px-1 pt-1 text-sm font-serif font-medium text-gray-100 hover:text-mystic-300 transition-colors border-b-2 border-transparent hover:border-mystic-500"
                >
                  Spreads
                </Link>
                {hasRole('ADMIN') && (
                  <Link
                    to="/users"
                    className="inline-flex items-center px-1 pt-1 text-sm font-serif font-medium text-gray-400 hover:text-cosmic-300 transition-colors border-b-2 border-transparent hover:border-cosmic-500"
                  >
                    Users
                  </Link>
                )}
              </div>
            </div>
            <div className="flex items-center space-x-4">
              {user && (
                <>
                  <NotificationBell />
                  <div className="flex items-center space-x-3">
                    <span className="text-sm font-serif text-gray-300">{user.username}</span>
                    <span
                      className={`px-3 py-1 text-xs font-display font-semibold rounded-full ${getRoleBadgeColor(
                        user.role.name
                      )}`}
                    >
                      {user.role.name}
                    </span>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="text-sm font-serif text-gray-400 hover:text-red-400 transition-colors px-3 py-1.5 rounded-md"
                    style={{ '--hover-bg': 'rgba(31, 41, 55, 0.5)' } as React.CSSProperties}
                    onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(31, 41, 55, 0.5)'}
                    onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                  >
                    Logout
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">{children}</main>
    </div>
  );
}
