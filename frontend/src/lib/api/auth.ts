import { request } from './client';
import type { AuthResponse } from './types';

type AuthCredentials = {
  email: string;
  password: string;
};

type RefreshPayload = {
  refreshToken: string;
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
