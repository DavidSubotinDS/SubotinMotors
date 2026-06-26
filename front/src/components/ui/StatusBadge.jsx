export default function StatusBadge({ value }) {
  const label = String(value ?? 'Unknown');
  return <span className={`status-badge status-${label.toLowerCase().replaceAll('_', '-')}`}>{label}</span>;
}
