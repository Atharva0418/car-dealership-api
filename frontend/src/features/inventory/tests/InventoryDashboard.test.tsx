// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { delay, http, HttpResponse } from 'msw';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';

import App from '../../../app/App';
import { AuthProvider } from '../../auth/context/AuthContext';
import { setToken } from '../../auth/api/tokenStore';
import { getLastRequest, resetRequestSnapshots } from '../../../shared/api/tests/msw/handlers';
import { server } from '../../../shared/api/tests/msw/server';

const vehicles = [
  {
    id: 'vehicle-1',
    make: 'Toyota',
    model: 'Camry',
    category: 'Sedan',
    price: 28000,
    quantityInStock: 4,
  },
  {
    id: 'vehicle-2',
    make: 'Ford',
    model: 'Bronco',
    category: 'SUV',
    price: 41500,
    quantityInStock: 0,
  },
];

function renderAuthenticatedApp() {
  localStorage.setItem('accessToken', 'access-token');
  localStorage.setItem('refreshToken', 'refresh-token');
  localStorage.setItem('email', 'manager@example.com');
  setToken('access-token');

  return render(
    <AuthProvider>
      <App />
    </AuthProvider>,
  );
}

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

beforeEach(() => {
  localStorage.clear();
  setToken(null);
});

afterEach(() => {
  cleanup();
  resetRequestSnapshots();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('inventory dashboard', () => {
  it('fetches vehicles from the vehicles API and renders each vehicle card', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp();

    expect(await screen.findByText('Toyota')).not.toBeNull();
    expect(screen.getByText('Camry')).not.toBeNull();
    expect(screen.getByText('Sedan')).not.toBeNull();
    expect(screen.getByText(/\$28,000/)).not.toBeNull();
    expect(screen.getByText(/4\s+in stock/i)).not.toBeNull();

    expect(screen.getByText('Ford')).not.toBeNull();
    expect(screen.getByText('Bronco')).not.toBeNull();
    expect(screen.getByText('SUV')).not.toBeNull();
    expect(screen.getByText(/\$41,500/)).not.toBeNull();
    expect(screen.getByText(/0\s+in stock/i)).not.toBeNull();
  });

  it('calls GET /api/vehicles with the authenticated access token', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp();

    await screen.findByText('Toyota');

    const request = getLastRequest();

    expect(request.method).toBe('GET');
    expect(new URL(request.url).pathname).toBe('/api/vehicles');
    expect(request.headers.authorization).toBe('Bearer access-token');
  });

  it('shows a loading state while vehicle inventory is being fetched', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', async () => {
        await delay(150);

        return HttpResponse.json(vehicles);
      }),
    );

    renderAuthenticatedApp();

    expect(await screen.findByText(/loading inventory/i)).not.toBeNull();
    expect(await screen.findByText('Toyota')).not.toBeNull();
    expect(screen.queryByText(/loading inventory/i)).toBeNull();
  });

  it('shows an empty inventory state when the API returns no vehicles', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json([])),
    );

    renderAuthenticatedApp();

    expect(await screen.findByText(/empty inventory/i)).not.toBeNull();
    expect(screen.getByText(/no vehicles/i)).not.toBeNull();
    expect(screen.queryByText('Toyota')).toBeNull();
  });

  it('shows an error state when the vehicles API fails', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () =>
        HttpResponse.json({ message: 'Inventory service unavailable' }, { status: 500 }),
      ),
    );

    renderAuthenticatedApp();

    expect(await screen.findByRole('alert')).not.toBeNull();
    expect(screen.getByText(/unable to load vehicles/i)).not.toBeNull();
    expect(screen.getByText(/inventory service unavailable/i)).not.toBeNull();
  });

  it('clears dashboard access and returns to the login screen after logout', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp();

    await screen.findByText('Toyota');
    fireEvent.click(screen.getByRole('button', { name: /logout/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^login$/i })).not.toBeNull();
    });
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('email')).toBeNull();
    expect(screen.queryByText('Toyota')).toBeNull();
  });
});
