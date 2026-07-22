import { request } from '../../../shared/api/client';
import type { Vehicle, VehicleInput } from './types';

type VehicleSearchFilters = {
  make?: string;
  model?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
};

function buildSearchPath(filters: VehicleSearchFilters): string {
  const searchParams = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      searchParams.set(key, String(value));
    }
  });

  const queryString = searchParams.toString();

  return queryString ? `/vehicles?${queryString}` : '/vehicles';
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
