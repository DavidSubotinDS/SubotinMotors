export default function Button({
  children,
  href,
  icon: Icon,
  type = 'button',
  variant = 'primary',
  ...props
}) {
  const className = `button button-${variant}`;
  const content = (
    <>
      {Icon && <Icon aria-hidden="true" size={17} />}
      <span>{children}</span>
    </>
  );

  if (href) {
    return (
      <a className={className} href={href} {...props}>
        {content}
      </a>
    );
  }

  return (
    <button className={className} type={type} {...props}>
      {content}
    </button>
  );
}
