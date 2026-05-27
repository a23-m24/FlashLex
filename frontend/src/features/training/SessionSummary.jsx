import { CheckCircle2, RotateCcw } from 'lucide-react'
import { LinkButton } from '../../shared/ui/LinkButton'
import { StatTile } from '../../shared/ui/StatTile'

export function SessionSummary({ results }) {
  const correct = results.filter((item) => item !== 'AGAIN_0').length
  const hard = results.filter((item) => item === 'HARD_3').length
  const again = results.filter((item) => item === 'AGAIN_0').length

  return (
    <section className="panel centered-panel">
      <CheckCircle2 aria-hidden="true" className="success-icon" size={44} />
      <h1>Тренировка завершена</h1>
      <div className="stats-grid">
        <StatTile label="Ответов" value={results.length} tone="blue" />
        <StatTile label="Зачтено" value={correct} tone="green" />
        <StatTile label="Сложно" value={hard} tone="amber" />
        <StatTile label="Снова" value={again} tone="red" />
      </div>
      <div className="form-actions">
        <LinkButton icon={RotateCcw} to="/training">
          Новая тренировка
        </LinkButton>
        <LinkButton to="/stats" variant="secondary">
          Статистика
        </LinkButton>
      </div>
    </section>
  )
}
