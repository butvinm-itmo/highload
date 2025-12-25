import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { UserDto } from '../types';
import { authApi } from '../api';

interface AuthContextType {
  user: UserDto | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: 'USER' | 'MEDIUM' | 'ADMIN') => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Helper function to decode JWT and extract user ID
  const getUserIdFromToken = (token: string): string => {
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      return decoded.sub || '';
    } catch {
      return '';
    }
  };

  // Load user from localStorage on mount
  useEffect(() => {
    const storedToken = localStorage.getItem('auth_token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      try {
        const parsedUser: UserDto = JSON.parse(storedUser);

        // If user doesn't have ID, decode it from token
        if (!parsedUser.id && storedToken) {
          parsedUser.id = getUserIdFromToken(storedToken);
          // Update localStorage with the ID
          localStorage.setItem('user', JSON.stringify(parsedUser));
        }

        setToken(storedToken);
        setUser(parsedUser);
      } catch (error) {
        // Invalid stored data, clear it
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    try {
      const response = await authApi.login({ username, password });

      // Store token
      localStorage.setItem('auth_token', response.token);

      // Decode JWT to extract user ID from the payload
      const userId = getUserIdFromToken(response.token);

      // Create user object from auth response
      // Backend returns role as string, but we need Role object
      const roleObject = typeof response.role === 'string'
        ? { id: '', name: response.role as 'USER' | 'MEDIUM' | 'ADMIN' }
        : response.role;

      const userData: UserDto = {
        id: userId,
        username: response.username,
        role: roleObject,
        createdAt: new Date().toISOString(),
      };

      localStorage.setItem('user', JSON.stringify(userData));
      setToken(response.token);
      setUser(userData);
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role: 'USER' | 'MEDIUM' | 'ADMIN'): boolean => {
    if (!user) return false;

    // Role hierarchy: ADMIN > MEDIUM > USER
    const roleHierarchy: Record<string, number> = {
      ADMIN: 3,
      MEDIUM: 2,
      USER: 1,
    };

    const userRoleLevel = roleHierarchy[user.role.name] || 0;
    const requiredRoleLevel = roleHierarchy[role] || 0;

    return userRoleLevel >= requiredRoleLevel;
  };

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: !!token && !!user,
    isLoading,
    login,
    logout,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
