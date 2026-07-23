// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';

import App from '../App';
import { AuthProvider } from '../../features/auth/context/AuthContext';
import { setToken } from '../../features/auth/api/tokenStore';
import { server } from '../../shared/api/tests/msw/server';

const vehicles = [
  {
    id: 'vehicle-1',
    make: 'Toyota',
    model: 'Camry',
    category: 'Sedan',
    price: 28000,
    quantityInStock: 4,
  },
];

const purchases = [
  {
    id: 'purchase-1',
    vehicleId: 'vehicle-1',
    make: 'Toyota',
    model: 'Camry',
    category: 'Sedan',
    price: 28000,
    purchasedAt: '2026-07-23T05:00:00Z',
  },
];

function renderAuthenticatedApp(role: 'CUSTOMER' | 'ADMIN' = 'CUSTOMER') {
  localStorage.setItem('accessToken', 'access-token');
  localStorage.setItem('refreshToken', 'refresh-token');
  localStorage.setItem('email', 'customer@example.com');
  localStorage.setItem('role', role);
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
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('my purchases navigation', () => {
  it('shows Inventory and My Purchases navigation buttons for customers', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp('CUSTOMER');

    expect(await screen.findByText('Toyota')).not.toBeNull();
    expect(screen.getByRole('button', { name: /^inventory$/i })).not.toBeNull();
    expect(screen.getByRole('button', { name: /my purchases/i })).not.toBeNull();
  });

  it('does not show My Purchases navigation for admins', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp('ADMIN');

    expect(await screen.findByText('Toyota')).not.toBeNull();
    expect(screen.getByRole('button', { name: /^inventory$/i })).not.toBeNull();
    expect(screen.queryByRole('button', { name: /my purchases/i })).toBeNull();
  });

  it('fetches and displays persisted purchase history when a customer opens My Purchases', async () => {
    let purchasesRequestUrl: string | undefined;
    let purchasesRequestAuthorization: string | null | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.get('http://localhost/api/purchases/me', ({ request }) => {
        purchasesRequestUrl = request.url;
        purchasesRequestAuthorization = request.headers.get('authorization');

        return HttpResponse.json(purchases);
      }),
    );

    renderAuthenticatedApp('CUSTOMER');

    await screen.findByText('Toyota');
    fireEvent.click(screen.getByRole('button', { name: /my purchases/i }));

    expect(await screen.findByText(/my purchases/i)).not.toBeNull();
    expect(screen.getByText('Camry')).not.toBeNull();
    expect(screen.getByText('Sedan')).not.toBeNull();
    expect(screen.getByText(/28,000/)).not.toBeNull();
    expect(purchasesRequestUrl).toBeDefined();
    expect(new URL(purchasesRequestUrl as string).pathname).toBe('/api/purchases/me');
    expect(purchasesRequestAuthorization).toBe('Bearer access-token');
  });

  it('shows an empty state when the customer has no purchases', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.get('http://localhost/api/purchases/me', () => HttpResponse.json([])),
    );

    renderAuthenticatedApp('CUSTOMER');

    await screen.findByText('Toyota');
    fireEvent.click(screen.getByRole('button', { name: /my purchases/i }));

    expect(await screen.findByText(/no purchases/i)).not.toBeNull();
  });

  it('shows an error state when purchase history cannot be loaded', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.get('http://localhost/api/purchases/me', () =>
        HttpResponse.json({ message: 'Purchase history unavailable' }, { status: 500 }),
      ),
    );

    renderAuthenticatedApp('CUSTOMER');

    await screen.findByText('Toyota');
    fireEvent.click(screen.getByRole('button', { name: /my purchases/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).not.toBeNull();
    });
    expect(screen.getByText(/purchase history unavailable/i)).not.toBeNull();
  });
});
