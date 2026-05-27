export function StatTile({ label, value, caption, icon: Icon, tone = 'blue' }) {
  return (
    <section className={`stat-tile stat-tile--${tone}`}>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        {caption ? <small>{caption}</small> : null}
      </div>
      {Icon ? <Icon aria-hidden="true" size={24} /> : null}
    </section>
  )
}
