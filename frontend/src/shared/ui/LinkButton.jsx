import { Link } from 'react-router-dom'

export function LinkButton({
  children,
  className = '',
  variant = 'primary',
  size = 'md',
  icon: Icon,
  to,
  ...props
}) {
  return (
    <Link
      className={`button button--${variant} button--${size} ${className}`}
      to={to}
      {...props}
    >
      {Icon ? <Icon aria-hidden="true" size={18} /> : null}
      <span>{children}</span>
    </Link>
  )
}
