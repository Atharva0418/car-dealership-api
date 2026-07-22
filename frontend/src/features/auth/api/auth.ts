import { request } from '../../../shared/api/client';

type AuthCredentials = {
  email: string;
  password: string;
};

type RefreshPayload = {
  refreshToken: string;
};

type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  role: 'CUSTOMER' | 'ADMIN';
};

export function register(payload: AuthCredentials): Promise<AuthResponse> {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: payload,
  });
}

export function login(payload: AuthCredentials): Promise<AuthResponse> {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: payload,
  });
}

export function refresh(payload: RefreshPayload): Promise<AuthResponse> {
  return request<AuthResponse>('/auth/refresh', {
    method: 'POST',
    body: payload,
  });
}
