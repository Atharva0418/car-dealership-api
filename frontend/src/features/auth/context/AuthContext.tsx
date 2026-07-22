import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useState,
} from 'react';

type AuthContextValue = {
  accessToken: string | null;
  refreshToken: string | null;
  email: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (accessToken: string, refreshToken: string, email: string) => void;
  logout: () => void;
  updateAccessToken: (newAccessToken: string) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

const canUseLocalStorage = () => typeof window !== 'undefined';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);

  useEffect(() => {
    if (!canUseLocalStorage()) {
      return;
    }

    setAccessToken(localStorage.getItem('accessToken'));
    setRefreshToken(localStorage.getItem('refreshToken'));
    setEmail(localStorage.getItem('email'));
  }, []);

  const login = useCallback(
    (newAccessToken: string, newRefreshToken: string, newEmail: string) => {
      setAccessToken(newAccessToken);
      setRefreshToken(newRefreshToken);
      setEmail(newEmail);

      if (!canUseLocalStorage()) {
        return;
      }

      localStorage.setItem('accessToken', newAccessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      localStorage.setItem('email', newEmail);
    },
    [],
  );

  const logout = useCallback(() => {
    setAccessToken(null);
    setRefreshToken(null);
    setEmail(null);

    if (!canUseLocalStorage()) {
      return;
    }

    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('email');
  }, []);

  const updateAccessToken = useCallback((newAccessToken: string) => {
    setAccessToken(newAccessToken);

    if (!canUseLocalStorage()) {
      return;
    }

    localStorage.setItem('accessToken', newAccessToken);
  }, []);

  const username = email?.split('@')[0] ?? '';
  const isAdmin = username.toLowerCase().includes('admin');

  return (
    <AuthContext.Provider
      value={{
        accessToken,
        refreshToken,
        email,
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
