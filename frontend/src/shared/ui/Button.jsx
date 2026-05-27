export function Button({
  children,
  className = '',
  variant = 'primary',
  size = 'md',
  icon: Icon,
  type = 'button',
  ...props
}) {
  return (
    <button
      className={`button button--${variant} button--${size} ${className}`}
      type={type}
      {...props}
    >
      {Icon ? <Icon aria-hidden="true" size={18} /> : null}
      <span>{children}</span>
    </button>
  )
}
