export function TagProgressChart({ tags }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>Тэги наборов</h2>
          <p>Прогресс по группам учебных наборов.</p>
        </div>
      </div>
      <div className="tag-progress-list">
        {tags.map((tag) => (
          <div className="tag-progress-row" key={tag.name}>
            <div>
              <strong>{tag.name}</strong>
              <span>
                {tag.graduated} повторяется из {tag.total}
              </span>
            </div>
            <div className="tag-progress-row__bar">
              <span style={{ width: `${tag.percent}%` }} />
            </div>
            <b>{tag.percent}%</b>
          </div>
        ))}
      </div>
    </section>
  )
}
