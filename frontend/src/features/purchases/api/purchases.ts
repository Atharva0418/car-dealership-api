import { request } from '../../../shared/api/client';
import type { Purchase } from './types';

export function listMyPurchases(): Promise<Purchase[]> {
  return request<Purchase[]>('/purchases/me');
}
