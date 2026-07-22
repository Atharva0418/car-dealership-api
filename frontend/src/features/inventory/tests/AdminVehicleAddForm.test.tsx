// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';

import App from '../../../app/App';
import { setToken } from '../../auth/api/tokenStore';
import { AuthProvider } from '../../auth/context/AuthContext';
import { resetRequestSnapshots } from '../../../shared/api/tests/msw/handlers';
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
];

const vehiclesAfterCreate = [
  ...vehicles,
  {
    id: 'vehicle-2',
    make: 'Honda',
    model: 'Civic',
    category: 'Sedan',
    price: 24500,
    quantityInStock: 7,
  },
];

function renderAuthenticatedApp(role: 'CUSTOMER' | 'ADMIN' = 'CUSTOMER') {
  localStorage.setItem('accessToken', 'access-token');
  localStorage.setItem('refreshToken', 'refresh-token');
  localStorage.setItem('email', 'manager@example.com');
  localStorage.setItem('role', role);
  setToken('access-token');

  return render(
    <AuthProvider>
      <App />
    </AuthProvider>,
  );
}

function fillAddVehicleForm(form: HTMLElement) {
  fireEvent.change(within(form).getByLabelText(/^make$/i), {
    target: { value: 'Honda' },
  });
  fireEvent.change(within(form).getByLabelText(/^model$/i), {
    target: { value: 'Civic' },
  });
  fireEvent.change(within(form).getByLabelText(/^category$/i), {
    target: { value: 'Sedan' },
  });
  fireEvent.change(within(form).getByLabelText(/^price$/i), {
    target: { value: '24500' },
  });
  fireEvent.change(within(form).getByLabelText(/^quantity in stock$/i), {
    target: { value: '7' },
  });
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

describe('admin vehicle add form', () => {
  it('does not show the add vehicle form to non-admin users', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp('CUSTOMER');

    expect(await screen.findByText('Toyota')).not.toBeNull();
    expect(screen.queryByRole('form', { name: /add vehicle/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /add vehicle/i })).toBeNull();
  });

  it('shows the add vehicle form with required fields to admin users', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp('ADMIN');

    expect(await screen.findByText('Toyota')).not.toBeNull();

    const form = screen.getByRole('form', { name: /add vehicle/i });

    expect(within(form).getByLabelText(/^make$/i)).not.toBeNull();
    expect(within(form).getByLabelText(/^model$/i)).not.toBeNull();
    expect(within(form).getByLabelText(/^category$/i)).not.toBeNull();
    expect(within(form).getByLabelText(/^price$/i)).not.toBeNull();
    expect(within(form).getByLabelText(/^quantity in stock$/i)).not.toBeNull();
    expect(within(form).getByRole('button', { name: /add vehicle/i })).not.toBeNull();
  });

  it('submits the add vehicle form to POST /api/vehicles with the correct payload', async () => {
    let createRequestBody: unknown;
    let createRequestHeaders: Record<string, string> | undefined;
    let createRequestMethod: string | undefined;
    let createRequestUrl: string | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.post('http://localhost/api/vehicles', async ({ request }) => {
        createRequestBody = await request.json();
        createRequestHeaders = Object.fromEntries(request.headers.entries());
        createRequestMethod = request.method;
        createRequestUrl = request.url;

        return HttpResponse.json(vehiclesAfterCreate[1], { status: 201 });
      }),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await screen.findByRole('form', { name: /add vehicle/i });
    fillAddVehicleForm(form);
    fireEvent.click(within(form).getByRole('button', { name: /add vehicle/i }));

    await waitFor(() => {
      expect(createRequestBody).toBeDefined();
    });

    expect(createRequestMethod).toBe('POST');
    expect(new URL(createRequestUrl as string).pathname).toBe('/api/vehicles');
    expect(createRequestHeaders?.authorization).toBe('Bearer access-token');
    expect(createRequestHeaders?.['content-type']).toContain('application/json');
    expect(createRequestBody).toEqual({
      make: 'Honda',
      model: 'Civic',
      category: 'Sedan',
      price: 24500,
      quantityInStock: 7,
    });
  });

  it('refreshes the vehicle list after a vehicle is added successfully', async () => {
    let vehicleListRequestCount = 0;

    server.use(
      http.get('http://localhost/api/vehicles', () => {
        vehicleListRequestCount += 1;

        return HttpResponse.json(
          vehicleListRequestCount === 1 ? vehicles : vehiclesAfterCreate,
        );
      }),
      http.post('http://localhost/api/vehicles', () =>
        HttpResponse.json(vehiclesAfterCreate[1], { status: 201 }),
      ),
    );

    renderAuthenticatedApp('ADMIN');

    expect(await screen.findByText('Toyota')).not.toBeNull();

    const form = screen.getByRole('form', { name: /add vehicle/i });
    fillAddVehicleForm(form);
    fireEvent.click(within(form).getByRole('button', { name: /add vehicle/i }));

    expect(await screen.findByText('Honda')).not.toBeNull();

    await waitFor(() => {
      expect(vehicleListRequestCount).toBe(2);
    });
  });

  it('shows validation errors and does not call the API when required fields are empty', async () => {
    let createRequestCount = 0;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.post('http://localhost/api/vehicles', () => {
        createRequestCount += 1;

        return HttpResponse.json(vehiclesAfterCreate[1], { status: 201 });
      }),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await screen.findByRole('form', { name: /add vehicle/i });
    fireEvent.click(within(form).getByRole('button', { name: /add vehicle/i }));

    expect(await within(form).findByText(/make is required/i)).not.toBeNull();
    expect(within(form).getByText(/model is required/i)).not.toBeNull();
    expect(within(form).getByText(/category is required/i)).not.toBeNull();
    expect(within(form).getByText(/price is required/i)).not.toBeNull();
    expect(within(form).getByText(/quantity in stock is required/i)).not.toBeNull();
    expect(createRequestCount).toBe(0);
  });

  it('shows the API error when adding a vehicle fails', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.post('http://localhost/api/vehicles', () =>
        HttpResponse.json({ message: 'Vehicle category is not supported' }, { status: 400 }),
      ),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await screen.findByRole('form', { name: /add vehicle/i });
    fillAddVehicleForm(form);
    fireEvent.click(within(form).getByRole('button', { name: /add vehicle/i }));

    expect(await within(form).findByRole('alert')).not.toBeNull();
    expect(within(form).getByText(/vehicle category is not supported/i)).not.toBeNull();
  });
});
