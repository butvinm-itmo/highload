import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorHandling';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(username, password);
      navigate('/');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
      {/* Animated background orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 rounded-full blur-3xl animate-pulse-slow" style={{ backgroundColor: 'rgba(124, 58, 237, 0.1)' }} />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 rounded-full blur-3xl animate-pulse-slow" style={{ backgroundColor: 'rgba(192, 38, 211, 0.1)', animationDelay: '1s' }} />
      </div>

      <div className="max-w-md w-full mx-4 relative z-10">
        <div className="mystical-card p-10">
          <div className="text-center mb-10">
            <h1 className="text-5xl font-display font-bold bg-gradient-to-r from-mystic-400 via-cosmic-400 to-mystic-400 bg-clip-text text-transparent mb-3">
              Tarology
            </h1>
            <p className="text-gold-400 font-accent text-lg italic tracking-wide">Unveil Your Destiny</p>
            <div className="mt-4 h-px w-32 mx-auto bg-gradient-to-r from-transparent via-mystic-500 to-transparent" />
            <p className="text-gray-400 font-serif mt-4">Sign in to your account</p>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-900/30 border border-red-500/50 text-red-300 rounded-lg backdrop-blur-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="username" className="block text-sm font-serif font-medium text-gray-300 mb-2">
                Username
              </label>
              <input
                id="username"
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="block w-full px-4 py-3 border rounded-lg backdrop-blur-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif"
                style={{ backgroundColor: 'rgba(3, 7, 18, 0.5)', borderColor: 'rgba(91, 33, 182, 0.5)' }}
                placeholder="Enter your username"
                disabled={isLoading}
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-serif font-medium text-gray-300 mb-2">
                Password
              </label>
              <input
                id="password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="block w-full px-4 py-3 border rounded-lg backdrop-blur-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-mystic-500 focus:border-mystic-500 transition-all font-serif"
                style={{ backgroundColor: 'rgba(3, 7, 18, 0.5)', borderColor: 'rgba(91, 33, 182, 0.5)' }}
                placeholder="Enter your password"
                disabled={isLoading}
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex justify-center py-3.5 px-6 border border-transparent rounded-lg shadow-mystic text-base font-serif font-medium text-white bg-gradient-to-r from-mystic-600 to-cosmic-600 hover:from-mystic-500 hover:to-cosmic-500 hover:shadow-cosmic focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-void-950 focus:ring-mystic-500 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed transition-all duration-300"
            >
              {isLoading ? 'Summoning the cards...' : 'Enter the Realm'}
            </button>
          </form>

          <div className="mt-8 text-center">
            <div className="h-px w-full bg-gradient-to-r from-transparent via-mystic-800/50 to-transparent mb-4" />
            <p className="text-xs font-serif text-gray-500 uppercase tracking-wider mb-2">Demo Accounts</p>
            <p className="text-sm font-accent text-mystic-400">Admin: <span className="text-gray-300">admin</span> / <span className="text-gray-300">Admin@123</span></p>
          </div>
        </div>
      </div>
    </div>
  );
}
