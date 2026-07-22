type StatusCardProps = {
  body: string;
  className: string;
  title: string;
};

export function StatusCard({ body, className, title }: StatusCardProps) {
  return (
    <div className={`rounded-lg border p-4 shadow-sm ${className}`}>
      <h3 className="font-semibold">{title}</h3>
      <p className="mt-2 text-sm opacity-80">{body}</p>
    </div>
  );
}
