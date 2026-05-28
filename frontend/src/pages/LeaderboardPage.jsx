import { Flame, Medal, Target, Trophy } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useFlashLex } from '../features/auth/useFlashLex'
import { LeaderboardTable } from '../features/leaderboard/LeaderboardTable'
import { StatTile } from '../shared/ui/StatTile'

export function LeaderboardPage() {
  const { user, leaderboard, dailyStats, loadLeaderboard } = useFlashLex()
  const [period, setPeriod] = useState('DAY')

  useEffect(() => {
    loadLeaderboard(period)
  }, [loadLeaderboard, period])

  const sorted = [...leaderboard].sort((a, b) => b.points - a.points)
  const place = sorted.findIndex((row) => row.userId === user.id) + 1
  const leader = sorted[0]
  const currentRow = sorted.find((row) => row.userId === user.id)

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Рейтинг</span>
          <h1>{period === 'DAY' ? 'Активность за день' : 'Активность за неделю'}</h1>
          <p>Цели, дополнительные слова, точность и серия без бесконечной накрутки.</p>
        </div>
      </section>

      <div className="segmented-control" aria-label="Период рейтинга">
        <button
          className={period === 'DAY' ? 'segmented-control__item segmented-control__item--active' : 'segmented-control__item'}
          onClick={() => setPeriod('DAY')}
          type="button"
        >
          День
        </button>
        <button
          className={period === 'WEEK' ? 'segmented-control__item segmented-control__item--active' : 'segmented-control__item'}
          onClick={() => setPeriod('WEEK')}
          type="button"
        >
          Неделя
        </button>
      </div>

      <div className="stats-grid">
        <StatTile icon={Medal} label="Ваше место" value={`#${place}`} />
        <StatTile icon={Trophy} label="Лидер" tone="amber" value={leader?.name || '-'} />
        <StatTile icon={Target} label="Ваши очки" tone="green" value={currentRow?.points || 0} />
        <StatTile icon={Flame} label="Серия" tone="red" value={`${dailyStats.streakDays} дн.`} />
      </div>

      <LeaderboardTable currentUserId={user.id} period={period} rows={leaderboard} />
    </div>
  )
}
