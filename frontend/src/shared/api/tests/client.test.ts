import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { setToken } from '../../../features/auth/api/tokenStore';
import { ApiError, request } from '../client';
import { getLastRequest, resetRequestSnapshots } from './msw/handlers';
import { server } from './msw/server';

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  vi.unstubAllEnvs();
  setToken(null);
  resetRequestSnapshots();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('request', () => {
  it('builds request URL as /api plus the given path', async () => {
    await request('/echo');

    expect(new URL(getLastRequest().url).pathname).toBe('/api/echo');
  });

  it('uses VITE_API_BASE_URL when it is configured', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://car-api.onrender.com');

    await request('/echo');

    const capturedUrl = new URL(getLastRequest().url);

    expect(capturedUrl.origin).toBe('https://car-api.onrender.com');
    expect(capturedUrl.pathname).toBe('/api/echo');
  });

  it('sends JSON body with correct Content-Type on POST', async () => {
    await request('/echo', {
      method: 'POST',
      body: { make: 'Toyota' },
    });

    const capturedRequest = getLastRequest();

    expect(capturedRequest.method).toBe('POST');
    expect(capturedRequest.headers['content-type']).toBe('application/json');
    expect(capturedRequest.body).toEqual({ make: 'Toyota' });
  });

  it('sends JSON body with correct Content-Type on PUT', async () => {
    await request('/echo', {
      method: 'PUT',
      body: { price: 25000 },
    });

    const capturedRequest = getLastRequest();

    expect(capturedRequest.method).toBe('PUT');
    expect(capturedRequest.headers['content-type']).toBe('application/json');
    expect(capturedRequest.body).toEqual({ price: 25000 });
  });

  it('parses and returns JSON response body on success', async () => {
    await expect(request<{ message: string }>('/success')).resolves.toEqual({ message: 'done' });
  });

  it('throws ApiError with correct status and parsed body on non-2xx', async () => {
    await expect(request('/failure')).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      body: { message: 'bad request' },
    });

    await expect(request('/failure')).rejects.toBeInstanceOf(ApiError);
  });

  it('keeps plain text error responses as text on non-2xx', async () => {
    await expect(request('/text-failure')).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      body: 'Plain backend error',
    });
  });

  it('attaches Authorization header when getToken returns a token', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://car-api.onrender.com');
    setToken('abc');

    await request('/echo');

    expect(new URL(getLastRequest().url).origin).toBe('https://car-api.onrender.com');
    expect(getLastRequest().headers.authorization).toBe('Bearer abc');
  });

  it('omits Authorization header entirely when getToken returns null', async () => {
    setToken(null);

    await request('/echo');

    expect(getLastRequest().headers).not.toHaveProperty('authorization');
  });
});
