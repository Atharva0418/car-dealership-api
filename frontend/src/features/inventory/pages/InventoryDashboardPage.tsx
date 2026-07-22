import { FormEvent, useCallback, useEffect, useState } from 'react';

import { ApiError } from '../../../shared/api/client';
import { useAuth } from '../../auth/context/AuthContext';
import { createVehicle, listVehicles, purchaseVehicle, searchVehicles } from '../api/vehicles';
import type { VehicleSearchFilters } from '../api/vehicles';
import type { Vehicle, VehicleInput } from '../api/types';
import { VehicleFilterBar } from '../components/VehicleFilterBar';

type InventoryStatus = 'loading' | 'error' | 'success';

type VehicleAddFormState = {
  make: string;
  model: string;
  category: string;
  price: string;
  quantityInStock: string;
};

type VehicleAddFormErrors = Partial<Record<keyof VehicleAddFormState, string>>;

const initialVehicleAddFormState: VehicleAddFormState = {
  make: '',
  model: '',
  category: '',
  price: '',
  quantityInStock: '',
};

const currencyFormatter = new Intl.NumberFormat('en-US', {
  currency: 'USD',
  maximumFractionDigits: 0,
  style: 'currency',
});

function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.body && typeof error.body === 'object' && 'message' in error.body) {
      return String(error.body.message);
    }

    if (typeof error.body === 'string') {
      return error.body;
    }
  }

  return 'Please try again in a moment.';
}

function EmptyInventoryIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-14 w-14 text-cyan-700"
      fill="none"
      viewBox="0 0 48 48"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M10 29h28l-3.2-9.2A5 5 0 0 0 30.1 16H17.9a5 5 0 0 0-4.7 3.8L10 29Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2.5"
      />
      <path
        d="M8 29h32v7H8v-7Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="2.5"
      />
      <path
        d="M15 36a3 3 0 1 0 6 0M27 36a3 3 0 1 0 6 0M17 23h14"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="2.5"
      />
    </svg>
  );
}

function SkeletonCard() {
  return (
    <div className="loading-panel min-h-48">
      <div className="h-4 w-20 rounded-full bg-slate-200" />
      <div className="mt-5 h-6 w-3/4 rounded bg-slate-200" />
      <div className="mt-3 h-4 w-1/2 rounded bg-slate-200" />
      <div className="mt-8 flex items-end justify-between">
        <div className="h-8 w-28 rounded bg-slate-200" />
        <div className="h-7 w-24 rounded-full bg-slate-200" />
      </div>
    </div>
  );
}

type VehicleAddFormProps = {
  onCreated: () => Promise<void>;
};

function getRequiredError(label: string, value: string): string | undefined {
  return value.trim() ? undefined : `${label} is required`;
}

function validateVehicleAddForm(form: VehicleAddFormState): VehicleAddFormErrors {
  const errors: VehicleAddFormErrors = {
    make: getRequiredError('Make', form.make),
    model: getRequiredError('Model', form.model),
    category: getRequiredError('Category', form.category),
    price: getRequiredError('Price', form.price),
    quantityInStock: getRequiredError('Quantity in stock', form.quantityInStock),
  };

  if (!errors.price && Number(form.price) < 0) {
    errors.price = 'Price must be zero or greater';
  }

  if (!errors.quantityInStock && Number(form.quantityInStock) < 0) {
    errors.quantityInStock = 'Quantity in stock must be zero or greater';
  }

  return Object.fromEntries(
    Object.entries(errors).filter(([, message]) => message !== undefined),
  ) as VehicleAddFormErrors;
}

function buildVehicleInput(form: VehicleAddFormState): VehicleInput {
  return {
    make: form.make.trim(),
    model: form.model.trim(),
    category: form.category.trim(),
    price: Number(form.price),
    quantityInStock: Number(form.quantityInStock),
  };
}

