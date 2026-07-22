import { FormEvent, useState } from 'react';

import type { VehicleSearchFilters } from '../api/vehicles';

type VehicleFilterBarProps = {
  isLoading: boolean;
  onReset: () => void;
  onSearch: (filters: VehicleSearchFilters) => void;
};

type VehicleFilterFormState = {
  make: string;
  model: string;
  category: string;
  minPrice: string;
  maxPrice: string;
};

const initialFilterState: VehicleFilterFormState = {
  make: '',
  model: '',
  category: '',
  minPrice: '',
  maxPrice: '',
};

const categoryOptions = [
  { label: 'Sedan cars', value: 'Sedan' },
  { label: 'SUVs', value: 'SUV' },
  { label: 'Hatchbacks', value: 'Hatchback' },
];

function getOptionalNumber(value: string): number | undefined {
  if (!value.trim()) {
    return undefined;
  }

  const parsedValue = Number(value);

  return Number.isFinite(parsedValue) ? parsedValue : undefined;
}

export function VehicleFilterBar({ isLoading, onReset, onSearch }: VehicleFilterBarProps) {
  const [filters, setFilters] = useState<VehicleFilterFormState>(initialFilterState);

  const updateFilter = (key: keyof VehicleFilterFormState, value: string) => {
    setFilters((currentFilters) => ({
      ...currentFilters,
      [key]: value,
    }));
  };

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    onSearch({
      make: filters.make,
      model: filters.model,
      category: filters.category,
      minPrice: getOptionalNumber(filters.minPrice),
      maxPrice: getOptionalNumber(filters.maxPrice),
    });
  };

  const handleReset = () => {
    setFilters(initialFilterState);
    onReset();
  };

  return (
    <form
      aria-label="Vehicle filters"
      className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm shadow-slate-950/5"
      onSubmit={handleSearch}
    >
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1.35fr)_auto] lg:items-end">
        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Make</span>
          <input
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateFilter('make', event.target.value)}
            placeholder="Toyota"
            type="text"
            value={filters.make}
          />
        </label>

        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Model</span>
          <input
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateFilter('model', event.target.value)}
            placeholder="Camry"
            type="text"
            value={filters.model}
          />
        </label>

        <label className="block">
          <span className="text-xs font-bold uppercase text-slate-500">Category</span>
          <select
            className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onChange={(event) => updateFilter('category', event.target.value)}
            value={filters.category}
          >
            <option value="">Any category</option>
            {categoryOptions.map((category) => (
              <option key={category.value} value={category.value}>
                {category.label}
              </option>
            ))}
          </select>
        </label>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label className="block">
            <span className="text-xs font-bold uppercase text-slate-500">Minimum price</span>
            <input
              className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
              min="0"
              onChange={(event) => updateFilter('minPrice', event.target.value)}
              placeholder="20000"
              type="number"
              value={filters.minPrice}
            />
          </label>

          <label className="block">
            <span className="text-xs font-bold uppercase text-slate-500">Maximum price</span>
            <input
              className="mt-2 h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-medium text-slate-900 transition focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-cyan-100"
              min="0"
              onChange={(event) => updateFilter('maxPrice', event.target.value)}
              placeholder="50000"
              type="number"
              value={filters.maxPrice}
            />
          </label>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row lg:justify-end">
          <button
            className="inline-flex h-11 items-center justify-center rounded-lg bg-cyan-700 px-5 text-sm font-bold text-white shadow-sm shadow-cyan-950/20 transition hover:bg-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:bg-cyan-400"
            disabled={isLoading}
            type="submit"
          >
            {isLoading ? 'Searching...' : 'Search'}
          </button>
          <button
            className="inline-flex h-11 items-center justify-center rounded-lg border border-slate-200 bg-white px-5 text-sm font-bold text-slate-700 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isLoading}
            onClick={handleReset}
            type="button"
          >
            Reset
          </button>
        </div>
      </div>
    </form>
  );
}
