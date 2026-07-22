// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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

const filteredVehicles = [
  {
    id: 'vehicle-3',
    make: 'Honda',
    model: 'Civic',
    category: 'Sedan',
    price: 24500,
    quantityInStock: 2,
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

async function findVehicleCard(make: string) {
  const card = (await screen.findByText(make)).closest('article');

  if (!card) {
    throw new Error(`Expected ${make} to be rendered inside a vehicle card.`);
  }

  return card;
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

  it('disables the Purchase button for vehicles with zero stock', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp();

    const outOfStockVehicle = await findVehicleCard('Ford');
    const purchaseButton = within(outOfStockVehicle).getByRole('button', {
      name: /purchase/i,
    }) as HTMLButtonElement;

    expect(purchaseButton.disabled).toBe(true);
  });

  it('enables the Purchase button for vehicles with stock above zero', async () => {
    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
    );

    renderAuthenticatedApp();

    const inStockVehicle = await findVehicleCard('Toyota');
    const purchaseButton = within(inStockVehicle).getByRole('button', {
      name: /purchase/i,
    }) as HTMLButtonElement;

    expect(purchaseButton.disabled).toBe(false);
  });

  it('purchases an in-stock vehicle and renders the decremented stock returned by the API', async () => {
    let purchaseRequestAuthorization: string | null | undefined;
    let purchaseRequestMethod: string | undefined;
    let purchaseRequestUrl: string | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.post('http://localhost/api/vehicles/:id/purchase', ({ request, params }) => {
        purchaseRequestAuthorization = request.headers.get('authorization');
        purchaseRequestMethod = request.method;
        purchaseRequestUrl = request.url;

        return HttpResponse.json({
          ...vehicles[0],
          id: params.id,
          quantityInStock: 3,
        });
      }),
    );

    renderAuthenticatedApp();

    const inStockVehicle = await findVehicleCard('Toyota');
    fireEvent.click(
      within(inStockVehicle).getByRole('button', {
        name: /purchase/i,
      }),
    );

    await waitFor(() => {
      expect(within(inStockVehicle).getByText(/3\s+in stock/i)).not.toBeNull();
    });

    expect(purchaseRequestUrl).toBeDefined();
    expect(purchaseRequestMethod).toBe('POST');
    expect(new URL(purchaseRequestUrl as string).pathname).toBe('/api/vehicles/vehicle-1/purchase');
    expect(purchaseRequestAuthorization).toBe('Bearer access-token');
    expect(screen.queryByText(/purchase flow coming soon/i)).toBeNull();
  });

  it('calls GET /api/vehicles with the authenticated access token', async () => {
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

  it('searches vehicles with all populated filter query params', async () => {
    let searchRequestUrl: string | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.get('http://localhost/api/vehicles/search', ({ request }) => {
        searchRequestUrl = request.url;

        return HttpResponse.json(filteredVehicles);
      }),
    );

    renderAuthenticatedApp();

    await screen.findByText('Toyota');

    fireEvent.change(screen.getByLabelText(/make/i), { target: { value: 'Honda' } });
    fireEvent.change(screen.getByLabelText(/model/i), { target: { value: 'Civic' } });
    fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'Sedan' } });
    fireEvent.change(screen.getByLabelText(/minimum price/i), { target: { value: '20000' } });
    fireEvent.change(screen.getByLabelText(/maximum price/i), { target: { value: '30000' } });
    fireEvent.click(screen.getByRole('button', { name: /search/i }));

    await screen.findByText('Honda');

    expect(searchRequestUrl).toBeDefined();
    const searchUrl = new URL(searchRequestUrl as string);

    expect(searchUrl.pathname).toBe('/api/vehicles/search');
    expect(searchUrl.searchParams.get('make')).toBe('Honda');
    expect(searchUrl.searchParams.get('model')).toBe('Civic');
    expect(searchUrl.searchParams.get('category')).toBe('Sedan');
    expect(searchUrl.searchParams.get('minPrice')).toBe('20000');
    expect(searchUrl.searchParams.get('maxPrice')).toBe('30000');
    expect(screen.queryByText('Toyota')).toBeNull();
  });

  it('omits empty filter fields from the vehicle search query', async () => {
    let searchRequestUrl: string | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => HttpResponse.json(vehicles)),
      http.get('http://localhost/api/vehicles/search', ({ request }) => {
        searchRequestUrl = request.url;

        return HttpResponse.json(filteredVehicles);
      }),
    );

    renderAuthenticatedApp();

    await screen.findByText('Toyota');

    fireEvent.change(screen.getByLabelText(/make/i), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText(/model/i), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'Sedan' } });
    fireEvent.change(screen.getByLabelText(/minimum price/i), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText(/maximum price/i), { target: { value: '30000' } });
    fireEvent.click(screen.getByRole('button', { name: /search/i }));

    await waitFor(() => {
      expect(searchRequestUrl).toBeDefined();
    });

    const searchUrl = new URL(searchRequestUrl as string);

    expect(searchUrl.pathname).toBe('/api/vehicles/search');
    expect(searchUrl.searchParams.has('make')).toBe(false);
    expect(searchUrl.searchParams.has('model')).toBe(false);
    expect(searchUrl.searchParams.get('category')).toBe('Sedan');
    expect(searchUrl.searchParams.has('minPrice')).toBe(false);
    expect(searchUrl.searchParams.get('maxPrice')).toBe('30000');
  });

  it('clears filters and reloads all vehicles when reset is clicked', async () => {
    let vehicleListRequestCount = 0;
    let searchRequestUrl: string | undefined;

    server.use(
      http.get('http://localhost/api/vehicles', () => {
        vehicleListRequestCount += 1;

        return HttpResponse.json(vehicles);
      }),
      http.get('http://localhost/api/vehicles/search', ({ request }) => {
        searchRequestUrl = request.url;

        return HttpResponse.json(filteredVehicles);
      }),
    );

    renderAuthenticatedApp();

    await screen.findByText('Toyota');

    const makeInput = screen.getByLabelText(/make/i) as HTMLInputElement;
    const modelInput = screen.getByLabelText(/model/i) as HTMLInputElement;
    const categoryInput = screen.getByLabelText(/category/i) as HTMLInputElement;
    const minPriceInput = screen.getByLabelText(/minimum price/i) as HTMLInputElement;
    const maxPriceInput = screen.getByLabelText(/maximum price/i) as HTMLInputElement;

    fireEvent.change(makeInput, { target: { value: 'Honda' } });
    fireEvent.change(modelInput, { target: { value: 'Civic' } });
    fireEvent.change(categoryInput, { target: { value: 'Sedan' } });
    fireEvent.change(minPriceInput, { target: { value: '20000' } });
    fireEvent.change(maxPriceInput, { target: { value: '30000' } });
    fireEvent.click(screen.getByRole('button', { name: /search/i }));

    await screen.findByText('Honda');
    expect(searchRequestUrl).toBeDefined();

    fireEvent.click(screen.getByRole('button', { name: /reset/i }));

    await waitFor(() => {
      expect(vehicleListRequestCount).toBe(2);
    });

    expect(makeInput.value).toBe('');
    expect(modelInput.value).toBe('');
    expect(categoryInput.value).toBe('');
    expect(minPriceInput.value).toBe('');
    expect(maxPriceInput.value).toBe('');
    expect(await screen.findByText('Toyota')).not.toBeNull();
    expect(screen.queryByText('Honda')).toBeNull();
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
