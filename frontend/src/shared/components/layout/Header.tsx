import { useAuth } from '../../../features/auth/context/AuthContext';

export function Header() {
  const { logout } = useAuth();

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-cyan-700">
            Dealership Admin
          </p>
          <h1 className="text-xl font-semibold">Car Dealership</h1>
        </div>
        <button
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-100"
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
    </header>
  );
}
