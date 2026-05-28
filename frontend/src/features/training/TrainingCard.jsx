import { Eye, Lightbulb } from 'lucide-react'
import { useMemo, useState } from 'react'
import { difficultyLabels, phraseTypeLabels } from '../../shared/lib/labels'
import { getReviewOptions } from '../../shared/lib/review'
import { Badge } from '../../shared/ui/Badge'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'

const directionLabels = {
  DIRECT_TRANSLATION: 'Прямой перевод',
  REVERSE_TRANSLATION: 'Обратный перевод',
}

const answerLabels = {
  TEXT_INPUT: 'Ввод',
  SELF_CHECK: 'Самопроверка',
}

const getPrompt = (card, directionMode) => {
  if (directionMode === 'REVERSE_TRANSLATION') {
    return card.translation
  }
  return card.englishWord
}

const getAnswer = (card, directionMode) => {
  if (directionMode === 'REVERSE_TRANSLATION') {
    return card.englishWord
  }
  return card.translation
}

const normalizeTextAnswer = (value) =>
  String(value || '')
    .toLowerCase()
    .replace(/ё/g, 'е')
    .replace(/[.,!?;:()[\]{}"«»'`´]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

export function TrainingCard({
  answerMode,
  answerOptions = [],
  card,
  cardProgress,
  directionMode,
  isSubmitting = false,
  onAnswer,
}) {
  const [revealed, setRevealed] = useState(false)
  const [textAnswer, setTextAnswer] = useState('')
  const [isChecked, setIsChecked] = useState(false)
  const prompt = getPrompt(card, directionMode)
  const answer = getAnswer(card, directionMode)
  const normalizedAnswer = normalizeTextAnswer(answer)
  const typedAnswer = normalizeTextAnswer(textAnswer)
  const hasTypedAnswer = answerMode === 'TEXT_INPUT' && Boolean(typedAnswer)
  const canGrade = answerMode === 'SELF_CHECK' ? revealed : isChecked
  const isTextMatch = hasTypedAnswer && normalizedAnswer === typedAnswer
  const reviewOptions = useMemo(
    () => getReviewOptions(cardProgress, answerOptions),
    [answerOptions, cardProgress],
  )

  return (
    <section className="training-card">
      <div className="training-card__meta">
        <Badge tone="blue">{directionLabels[directionMode]}</Badge>
        <Badge tone="green">{answerLabels[answerMode]}</Badge>
        <Badge tone="neutral">{phraseTypeLabels[card.phraseType]}</Badge>
        <Badge tone={card.difficulty === 'hard' ? 'red' : 'green'}>
          {difficultyLabels[card.difficulty]}
        </Badge>
      </div>

      <div className="training-card__prompt">
        <span>{phraseTypeLabels[card.phraseType]}</span>
        <h2>{prompt}</h2>
        {directionMode !== 'REVERSE_TRANSLATION' && card.transcription ? (
          <p>{card.transcription}</p>
        ) : null}
      </div>

      {answerMode === 'TEXT_INPUT' ? (
        <Input
          label="Ответ"
          onChange={(event) => {
            setTextAnswer(event.target.value)
            setIsChecked(false)
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              if (hasTypedAnswer) {
                setIsChecked(true)
              }
            }
          }}
          placeholder={
            directionMode === 'REVERSE_TRANSLATION'
              ? 'Введите английский вариант'
              : 'Введите перевод'
          }
          value={textAnswer}
        />
      ) : null}

      {answerMode === 'TEXT_INPUT' && !isChecked ? (
        <Button disabled={!hasTypedAnswer} onClick={() => setIsChecked(true)} variant="secondary">
          Проверить
        </Button>
      ) : null}

      {answerMode === 'SELF_CHECK' && !revealed ? (
        <Button icon={Eye} onClick={() => setRevealed(true)} variant="secondary">
          Показать
        </Button>
      ) : null}

      {revealed || isChecked ? (
        <div className="training-card__answer">
          <Lightbulb aria-hidden="true" size={22} />
          <div>
            <strong>{answer}</strong>
            <p>{card.exampleSentence}</p>
            {hasTypedAnswer ? (
              <span className={isTextMatch ? 'text-success' : 'text-warning'}>
                {isTextMatch ? 'Правильно' : 'Неправильно'}
              </span>
            ) : null}
          </div>
        </div>
      ) : null}

      {canGrade ? (
        <div className="quality-row">
          {reviewOptions.map((option) => (
            <Button
              disabled={isSubmitting}
              key={option.quality}
              onClick={() => onAnswer(option.quality)}
              variant={option.variant}
            >
              {option.label} · {option.time}
            </Button>
          ))}
        </div>
      ) : null}
    </section>
  )
}
