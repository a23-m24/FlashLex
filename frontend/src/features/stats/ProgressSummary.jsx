import { statusLabels } from '../../shared/lib/labels'

export function ProgressSummary({ summary }) {
  const total = Object.values(summary).reduce((sum, value) => sum + value, 0) || 1

  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>Статусы карточек</h2>
          <p>Распределение по алгоритму повторения.</p>
        </div>
      </div>
      <div className="status-bars">
        {Object.entries(summary).map(([status, value]) => (
          <div className="status-bar" key={status}>
            <div className="status-bar__meta">
              <span>{statusLabels[status]}</span>
              <strong>{value}</strong>
            </div>
            <div className="status-bar__track">
              <div
                className={`status-bar__fill status-bar__fill--${status.toLowerCase()}`}
                style={{ width: `${Math.round((value / total) * 100)}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
