export function Select({ label, children, error, className = '', ...props }) {
  return (
    <label className={`field ${className}`}>
      <span className="field__label">{label}</span>
      <select className={`select ${error ? 'input--error' : ''}`} {...props}>
        {children}
      </select>
      {error ? <span className="field__error">{error}</span> : null}
    </label>
  )
}
