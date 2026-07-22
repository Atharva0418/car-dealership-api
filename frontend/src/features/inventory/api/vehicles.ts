import { request } from '../../../shared/api/client';
import type { Vehicle, VehicleInput } from './types';

export type VehicleSearchFilters = {
  make?: string;
  model?: string;
  category?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
};

export function buildVehicleSearchParams(filters: VehicleSearchFilters): URLSearchParams {
  const searchParams = new URLSearchParams();

  const setTextParam = (key: 'make' | 'model' | 'category', value?: string) => {
    const nextValue = value?.trim();

    if (nextValue) {
      searchParams.set(key, nextValue);
    }
  };

  const setNumberParam = (key: 'minPrice' | 'maxPrice', value?: number | null) => {
    if (typeof value === 'number' && Number.isFinite(value)) {
      searchParams.set(key, String(value));
    }
  };

  setTextParam('make', filters.make);
  setTextParam('model', filters.model);
  setTextParam('category', filters.category);
  setNumberParam('minPrice', filters.minPrice);
  setNumberParam('maxPrice', filters.maxPrice);

  return searchParams;
}

function buildSearchPath(filters: VehicleSearchFilters): string {
  const searchParams = buildVehicleSearchParams(filters);
  const queryString = searchParams.toString();

  return queryString ? `/vehicles/search?${queryString}` : '/vehicles/search';
}

export function listVehicles(): Promise<Vehicle[]> {
  return request<Vehicle[]>('/vehicles');
}

export function searchVehicles(filters: VehicleSearchFilters): Promise<Vehicle[]> {
  return request<Vehicle[]>(buildSearchPath(filters));
}

export function createVehicle(payload: VehicleInput): Promise<Vehicle> {
  return request<Vehicle>('/vehicles', {
    method: 'POST',
    body: payload,
  });
}

export function updateVehicle(id: string, payload: Partial<VehicleInput>): Promise<Vehicle> {
  return request<Vehicle>(`/vehicles/${id}`, {
    method: 'PUT',
    body: payload,
  });
}

export function deleteVehicle(id: string): Promise<void> {
  return request<void>(`/vehicles/${id}`, {
    method: 'DELETE',
  });
}

export function purchaseVehicle(id: string): Promise<Vehicle> {
  return request<Vehicle>(`/vehicles/${id}/purchase`, {
    method: 'POST',
  });
}
