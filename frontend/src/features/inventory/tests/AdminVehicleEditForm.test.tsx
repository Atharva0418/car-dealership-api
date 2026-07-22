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
  {
    id: 'vehicle-2',
    make: 'Ford',
    model: 'Bronco',
    category: 'SUV',
    price: 41500,
    quantityInStock: 0,
  },
];

const updatedVehicle = {
  id: 'vehicle-1',
  make: 'Honda',
  model: 'Accord',
  category: 'Sedan',
  price: 31500,
  quantityInStock: 6,
};

const vehiclesAfterUpdate = [updatedVehicle, vehicles[1]];

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

async function findVehicleCard(make: string) {
  const card = (await screen.findByText(make)).closest('article');

  if (!card) {
    throw new Error(`Expected ${make} to be rendered inside a vehicle card.`);
  }

  return card;
}

async function openEditForm(make: string) {
  const vehicleCard = await findVehicleCard(make);

  fireEvent.click(within(vehicleCard).getByRole('button', { name: /edit/i }));

  return screen.findByRole('form', { name: /edit vehicle/i });
}

function updateEditVehicleForm(form: HTMLElement) {
  fireEvent.change(within(form).getByLabelText(/^make$/i), {
    target: { value: 'Honda' },
  });
  fireEvent.change(within(form).getByLabelText(/^model$/i), {
    target: { value: 'Accord' },
  });
  fireEvent.change(within(form).getByLabelText(/^category$/i), {
    target: { value: 'Sedan' },
  });
  fireEvent.change(within(form).getByLabelText(/^price$/i), {
    target: { value: '31500' },
  });
  fireEvent.change(within(form).getByLabelText(/^quantity in stock$/i), {
    target: { value: '6' },
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

describe('admin vehicle edit form', () => {
  it('opens the edit form with the selected vehicle existing values', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await openEditForm('Toyota');

    expect((within(form).getByLabelText(/^make$/i) as HTMLInputElement).value).toBe('Toyota');
    expect((within(form).getByLabelText(/^model$/i) as HTMLInputElement).value).toBe('Camry');
    expect((within(form).getByLabelText(/^category$/i) as HTMLSelectElement).value).toBe('Sedan');
    expect((within(form).getByLabelText(/^price$/i) as HTMLInputElement).value).toBe('28000');
    expect((within(form).getByLabelText(/^quantity in stock$/i) as HTMLInputElement).value).toBe('4');
  });

  it('submits the edit form to PUT /api/vehicles/:id with the correct payload', async () => {
    let updateRequestBody: unknown;
    let updateRequestHeaders: Record<string, string> | undefined;
    let updateRequestMethod: string | undefined;
    let updateRequestUrl: string | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.put('http://localhost/api/vehicles/:id', async ({ request }) => {
        updateRequestBody = await request.json();
        updateRequestHeaders = Object.fromEntries(request.headers.entries());
        updateRequestMethod = request.method;
        updateRequestUrl = request.url;

        return HttpResponse.json(updatedVehicle);
      }),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await openEditForm('Toyota');
    updateEditVehicleForm(form);
    fireEvent.click(within(form).getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(updateRequestBody).toBeDefined();
    });

    expect(updateRequestMethod).toBe('PUT');
    expect(new URL(updateRequestUrl as string).pathname).toBe('/api/vehicles/vehicle-1');
    expect(updateRequestHeaders?.authorization).toBe('Bearer access-token');
    expect(updateRequestHeaders?.['content-type']).toContain('application/json');
    expect(updateRequestBody).toEqual({
      make: 'Honda',
      model: 'Accord',
      category: 'Sedan',
      price: 31500,
      quantityInStock: 6,
    });
  });

  it('closes the edit form without calling the API when cancel is clicked', async () => {
    let updateRequestCount = 0;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.put('http://localhost/api/vehicles/:id', () => {
        updateRequestCount += 1;

        return HttpResponse.json(updatedVehicle);
      }),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await openEditForm('Toyota');
    fireEvent.change(within(form).getByLabelText(/^price$/i), {
      target: { value: '31500' },
    });
    fireEvent.click(within(form).getByRole('button', { name: /cancel/i }));

    await waitFor(() => {
      expect(screen.queryByRole('form', { name: /edit vehicle/i })).toBeNull();
    });
    expect(updateRequestCount).toBe(0);
    expect(screen.getByText('Toyota')).not.toBeNull();
    expect(screen.getByText(/\$28,000/)).not.toBeNull();
  });

  it('refreshes the vehicle list after a vehicle is updated successfully', async () => {
    let vehicleListRequestCount = 0;

    server.use(
      http.get('http://localhost/api/vehicles', () => {
        vehicleListRequestCount += 1;

        return HttpResponse.json(
          vehicleListRequestCount === 1 ? vehicles : vehiclesAfterUpdate,
        );
      }),
      http.put('http://localhost/api/vehicles/:id', () => HttpResponse.json(updatedVehicle)),
    );

    renderAuthenticatedApp('ADMIN');

    const form = await openEditForm('Toyota');
    updateEditVehicleForm(form);
    fireEvent.click(within(form).getByRole('button', { name: /save/i }));

    expect(await screen.findByText('Honda')).not.toBeNull();
    expect(screen.getByText('Accord')).not.toBeNull();
    expect(screen.getByText(/\$31,500/)).not.toBeNull();
    expect(screen.getByText(/6\s+in stock/i)).not.toBeNull();
    expect(screen.queryByText('Toyota')).toBeNull();

    await waitFor(() => {
      expect(vehicleListRequestCount).toBe(2);
    });
  });
});
