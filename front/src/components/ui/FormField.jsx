export default function FormField({
  label,
  name,
  type = 'text',
  value,
  onChange,
  error,
  as = 'input',
  children,
  ...props
}) {
  const Control = as;
  return (
    <label className="form-field">
      <span>{label}</span>
      {children ?? (
        <Control
          name={name}
          type={type}
          value={value ?? ''}
          onChange={onChange}
          {...props}
        />
      )}
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}
