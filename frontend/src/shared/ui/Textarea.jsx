export function Textarea({ label, error, className = '', ...props }) {
  return (
    <label className={`field ${className}`}>
      <span className="field__label">{label}</span>
      <textarea className={`textarea ${error ? 'input--error' : ''}`} {...props} />
      {error ? <span className="field__error">{error}</span> : null}
    </label>
  )
}
