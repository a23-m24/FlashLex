import { CalendarCheck2 } from 'lucide-react'
import { ProgressBar } from '../../shared/ui/ProgressBar'

export function DailyGoalWidget({ reviewed, learned, reviewLimit, newLimit }) {
  return (
    <section className="panel goal-widget">
      <div className="section-heading">
        <div>
          <h2>Цель на день</h2>
          <p>Повторение и новые слова.</p>
        </div>
        <CalendarCheck2 aria-hidden="true" size={24} />
      </div>
      <ProgressBar label="Повторить" max={reviewLimit} value={reviewed} />
      <ProgressBar label="Новые" max={newLimit} value={learned} />
    </section>
  )
}
