import { Trophy } from 'lucide-react'

export function LeaderboardTable({ rows, currentUserId, period = 'DAY' }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>{period === 'DAY' ? 'Рейтинг за день' : 'Рейтинг за неделю'}</h2>
          <p>Очки начисляются за цели, доп. слова, точность и серию.</p>
        </div>
        <Trophy aria-hidden="true" size={24} />
      </div>
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Место</th>
              <th>Пользователь</th>
              <th>Новые</th>
              <th>Повторения</th>
              <th>Доп.</th>
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
                  <td>{row.reviewed}</td>
                  <td>
                    +{row.extraNew} / +{row.extraReview}
                  </td>
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
