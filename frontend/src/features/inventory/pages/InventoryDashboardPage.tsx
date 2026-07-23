import { FormEvent, useCallback, useEffect, useState } from 'react';

import { ApiError } from '../../../shared/api/client';
import { useAuth } from '../../auth/context/AuthContext';
import {
  createVehicle,
  deleteVehicle,
  listVehicles,
  purchaseVehicle,
  searchVehicles,
  updateVehicle,
} from '../api/vehicles';
import type { VehicleSearchFilters } from '../api/vehicles';
import type { Vehicle, VehicleInput } from '../api/types';
import { VehicleFilterBar } from '../components/VehicleFilterBar';

type InventoryStatus = 'loading' | 'error' | 'success';

type VehicleFormState = {
  make: string;
  model: string;
  category: string;
  price: string;
  quantityInStock: string;
};

type VehicleFormErrors = Partial<Record<keyof VehicleFormState, string>>;

const initialVehicleAddFormState: VehicleFormState = {
  make: '',
  model: '',
  category: '',
  price: '',
  quantityInStock: '',
};

const currencyFormatter = new Intl.NumberFormat('en-IN', {
  currency: 'INR',
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

function validateVehicleForm(form: VehicleFormState): VehicleFormErrors {
  const errors: VehicleFormErrors = {
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
  ) as VehicleFormErrors;
}

function buildVehicleInput(form: VehicleFormState): VehicleInput {
  return {
    make: form.make.trim(),
    model: form.model.trim(),
    category: form.category.trim(),
    price: Number(form.price),
    quantityInStock: Number(form.quantityInStock),
  };
}

function buildVehicleFormState(vehicle: Vehicle): VehicleFormState {
  return {
    make: vehicle.make,
    model: vehicle.model,
    category: vehicle.category,
    price: String(vehicle.price),
    quantityInStock: String(vehicle.quantityInStock),
  };
}

function VehicleAddForm({ onCreated }: VehicleAddFormProps) {
  const [form, setForm] = useState<VehicleFormState>(initialVehicleAddFormState);
  const [errors, setErrors] = useState<VehicleFormErrors>({});
  const [apiError, setApiError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const updateField = (key: keyof VehicleFormState, value: string) => {
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

    const nextErrors = validateVehicleForm(form);
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
            <option value="Sedan">Sedans</option>
            <option value="SUV">SUVs</option>
            <option value="Hatchback">Hatchbacks</option>
            <option value="Supercars">Supercars</option>
            <option value="Sports cars">Sports cars</option>
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
  canDelete: boolean;
  canEdit: boolean;
  canPurchase: boolean;
  onDelete: (vehicle: Vehicle) => Promise<void>;
  onEdit: (vehicle: Vehicle) => void;
  onPurchase: (id: string) => Promise<void>;
  vehicle: Vehicle;
};

type VehicleEditFormProps = {
  onCancel: () => void;
  onUpdated: () => Promise<void>;
  vehicle: Vehicle;
};

function VehicleEditForm({ onCancel, onUpdated, vehicle }: VehicleEditFormProps) {
  const [form, setForm] = useState<VehicleFormState>(() => buildVehicleFormState(vehicle));
  const [errors, setErrors] = useState<VehicleFormErrors>({});
  const [apiError, setApiError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const updateField = (key: keyof VehicleFormState, value: string) => {
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

    const nextErrors = validateVehicleForm(form);
    setErrors(nextErrors);
    setApiError('');

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      await updateVehicle(vehicle.id, buildVehicleInput(form));
      await onUpdated();
    } catch (error) {
      setApiError(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      aria-label="Edit vehicle"
      className="animate-[auth-panel-enter_220ms_ease-out] rounded-lg border border-cyan-200 bg-white p-5 shadow-xl shadow-cyan-950/10"
      onSubmit={(event) => void handleSubmit(event)}
    >
      <div className="flex flex-col gap-3 border-b border-slate-100 pb-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-xs font-bold uppercase text-cyan-700">Editing vehicle</p>
          <h2 className="mt-1 text-2xl font-bold tracking-normal text-slate-950">
            {vehicle.make} {vehicle.model}
          </h2>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <button
            className="inline-flex min-h-11 items-center justify-center rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-4 focus:ring-slate-100 disabled:cursor-not-allowed disabled:opacity-70"
            disabled={isSubmitting}
            onClick={onCancel}
            type="button"
          >
            Cancel
          </button>
          <button
            className="inline-flex min-h-11 items-center justify-center rounded-lg bg-cyan-700 px-4 py-2 text-sm font-bold text-white shadow-sm shadow-cyan-950/10 transition hover:bg-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:bg-cyan-400"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>

      {apiError ? (
        <div
          className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700"
          role="alert"
        >
          {apiError}
        </div>
      ) : null}

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Make</span>
          <input
            aria-invalid={errors.make ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateField('make', event.target.value)}
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
            <option value="Sedan">Sedans</option>
            <option value="SUV">SUVs</option>
            <option value="Hatchback">Hatchbacks</option>
            <option value="Supercars">Supercars</option>
            <option value="Sports cars">Sports cars</option>
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
            type="number"
            value={form.price}
          />
          {errors.price ? <p className="mt-1 text-xs font-semibold text-red-600">{errors.price}</p> : null}
        </label>

        <label className="block sm:col-span-2">
          <span className="text-xs font-bold uppercase text-slate-500">Quantity in stock</span>
          <input
            aria-invalid={errors.quantityInStock ? 'true' : 'false'}
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            min="0"
            onChange={(event) => updateField('quantityInStock', event.target.value)}
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

function VehicleCard({
  canDelete,
  canEdit,
  canPurchase,
  onDelete,
  onEdit,
  onPurchase,
  vehicle,
}: VehicleCardProps) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [isPurchasing, setIsPurchasing] = useState(false);
  const isOutOfStock = vehicle.quantityInStock === 0;

  const handleDelete = async () => {
    const confirmed = window.confirm(
      `Delete ${vehicle.make} ${vehicle.model} from inventory?`,
    );

    if (!confirmed) {
      return;
    }

    setIsDeleting(true);

    try {
      await onDelete(vehicle);
    } finally {
      setIsDeleting(false);
    }
  };

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
        <div className="flex flex-col gap-4 sm:flex-row sm:flex-wrap sm:items-end sm:justify-between">
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase text-slate-500">Listed price</p>
            <p className="mt-1 break-words text-3xl font-bold tracking-normal text-slate-950">
              {currencyFormatter.format(vehicle.price)}
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:justify-end">
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
            {canEdit ? (
              <button
                className="inline-flex min-h-11 items-center justify-center rounded-lg border border-cyan-200 bg-white px-4 py-2 text-sm font-bold text-cyan-800 shadow-sm transition hover:bg-cyan-50 focus:outline-none focus:ring-4 focus:ring-cyan-100"
                onClick={() => onEdit(vehicle)}
                type="button"
              >
                Edit
              </button>
            ) : null}
            {canDelete ? (
              <button
                className="inline-flex min-h-11 items-center justify-center rounded-lg border border-red-200 bg-white px-4 py-2 text-sm font-bold text-red-700 shadow-sm transition hover:bg-red-50 focus:outline-none focus:ring-4 focus:ring-red-100 disabled:cursor-not-allowed disabled:opacity-70"
                disabled={isDeleting}
                onClick={() => void handleDelete()}
                type="button"
              >
                {isDeleting ? 'Deleting...' : 'Delete'}
              </button>
            ) : null}
          </div>
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
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('');
  const [isFiltered, setIsFiltered] = useState(false);
  const [editingVehicleId, setEditingVehicleId] = useState<string | null>(null);

  const fetchVehicles = useCallback(async () => {
    setStatus('loading');
    setErrorMessage('');
    setDeleteErrorMessage('');

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
    setDeleteErrorMessage('');

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

  const handleDelete = useCallback(async (vehicle: Vehicle) => {
    setDeleteErrorMessage('');

    try {
      await deleteVehicle(vehicle.id);
      setVehicles((currentVehicles) =>
        currentVehicles.filter((currentVehicle) => currentVehicle.id !== vehicle.id),
      );
    } catch (error) {
      setDeleteErrorMessage(getErrorMessage(error));
    }
  }, []);

  const handleVehicleUpdated = useCallback(async () => {
    await fetchVehicles();
    setEditingVehicleId(null);
  }, [fetchVehicles]);

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

      {deleteErrorMessage ? (
        <div
          className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700 shadow-sm"
          role="alert"
        >
          {deleteErrorMessage}
        </div>
      ) : null}

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
            editingVehicleId === vehicle.id ? (
              <VehicleEditForm
                key={vehicle.id}
                onCancel={() => setEditingVehicleId(null)}
                onUpdated={handleVehicleUpdated}
                vehicle={vehicle}
              />
            ) : (
              <VehicleCard
                canDelete={isAdmin}
                canEdit={isAdmin}
                canPurchase={!isAdmin}
                key={vehicle.id}
                onDelete={handleDelete}
                onEdit={(nextVehicle) => setEditingVehicleId(nextVehicle.id)}
                onPurchase={handlePurchase}
                vehicle={vehicle}
              />
            )
          ))}
        </div>
      ) : null}
    </section>
  );
}
