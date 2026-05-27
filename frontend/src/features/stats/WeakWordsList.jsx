import { AlertTriangle } from 'lucide-react'
import { Badge } from '../../shared/ui/Badge'

export function WeakWordsList({ items }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>Слабые карточки</h2>
          <p>Слова с частыми ошибками.</p>
        </div>
        <AlertTriangle aria-hidden="true" size={22} />
      </div>
      <div className="compact-list">
        {items.length ? (
          items.map(({ card, wrongRate }) => (
            <div className="compact-row" key={card.id}>
              <div>
                <strong>{card.englishWord}</strong>
                <span>{card.translation}</span>
              </div>
              <Badge tone="red">{Math.round(wrongRate * 100)}% ошибок</Badge>
            </div>
          ))
        ) : (
          <p className="muted">Нет карточек с повышенной ошибочностью.</p>
        )}
      </div>
    </section>
  )
}
