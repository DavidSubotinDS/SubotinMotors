export default function Alert({ title = 'Something went wrong', children, variant = 'error' }) {
  return (
    <div className={`alert alert-${variant}`} role="alert">
      <strong>{title}</strong>
      {children && <span>{children}</span>}
    </div>
  );
}
