import { useAuth } from '../../../features/auth/context/AuthContext';

type AppView = 'inventory' | 'purchases';

type HeaderProps = {
  currentView?: AppView;
  onViewChange?: (view: AppView) => void;
};

export function Header({ currentView = 'inventory', onViewChange }: HeaderProps) {
  const { isAdmin, logout } = useAuth();
  const showCustomerNavigation = !isAdmin && onViewChange !== undefined;

  const navButtonClass = (view: AppView) =>
    `inline-flex h-10 items-center justify-center rounded-lg px-4 text-sm font-bold transition focus:outline-none focus:ring-4 focus:ring-cyan-100 ${
      currentView === view
        ? 'bg-cyan-700 text-white shadow-sm shadow-cyan-950/20'
        : 'border border-slate-200 bg-white text-slate-700 shadow-sm hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-800'
    }`;

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-cyan-700">
            {isAdmin ? 'Dealership Admin' : 'Find Your Car'}
          </p>
          <h1 className="text-xl font-semibold">Car Dealership</h1>
        </div>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between lg:justify-end">
          <div className="flex gap-2">
            <button
              className={navButtonClass('inventory')}
              onClick={() => onViewChange?.('inventory')}
              type="button"
            >
              Inventory
            </button>
            {showCustomerNavigation ? (
              <button
                className={navButtonClass('purchases')}
                onClick={() => onViewChange('purchases')}
                type="button"
              >
                My Purchases
              </button>
            ) : null}
          </div>
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 shadow-sm transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100"
            onClick={logout}
            type="button"
          >
            <svg
              aria-hidden="true"
              className="h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M15.5 8.5 19 12m0 0-3.5 3.5M19 12H8m4-7H6.5A2.5 2.5 0 0 0 4 7.5v9A2.5 2.5 0 0 0 6.5 19H12"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
              />
            </svg>
            Logout
          </button>
        </div>
      </div>
    </header>
  );
}
