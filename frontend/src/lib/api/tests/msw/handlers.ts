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
];
