import { getToken } from './tokenStore';

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  headers?: Record<string, string>;
};

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(status: number, body: unknown) {
    super(`API request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

function buildApiUrl(path: string): string {
  const baseUrl = globalThis.location?.origin ?? 'http://localhost';

  return new URL(`/api${path}`, baseUrl).toString();
}

async function parseResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined;
  }

  const text = await response.text();

  if (!text) {
    return undefined;
  }

  return JSON.parse(text);
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    ...options.headers,
  };

  if (options.body !== undefined) {
    headers['Content-Type'] = headers['Content-Type'] ?? 'application/json';
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(buildApiUrl(path), {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const body = await parseResponseBody(response);

  if (!response.ok) {
    throw new ApiError(response.status, body);
  }

  return body as T;
}
