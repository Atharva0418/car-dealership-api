import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';

import { setToken } from '../../auth/api/tokenStore';
import {
  createVehicle,
  deleteVehicle,
  listVehicles,
  searchVehicles,
  updateVehicle,
} from '../api/vehicles';
import { getLastRequest, resetRequestSnapshots } from '../../../shared/api/tests/msw/handlers';
import { server } from '../../../shared/api/tests/msw/server';

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

describe('vehicles API', () => {
  it('listVehicles calls GET /api/vehicles and includes auth header when token is set', async () => {
    setToken('access-token');

    await listVehicles();

    const request = getLastRequest();

    expect(request.method).toBe('GET');
    expect(new URL(request.url).pathname).toBe('/api/vehicles');
    expect(request.headers.authorization).toBe('Bearer access-token');
  });

  it('searchVehicles builds correct query string from provided filters', async () => {
    await searchVehicles({
      make: 'Toyota',
      model: 'Camry',
      category: 'sedan',
      minPrice: 20000,
      maxPrice: 35000,
    });

    const url = new URL(getLastRequest().url);

    expect(url.pathname).toBe('/api/vehicles');
    expect(url.searchParams.get('make')).toBe('Toyota');
    expect(url.searchParams.get('model')).toBe('Camry');
    expect(url.searchParams.get('category')).toBe('sedan');
    expect(url.searchParams.get('minPrice')).toBe('20000');
    expect(url.searchParams.get('maxPrice')).toBe('35000');
  });

  it('searchVehicles omits query params for filters that are undefined or empty', async () => {
    await searchVehicles({
      make: '',
      model: undefined,
      category: 'suv',
      minPrice: undefined,
      maxPrice: 45000,
    });

    const url = new URL(getLastRequest().url);

    expect(url.searchParams.has('make')).toBe(false);
    expect(url.searchParams.has('model')).toBe(false);
    expect(url.searchParams.get('category')).toBe('suv');
    expect(url.searchParams.has('minPrice')).toBe(false);
    expect(url.searchParams.get('maxPrice')).toBe('45000');
  });

  it('createVehicle sends POST /api/vehicles with JSON body', async () => {
    const payload = {
      make: 'Toyota',
      model: 'Camry',
      price: 28000,
      category: 'sedan',
      quantityInStock: 4,
    };

    await createVehicle(payload);

    const request = getLastRequest();

    expect(request.method).toBe('POST');
    expect(new URL(request.url).pathname).toBe('/api/vehicles');
    expect(request.headers['content-type']).toBe('application/json');
    expect(request.body).toEqual(payload);
  });

  it('updateVehicle sends PUT /api/vehicles/:id with JSON body', async () => {
    const payload = {
      price: 29000,
    };

    await updateVehicle('vehicle-1', payload);

    const request = getLastRequest();

    expect(request.method).toBe('PUT');
    expect(new URL(request.url).pathname).toBe('/api/vehicles/vehicle-1');
    expect(request.headers['content-type']).toBe('application/json');
    expect(request.body).toEqual(payload);
  });

  it('deleteVehicle sends DELETE /api/vehicles/:id', async () => {
    await deleteVehicle('vehicle-1');

    const request = getLastRequest();

    expect(request.method).toBe('DELETE');
    expect(new URL(request.url).pathname).toBe('/api/vehicles/vehicle-1');
  });
});