function VehicleAddForm({ onCreated }: VehicleAddFormProps) {
  const [form, setForm] = useState<VehicleAddFormState>(initialVehicleAddFormState);
  const [errors, setErrors] = useState<VehicleAddFormErrors>({});
  const [apiError, setApiError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const updateField = (key: keyof VehicleAddFormState, value: string) => {
    setForm((currentForm) => ({
      ...currentForm,
      [key]: value,
    }));
    setErrors((currentErrors) => ({
      ...currentErrors,
      [key]: undefined,
    }));
    setApiError('');
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextErrors = validateVehicleAddForm(form);
    setErrors(nextErrors);
    setApiError('');

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      await createVehicle(buildVehicleInput(form));
      setForm(initialVehicleAddFormState);
      await onCreated();
    } catch (error) {
      setApiError(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      aria-label="Add vehicle"
      className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-950/5"
      onSubmit={(event) => void handleSubmit(event)}
    >
      <div className="flex flex-col gap-2 border-b border-slate-100 pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase text-cyan-700">Admin panel</p>
          <h2 className="mt-1 text-xl font-bold tracking-normal text-slate-950">
            Add vehicle
          </h2>
        </div>
        <button
          className="inline-flex h-11 items-center justify-center rounded-lg bg-cyan-700 px-5 text-sm font-bold text-white shadow-sm shadow-cyan-950/20 transition hover:bg-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:bg-cyan-400"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? 'Adding...' : 'Add vehicle'}
        </button>
      </div>

      {apiError ? (
        <div
          className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700"
          role="alert"
        >
          {apiError}
        </div>
      ) : null}

      <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Make</span>
          <input
            aria-invalid={errors.make ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateField('make', event.target.value)}
            placeholder="Honda"
            type="text"
            value={form.make}
          />
          {errors.make ? <p className="mt-1 text-xs font-semibold text-red-600">{errors.make}</p> : null}
        </label>

        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Model</span>
          <input
            aria-invalid={errors.model ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateField('model', event.target.value)}
            placeholder="Civic"
            type="text"
            value={form.model}
          />
          {errors.model ? <p className="mt-1 text-xs font-semibold text-red-600">{errors.model}</p> : null}
        </label>

        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Category</span>
          <select
            aria-invalid={errors.category ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateField('category', event.target.value)}
            value={form.category}
          >
            <option value="">Select category</option>
            <option value="Sedan">Sedan cars</option>
            <option value="SUV">SUVs</option>
            <option value="Hatchback">Hatchbacks</option>
          </select>
          {errors.category ? (
            <p className="mt-1 text-xs font-semibold text-red-600">{errors.category}</p>
          ) : null}
        </label>

        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Price</span>
          <input
            aria-invalid={errors.price ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            min="0"
            onChange={(event) => updateField('price', event.target.value)}
            placeholder="24500"
            type="number"
            value={form.price}
          />
          {errors.price ? <p className="mt-1 text-xs font-semibold text-red-600">{errors.price}</p> : null}
        </label>

        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Quantity in stock</span>
          <input
            aria-invalid={errors.quantityInStock ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            min="0"
            onChange={(event) => updateField('quantityInStock', event.target.value)}
            placeholder="7"
            type="number"
            value={form.quantityInStock}
          />
          {errors.quantityInStock ? (
            <p className="mt-1 text-xs font-semibold text-red-600">
              {errors.quantityInStock}
            </p>
          ) : null}
        </label>
      </div>
    </form>
  );
}

type VehicleCardProps = {
  canPurchase: boolean;
  onPurchase: (id: string) => Promise<void>;
  vehicle: Vehicle;
};

function VehicleCard({ canPurchase, onPurchase, vehicle }: VehicleCardProps) {
  const [isPurchasing, setIsPurchasing] = useState(false);
  const isOutOfStock = vehicle.quantityInStock === 0;

  const handlePurchase = async () => {
    setIsPurchasing(true);

    try {
      await onPurchase(vehicle.id);
    } finally {
      setIsPurchasing(false);
    }
  };

  return (
    <article className="group flex min-h-48 animate-[auth-panel-enter_220ms_ease-out] flex-col justify-between rounded-lg border border-slate-200 bg-white p-5 shadow-sm shadow-slate-950/5 transition duration-200 hover:-translate-y-1 hover:border-cyan-200 hover:shadow-xl hover:shadow-cyan-950/10">
      <div>
        <div className="flex items-start justify-between gap-3">
          <span className="rounded-full bg-cyan-50 px-3 py-1 text-xs font-bold uppercase text-cyan-800 ring-1 ring-inset ring-cyan-100">
            {vehicle.category}
          </span>
          <span
            className={`rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ${
              isOutOfStock
                ? 'bg-rose-50 text-rose-700 ring-rose-100'
                : 'bg-emerald-50 text-emerald-700 ring-emerald-100'
            }`}
          >
            {isOutOfStock
              ? `Out of stock - ${vehicle.quantityInStock} in stock`
              : `${vehicle.quantityInStock} in stock`}
          </span>
        </div>

        <h2 className="mt-5 text-2xl font-bold tracking-normal text-slate-950">
          {vehicle.make}
        </h2>
        <p className="mt-1 text-base font-medium text-slate-600">{vehicle.model}</p>
      </div>

      <div className="mt-8 border-t border-slate-100 pt-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase text-slate-500">Listed price</p>
            <p className="mt-1 text-3xl font-bold tracking-normal text-slate-950">
              {currencyFormatter.format(vehicle.price)}
            </p>
          </div>
          {canPurchase ? (
            <button
              className="inline-flex min-h-11 items-center justify-center rounded-lg bg-cyan-700 px-4 py-2 text-sm font-bold text-white shadow-sm shadow-cyan-950/10 transition hover:bg-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:shadow-none"
              disabled={isOutOfStock || isPurchasing}
              onClick={() => void handlePurchase()}
              type="button"
            >
              Purchase
            </button>
          ) : null}
        </div>
      </div>
    </article>
  );
}

export function InventoryDashboardPage() {
  const { isAdmin } = useAuth();
  const [status, setStatus] = useState<InventoryStatus>('loading');
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [errorMessage, setErrorMessage] = useState('');
  const [isFiltered, setIsFiltered] = useState(false);

  const fetchVehicles = useCallback(async () => {
    setStatus('loading');
    setErrorMessage('');

    try {
      const nextVehicles = await listVehicles();

      setVehicles(nextVehicles);
      setIsFiltered(false);
      setStatus('success');
    } catch (error) {
      setVehicles([]);
      setErrorMessage(getErrorMessage(error));
      setStatus('error');
    }
  }, []);

  const handleSearch = useCallback(async (filters: VehicleSearchFilters) => {
    setStatus('loading');
    setErrorMessage('');

    try {
      const nextVehicles = await searchVehicles(filters);

      setVehicles(nextVehicles);
      setIsFiltered(true);
      setStatus('success');
    } catch (error) {
      setVehicles([]);
      setErrorMessage(getErrorMessage(error));
      setStatus('error');
    }
  }, []);

  const handlePurchase = useCallback(async (id: string) => {
    const purchasedVehicle = await purchaseVehicle(id);

    setVehicles((currentVehicles) =>
      currentVehicles.map((vehicle) =>
        vehicle.id === purchasedVehicle.id ? purchasedVehicle : vehicle,
      ),
    );
  }, []);

  useEffect(() => {
    void fetchVehicles();
  }, [fetchVehicles]);

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase text-cyan-700">Inventory</p>
          <h1 className="mt-2 text-3xl font-bold tracking-normal text-slate-950">
            Vehicle Dashboard
          </h1>
          <span className="sr-only">Inventory workspace</span>
        </div>
        {status === 'success' && vehicles.length > 0 ? (
          <p className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm">
            {vehicles.length} vehicles available
          </p>
        ) : null}
      </div>

      <VehicleFilterBar
        isLoading={status === 'loading'}
        onReset={() => void fetchVehicles()}
        onSearch={(filters) => void handleSearch(filters)}
      />

      {isAdmin ? <VehicleAddForm onCreated={fetchVehicles} /> : null}

      {status === 'loading' ? (
        <div aria-label="Loading inventory" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }, (_, index) => (
            <SkeletonCard key={index} />
          ))}
          <span className="sr-only">Loading inventory</span>
        </div>
      ) : null}

      {status === 'error' ? (
        <div
          className="error-panel flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
          role="alert"
        >
          <div>
            <h2 className="text-base font-bold">Unable to load vehicles</h2>
            <p className="mt-1 text-sm">{errorMessage}</p>
          </div>
          <button
            className="rounded-lg border border-red-200 bg-white px-4 py-2 text-sm font-bold text-red-700 shadow-sm transition hover:bg-red-100 focus:outline-none focus:ring-4 focus:ring-red-100"
            onClick={() => void fetchVehicles()}
            type="button"
          >
            Retry
          </button>
        </div>
      ) : null}

      {status === 'success' && vehicles.length === 0 ? (
        <div className="flex min-h-80 animate-[auth-panel-enter_220ms_ease-out] flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white px-6 py-12 text-center shadow-sm">
          <div className="rounded-full bg-cyan-50 p-4">
            <EmptyInventoryIcon />
          </div>
          <h2 className="mt-6 text-2xl font-bold tracking-normal text-slate-950">
            Empty inventory
          </h2>
          <p className="mt-2 max-w-md text-sm leading-6 text-slate-600">
            {isFiltered
              ? 'No vehicles match the selected filters. Adjust your search to broaden the results.'
              : 'No vehicles in inventory. Add your first listing to start managing availability, pricing, and stock levels.'}
          </p>
        </div>
      ) : null}

      {status === 'success' && vehicles.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {vehicles.map((vehicle) => (
            <VehicleCard
              canPurchase={!isAdmin}
              key={vehicle.id}
              onPurchase={handlePurchase}
              vehicle={vehicle}
            />
          ))}
        </div>
      ) : null}
    </section>
  );
}
