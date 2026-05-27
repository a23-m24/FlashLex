import { Trophy } from 'lucide-react'

export function LeaderboardTable({ rows, currentUserId }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>Рейтинг за день</h2>
          <p>Очки начисляются за повторения и качество ответов.</p>
        </div>
        <Trophy aria-hidden="true" size={24} />
      </div>
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Место</th>
              <th>Пользователь</th>
              <th>Карточки</th>
              <th>Серия</th>
              <th>Точность</th>
              <th>Очки</th>
            </tr>
          </thead>
          <tbody>
            {[...rows]
              .sort((a, b) => b.points - a.points)
              .map((row, index) => (
                <tr
                  className={row.userId === currentUserId ? 'data-table__row--me' : ''}
                  key={row.userId}
                >
                  <td>{index + 1}</td>
                  <td>{row.name}</td>
                  <td>{row.learnedToday}</td>
                  <td>{row.streakDays} дн.</td>
                  <td>{row.accuracy}%</td>
                  <td>{row.points}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
