export function Header() {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-cyan-700">
            Dealership Admin
          </p>
          <h1 className="text-xl font-semibold">Car Dealership</h1>
        </div>
        <span className="rounded-full bg-emerald-100 px-3 py-1 text-sm font-medium text-emerald-800">
          Frontend Ready
        </span>
      </div>
    </header>
  );
}
