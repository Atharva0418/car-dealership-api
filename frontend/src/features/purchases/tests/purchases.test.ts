import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';

import { setToken } from '../../auth/api/tokenStore';
import { getLastRequest, resetRequestSnapshots } from '../../../shared/api/tests/msw/handlers';
import { server } from '../../../shared/api/tests/msw/server';
import { listMyPurchases } from '../api/purchases';

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  setToken(null);
  resetRequestSnapshots();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('purchases API', () => {
  it('calls GET /api/purchases/me with the authenticated access token', async () => {
    setToken('access-token');

    await listMyPurchases();

    const request = getLastRequest();

    expect(request.method).toBe('GET');
    expect(new URL(request.url).pathname).toBe('/api/purchases/me');
    expect(request.headers.authorization).toBe('Bearer access-token');
  });
});
