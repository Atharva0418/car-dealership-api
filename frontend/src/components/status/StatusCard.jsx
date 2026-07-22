export function StatusCard({ body, className, title }) {
  return (
    <div className={`rounded-lg border p-4 shadow-sm ${className}`}>
      <h3 className="font-semibold">{title}</h3>
      <p className="mt-2 text-sm opacity-80">{body}</p>
    </div>
  );
}
