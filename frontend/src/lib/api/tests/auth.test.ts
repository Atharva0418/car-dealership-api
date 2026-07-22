import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';

import { login, refresh, register } from '../auth';
import { getLastRequest, resetRequestSnapshots } from './msw/handlers';
import { server } from './msw/server';

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  resetRequestSnapshots();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('auth API', () => {
  it('login sends POST /api/auth/login with email and password as body', async () => {
    await login({ email: 'buyer@example.com', password: 'secret' });

    const request = getLastRequest();

    expect(request.method).toBe('POST');
    expect(new URL(request.url).pathname).toBe('/api/auth/login');
    expect(request.body).toEqual({
      email: 'buyer@example.com',
      password: 'secret',
    });
  });

  it('register sends POST /api/auth/register with correct payload shape', async () => {
    await register({ email: 'seller@example.com', password: 'secret' });

    const request = getLastRequest();

    expect(request.method).toBe('POST');
    expect(new URL(request.url).pathname).toBe('/api/auth/register');
    expect(request.body).toEqual({
      email: 'seller@example.com',
      password: 'secret',
    });
  });

  it('refresh sends POST /api/auth/refresh with refresh token in body', async () => {
    await refresh({ refreshToken: 'refresh-token' });

    const request = getLastRequest();

    expect(request.method).toBe('POST');
    expect(new URL(request.url).pathname).toBe('/api/auth/refresh');
    expect(request.body).toEqual({
      refreshToken: 'refresh-token',
    });
  });
});
