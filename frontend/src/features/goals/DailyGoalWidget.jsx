import { CalendarCheck2 } from 'lucide-react'
import { ProgressBar } from '../../shared/ui/ProgressBar'

export function DailyGoalWidget({ reviewed, learned, reviewLimit, newLimit }) {
  const visibleReviewed = Math.min(reviewed, reviewLimit)
  const visibleLearned = Math.min(learned, newLimit)
  const extraReviewed = Math.max(0, reviewed - reviewLimit)
  const extraLearned = Math.max(0, learned - newLimit)

  return (
    <section className="panel goal-widget">
      <div className="section-heading">
        <div>
          <h2>Цель на день</h2>
          <p>Повторение и новые слова.</p>
        </div>
        <CalendarCheck2 aria-hidden="true" size={24} />
      </div>
      <ProgressBar extra={extraReviewed} label="Повторить" max={reviewLimit} value={visibleReviewed} />
      <ProgressBar extra={extraLearned} label="Новые" max={newLimit} value={visibleLearned} />
    </section>
  )
}
