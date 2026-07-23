import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '../../../shared/api/client';
import { listMyPurchases } from '../api/purchases';
import type { Purchase } from '../api/types';

type PurchasesStatus = 'loading' | 'error' | 'success';

const currencyFormatter = new Intl.NumberFormat('en-IN', {
  currency: 'INR',
  maximumFractionDigits: 0,
  style: 'currency',
});

const dateFormatter = new Intl.DateTimeFormat('en-IN', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
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

function formatPurchaseDate(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return dateFormatter.format(date);
}

function PurchaseSkeletonCard() {
  return (
    <div className="loading-panel min-h-44">
      <div className="h-4 w-24 rounded-full bg-slate-200" />
      <div className="mt-5 h-6 w-3/4 rounded bg-slate-200" />
      <div className="mt-3 h-4 w-1/2 rounded bg-slate-200" />
      <div className="mt-8 h-8 w-28 rounded bg-slate-200" />
    </div>
  );
}

function PurchaseCard({ purchase }: { purchase: Purchase }) {
  return (
    <article className="flex min-h-44 animate-[auth-panel-enter_220ms_ease-out] flex-col justify-between rounded-lg border border-slate-200 bg-white p-5 shadow-sm shadow-slate-950/5 transition duration-200 hover:-translate-y-1 hover:border-cyan-200 hover:shadow-xl hover:shadow-cyan-950/10">
      <div>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <span className="rounded-full bg-cyan-50 px-3 py-1 text-xs font-bold uppercase text-cyan-800 ring-1 ring-inset ring-cyan-100">
            {purchase.category}
          </span>
          <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700 ring-1 ring-inset ring-emerald-100">
            Purchased {formatPurchaseDate(purchase.purchasedAt)}
          </span>
        </div>

        <h2 className="mt-5 text-2xl font-bold tracking-normal text-slate-950">
          {purchase.make}
        </h2>
        <p className="mt-1 text-base font-medium text-slate-600">{purchase.model}</p>
      </div>

      <div className="mt-8 border-t border-slate-100 pt-4">
        <p className="text-xs font-semibold uppercase text-slate-500">Purchase price</p>
        <p className="mt-1 text-3xl font-bold tracking-normal text-slate-950">
          {currencyFormatter.format(purchase.price)}
        </p>
      </div>
    </article>
  );
}

export function MyPurchasesPage() {
  const [status, setStatus] = useState<PurchasesStatus>('loading');
  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchPurchases = useCallback(async () => {
    setStatus('loading');
    setErrorMessage('');

    try {
      const nextPurchases = await listMyPurchases();

      setPurchases(nextPurchases);
      setStatus('success');
    } catch (error) {
      setPurchases([]);
      setErrorMessage(getErrorMessage(error));
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    void fetchPurchases();
  }, [fetchPurchases]);

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase text-cyan-700">Purchase history</p>
          <h1 className="mt-2 text-3xl font-bold tracking-normal text-slate-950">
            Your Garage
          </h1>
        </div>
        {status === 'success' && purchases.length > 0 ? (
          <p className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm">
            {purchases.length} purchases saved
          </p>
        ) : null}
      </div>

      {status === 'loading' ? (
        <div aria-label="Loading purchases" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }, (_, index) => (
            <PurchaseSkeletonCard key={index} />
          ))}
          <span className="sr-only">Loading purchases</span>
        </div>
      ) : null}

      {status === 'error' ? (
        <div
          className="error-panel flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
          role="alert"
        >
          <div>
            <h2 className="text-base font-bold">Unable to load purchases</h2>
            <p className="mt-1 text-sm">{errorMessage}</p>
          </div>
          <button
            className="rounded-lg border border-red-200 bg-white px-4 py-2 text-sm font-bold text-red-700 shadow-sm transition hover:bg-red-100 focus:outline-none focus:ring-4 focus:ring-red-100"
            onClick={() => void fetchPurchases()}
            type="button"
          >
            Retry
          </button>
        </div>
      ) : null}

      {status === 'success' && purchases.length === 0 ? (
        <div className="flex min-h-80 animate-[auth-panel-enter_220ms_ease-out] flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white px-6 py-12 text-center shadow-sm">
          <div className="rounded-full bg-cyan-50 px-5 py-4 text-3xl font-black text-cyan-800">
            0
          </div>
          <h2 className="mt-6 text-2xl font-bold tracking-normal text-slate-950">
            No purchases yet
          </h2>
          <p className="mt-2 max-w-md text-sm leading-6 text-slate-600">
            Vehicles you purchase will appear here with the details saved from checkout.
          </p>
        </div>
      ) : null}

      {status === 'success' && purchases.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {purchases.map((purchase) => (
            <PurchaseCard key={purchase.id} purchase={purchase} />
          ))}
        </div>
      ) : null}
    </section>
  );
}
