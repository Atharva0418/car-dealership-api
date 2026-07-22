// @vitest-environment jsdom

import { renderHook, act } from '@testing-library/react';
import { type ReactNode } from 'react';
import { beforeEach, describe, expect, it } from 'vitest';

import { AuthProvider, useAuth } from '../context/AuthContext';

const wrapper = ({ children }: { children: ReactNode }) => (
  <AuthProvider>{children}</AuthProvider>
);

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('login stores accessToken, refreshToken, and email in both state and localStorage', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    act(() => {
      result.current.login(
        'access-token',
        'refresh-token',
        'buyer@example.com',
      );
    });

    expect(result.current.accessToken).toBe('access-token');
    expect(result.current.refreshToken).toBe('refresh-token');
    expect(result.current.email).toBe('buyer@example.com');
    expect(result.current.isAuthenticated).toBe(true);
    expect(localStorage.getItem('accessToken')).toBe('access-token');
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token');
    expect(localStorage.getItem('email')).toBe('buyer@example.com');
  });

  it('restores an existing localStorage session into state on mount', () => {
    localStorage.setItem('accessToken', 'stored-access-token');
    localStorage.setItem('refreshToken', 'stored-refresh-token');
    localStorage.setItem('email', 'stored@example.com');

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.accessToken).toBe('stored-access-token');
    expect(result.current.refreshToken).toBe('stored-refresh-token');
    expect(result.current.email).toBe('stored@example.com');
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('logout clears accessToken, refreshToken, and email from both state and localStorage', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    act(() => {
      result.current.login(
        'access-token',
        'refresh-token',
        'buyer@example.com',
      );
    });

    act(() => {
      result.current.logout();
    });

    expect(result.current.accessToken).toBeNull();
    expect(result.current.refreshToken).toBeNull();
    expect(result.current.email).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('email')).toBeNull();
  });

  it('isAdmin is true when the email username contains admin case-insensitively', () => {
    const { result, rerender } = renderHook(() => useAuth(), { wrapper });

    act(() => {
      result.current.login(
        'admin-access-token',
        'admin-refresh-token',
        'super.admin99@company.com',
      );
    });

    expect(result.current.isAdmin).toBe(true);

    act(() => {
      result.current.logout();
    });

    rerender();

    act(() => {
      result.current.login(
        'user-access-token',
        'user-refresh-token',
        'jane.doe@company.com',
      );
    });

    expect(result.current.isAdmin).toBe(false);
  });

  it('updateAccessToken replaces only accessToken in both state and localStorage', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    act(() => {
      result.current.login(
        'old-access-token',
        'refresh-token',
        'buyer@example.com',
      );
    });

    act(() => {
      result.current.updateAccessToken('new-access-token');
    });

    expect(result.current.accessToken).toBe('new-access-token');
    expect(result.current.refreshToken).toBe('refresh-token');
    expect(result.current.email).toBe('buyer@example.com');
    expect(localStorage.getItem('accessToken')).toBe('new-access-token');
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token');
    expect(localStorage.getItem('email')).toBe('buyer@example.com');
  });
});
