export function ProgressBar({ value, max = 100, label, extra = 0 }) {
  const percent = max ? Math.min(100, Math.round((value / max) * 100)) : 0

  return (
    <div className="progress">
      <div className="progress__meta">
        <span>{label}</span>
        <strong>
          {value}/{max}
          {extra > 0 ? ` + ${extra} доп.` : ''}
        </strong>
      </div>
      <div className="progress__track" aria-hidden="true">
        <div className="progress__fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
