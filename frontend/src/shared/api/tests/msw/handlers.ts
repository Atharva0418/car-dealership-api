import { http, HttpResponse } from 'msw';

export type RequestSnapshot = {
  method: string;
  url: string;
  headers: Record<string, string>;
  body: unknown;
};

const requestSnapshots: RequestSnapshot[] = [];

async function snapshotRequest(request: Request): Promise<void> {
  const contentType = request.headers.get('content-type') ?? '';
  const body = contentType.includes('application/json') ? await request.json() : null;

  requestSnapshots.push({
    method: request.method,
    url: request.url,
    headers: Object.fromEntries(request.headers.entries()),
    body,
  });
}

export function getLastRequest(): RequestSnapshot {
  const request = requestSnapshots.at(-1);

  if (!request) {
    throw new Error('No request was captured');
  }

  return request;
}

export function resetRequestSnapshots(): void {
  requestSnapshots.length = 0;
}

export const handlers = [
  http.all('http://localhost/api/echo', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({ ok: true });
  }),
  http.get('http://localhost/api/success', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({ message: 'done' });
  }),
  http.get('http://localhost/api/failure', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({ message: 'bad request' }, { status: 400 });
  }),
  http.get('http://localhost/api/text-failure', async ({ request }) => {
    await snapshotRequest(request);

    return new HttpResponse('Plain backend error', { status: 400 });
  }),
  http.post('*/api/auth/login', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
    });
  }),
  http.post('*/api/auth/register', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
    });
  }),
  http.post('*/api/auth/refresh', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
    });
  }),
  http.get('http://localhost/api/vehicles', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json([
      {
        id: 'vehicle-1',
        make: 'Toyota',
        model: 'Camry',
        year: 2024,
        price: 28000,
        category: 'sedan',
        quantityInStock: 4,
      },
    ]);
  }),
  http.post('http://localhost/api/vehicles', async ({ request }) => {
    await snapshotRequest(request);

    return HttpResponse.json({
      id: 'vehicle-1',
      make: 'Toyota',
      model: 'Camry',
      year: 2024,
      price: 28000,
      category: 'sedan',
      quantityInStock: 4,
    });
  }),
  http.put('http://localhost/api/vehicles/:id', async ({ request, params }) => {
    await snapshotRequest(request);

    return HttpResponse.json({
      id: params.id,
      make: 'Toyota',
      model: 'Camry',
      year: 2024,
      price: 29000,
      category: 'sedan',
      quantityInStock: 4,
    });
  }),
  http.delete('http://localhost/api/vehicles/:id', async ({ request }) => {
    await snapshotRequest(request);

    return new HttpResponse(null, { status: 204 });
  }),
];
