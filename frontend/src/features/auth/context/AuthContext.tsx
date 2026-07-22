import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useState,
} from 'react';

import { setToken } from '../api/tokenStore';

type UserRole = 'CUSTOMER' | 'ADMIN';

type AuthContextValue = {
  accessToken: string | null;
  refreshToken: string | null;
  email: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (
    accessToken: string,
    refreshToken: string,
    email: string,
    role?: UserRole,
  ) => void;
  logout: () => void;
  updateAccessToken: (newAccessToken: string) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

const canUseLocalStorage = () => typeof window !== 'undefined';

function normalizeRole(role: string | null | undefined): UserRole {
  return role === 'ADMIN' ? 'ADMIN' : 'CUSTOMER';
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);

  useEffect(() => {
    if (!canUseLocalStorage()) {
      return;
    }

    const storedAccessToken = localStorage.getItem('accessToken');

    setAccessToken(storedAccessToken);
    setRefreshToken(localStorage.getItem('refreshToken'));
    setEmail(localStorage.getItem('email'));
    setRole(storedAccessToken ? normalizeRole(localStorage.getItem('role')) : null);
    setToken(storedAccessToken);
  }, []);

  const login = useCallback(
    (
      newAccessToken: string,
      newRefreshToken: string,
      newEmail: string,
      newRole: UserRole = 'CUSTOMER',
    ) => {
      setAccessToken(newAccessToken);
      setRefreshToken(newRefreshToken);
      setEmail(newEmail);
      setRole(newRole);
      setToken(newAccessToken);

      if (!canUseLocalStorage()) {
        return;
      }

      localStorage.setItem('accessToken', newAccessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      localStorage.setItem('email', newEmail);
      localStorage.setItem('role', newRole);
    },
    [],
  );

  const logout = useCallback(() => {
    setAccessToken(null);
    setRefreshToken(null);
    setEmail(null);
    setRole(null);
    setToken(null);

    if (!canUseLocalStorage()) {
      return;
    }

    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
  }, []);

  const updateAccessToken = useCallback((newAccessToken: string) => {
    setAccessToken(newAccessToken);
    setToken(newAccessToken);

    if (!canUseLocalStorage()) {
      return;
    }

    localStorage.setItem('accessToken', newAccessToken);
  }, []);

  const isAdmin = role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        accessToken,
        refreshToken,
        email,
        role,
        isAuthenticated: accessToken !== null,
        isAdmin,
        login,
        logout,
        updateAccessToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const auth = useContext(AuthContext);

  if (auth === null) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return auth;
}
