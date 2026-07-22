import { StatusCard } from '../components/StatusCard';

const statusCards = [
  {
    title: 'Loading State',
    body: 'Neutral skeleton treatment for API-backed views.',
    className: 'border-slate-200 bg-white text-slate-700',
  },
  {
    title: 'Error State',
    body: 'Clear red styling for recoverable request failures.',
    className: 'border-red-200 bg-red-50 text-red-800',
  },
];

export function InventoryPlaceholderPage() {
  return (
    <section className="grid gap-6 lg:grid-cols-[1.5fr_1fr]">
      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-sm font-medium text-cyan-700">Placeholder Page</p>
        <h2 className="mt-2 text-3xl font-semibold tracking-normal text-slate-950">
          Inventory workspace
        </h2>
        <p className="mt-3 max-w-2xl text-slate-600">
          This React shell is ready for API-backed vehicle listing,
          authentication, and management screens.
        </p>

        <div className="mt-6 rounded-md border border-slate-200 bg-slate-50 p-4">
          <div className="flex items-center gap-3">
            <div className="h-3 w-3 rounded-full bg-cyan-500" />
            <p className="text-sm font-medium text-slate-700">
              Tailwind styles are active
            </p>
          </div>
        </div>
      </div>

      <aside className="space-y-4">
        {statusCards.map((card) => (
          <StatusCard key={card.title} {...card} />
        ))}
      </aside>
    </section>
  );
}
