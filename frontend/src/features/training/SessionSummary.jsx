import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, RotateCcw } from 'lucide-react'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'
import { LinkButton } from '../../shared/ui/LinkButton'

export function SessionSummary({
  answerMode,
  deckId,
  directionMode,
  remainingNewCount = 0,
  remainingReviewCount = 0,
}) {
  const navigate = useNavigate()
  const [isExtraPopupOpen, setIsExtraPopupOpen] = useState(false)
  const [newQuantity, setNewQuantity] = useState(remainingNewCount > 0 ? '1' : '0')
  const [reviewQuantity, setReviewQuantity] = useState(remainingReviewCount > 0 ? '1' : '0')
  const baseParams = `deck=${deckId}&direction=${directionMode}&answer=${answerMode}`
  const hasExtraWords = remainingNewCount > 0 || remainingReviewCount > 0
  const availableParts = [
    remainingNewCount > 0 ? `${remainingNewCount} новых` : '',
    remainingReviewCount > 0 ? `${remainingReviewCount} повторяемых` : '',
  ].filter(Boolean)

  const closeExtraRequest = () => {
    setIsExtraPopupOpen(false)
  }

  const openExtraRequest = () => {
    setNewQuantity(remainingNewCount > 0 ? '1' : '0')
    setReviewQuantity(remainingReviewCount > 0 ? '1' : '0')
    setIsExtraPopupOpen(true)
  }

  const limitedQuantity = (value, maxCount) => {
    if (maxCount <= 0) {
      return 0
    }
    const parsedQuantity = Math.floor(Number(value))
    const safeQuantity = Number.isFinite(parsedQuantity) ? parsedQuantity : 0
    return Math.min(maxCount, Math.max(0, safeQuantity))
  }

  const startExtraSession = (event) => {
    event.preventDefault()
    const extraNewLimit = limitedQuantity(newQuantity, remainingNewCount)
    const extraReviewLimit = limitedQuantity(reviewQuantity, remainingReviewCount)
    if (extraNewLimit + extraReviewLimit <= 0) {
      return
    }

    navigate(
      `/training/session?${baseParams}&queue=EXTRA_MIXED&extraNewLimit=${extraNewLimit}&extraReviewLimit=${extraReviewLimit}`,
    )
  }

  return (
    <section className="panel centered-panel">
      <CheckCircle2 aria-hidden="true" className="success-icon" size={44} />
      <h1>Тренировка завершена</h1>
      <div className="form-actions">
        {hasExtraWords ? (
          <Button onClick={openExtraRequest}>
            Учить еще ({availableParts.join(', ')})
          </Button>
        ) : null}
        <LinkButton icon={RotateCcw} to="/training" variant={hasExtraWords ? 'secondary' : 'primary'}>
          Новая тренировка
        </LinkButton>
        <LinkButton to="/stats" variant="secondary">
          Статистика
        </LinkButton>
      </div>
      {isExtraPopupOpen ? (
        <div
          className="summary-popup"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeExtraRequest()
            }
          }}
        >
          <form
            aria-labelledby="extra-session-title"
            aria-modal="true"
            className="summary-popup__panel"
            onSubmit={startExtraSession}
            role="dialog"
          >
            <h2 id="extra-session-title">Учить еще</h2>
            <p>Доступно: {availableParts.join(', ')}</p>
            {remainingNewCount > 0 ? (
              <Input
                label="Новые"
                max={remainingNewCount}
                min="0"
                onChange={(event) => setNewQuantity(event.target.value)}
                type="number"
                value={newQuantity}
              />
            ) : null}
            {remainingReviewCount > 0 ? (
              <Input
                label="Повторяемые"
                max={remainingReviewCount}
                min="0"
                onChange={(event) => setReviewQuantity(event.target.value)}
                type="number"
                value={reviewQuantity}
              />
            ) : null}
            <div className="form-actions summary-popup__actions">
              <Button onClick={closeExtraRequest} type="button" variant="ghost">
                Отмена
              </Button>
              <Button type="submit">Начать</Button>
            </div>
          </form>
        </div>
      ) : null}
    </section>
  )
}
