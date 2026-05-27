import { Flame, Medal, Target, Trophy } from 'lucide-react'
import { useFlashLex } from '../features/auth/useFlashLex'
import { LeaderboardTable } from '../features/leaderboard/LeaderboardTable'
import { StatTile } from '../shared/ui/StatTile'

export function LeaderboardPage() {
  const { user, leaderboard, dailyStats } = useFlashLex()
  const sorted = [...leaderboard].sort((a, b) => b.points - a.points)
  const place = sorted.findIndex((row) => row.userId === user.id) + 1
  const leader = sorted[0]

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Рейтинг</span>
          <h1>Активность за день</h1>
          <p>Сравнение по очкам, сериям и точности ответов.</p>
        </div>
      </section>

      <div className="stats-grid">
        <StatTile icon={Medal} label="Ваше место" value={`#${place}`} />
        <StatTile icon={Trophy} label="Лидер" tone="amber" value={leader?.name || '-'} />
        <StatTile icon={Target} label="Ваши очки" tone="green" value={dailyStats.points} />
        <StatTile icon={Flame} label="Серия" tone="red" value={`${dailyStats.streakDays} дн.`} />
      </div>

      <LeaderboardTable currentUserId={user.id} rows={leaderboard} />
    </div>
  )
}
