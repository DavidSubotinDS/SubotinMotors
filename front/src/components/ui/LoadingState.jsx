export default function LoadingState({ label = 'Loading' }) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <span className="loader" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
