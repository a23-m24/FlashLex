import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { SessionSummary } from '../features/training/SessionSummary'
import { TrainingCard } from '../features/training/TrainingCard'
import { isDue } from '../shared/lib/date'
import { getProgress } from '../shared/lib/metrics'
import { Button } from '../shared/ui/Button'
import { EmptyState } from '../shared/ui/EmptyState'
import { LinkButton } from '../shared/ui/LinkButton'

const isLearningProgress = (cardProgress) =>
  cardProgress?.status === 'LEARNING' || cardProgress?.status === 'RELEARNING'

const isReviewProgress = (cardProgress) =>
  cardProgress?.status === 'REVIEW' || cardProgress?.status === 'GRADUATED'

const reviewTime = (cardProgress) => {
  const rawValue = cardProgress?.nextReviewAt || cardProgress?.nextReviewDate
  return rawValue ? new Date(rawValue).getTime() : 0
}

export function TrainingSessionPage() {
  const [searchParams] = useSearchParams()
  const { user, decks, flashcards, progress, answerCard, getNextTrainingCard } = useFlashLex()
  const fallbackDeck = decks.find((deck) => deck.authorId === user.id)
  const deckId = searchParams.get('deck') || fallbackDeck?.id || ''
  const directionMode = searchParams.get('direction') || 'DIRECT_TRANSLATION'
  const answerMode = searchParams.get('answer') || 'SELF_CHECK'
  const deck = decks.find((item) => item.id === deckId)
  const [results, setResults] = useState([])
  const [entry, setEntry] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isAnswering, setIsAnswering] = useState(false)
  const [answerVersion, setAnswerVersion] = useState(0)
  const [error, setError] = useState('')
  const [localProgressOverrides, setLocalProgressOverrides] = useState({})

  const visibleProgress = useMemo(() => {
    const progressByCardId = new Map(progress.map((item) => [item.flashcardId, item]))
    Object.values(localProgressOverrides).forEach((item) => {
      progressByCardId.set(item.flashcardId, item)
    })
    return [...progressByCardId.values()]
  }, [localProgressOverrides, progress])

  const queueState = useMemo(() => {
    const deckCards = flashcards.filter((card) => card.deckId === deckId)
    const progressByCardId = new Map(visibleProgress.map((item) => [item.flashcardId, item]))

    if (!deckCards.length) {
      return {
        counts: {
          newCount: entry?.newCount || 0,
          learningCount: entry?.learningCount || 0,
          reviewCount: entry?.reviewCount || 0,
        },
        localNextEntry: null,
      }
    }

    let newCount = 0
    let learningCount = 0
    let reviewCount = 0
    const newItems = []
    const learningItems = []
    const dueReviewItems = []

    deckCards.forEach((card) => {
      const cardProgress = progressByCardId.get(card.id)
      if (!cardProgress) {
        newCount += 1
        newItems.push({ card, progress: null })
      } else if (isLearningProgress(cardProgress)) {
        learningCount += 1
        learningItems.push({ card, progress: cardProgress })
      } else if (isReviewProgress(cardProgress) && isDue(cardProgress.nextReviewDate || cardProgress.nextReviewAt)) {
        reviewCount += 1
        dueReviewItems.push({ card, progress: cardProgress })
      }
    })

    dueReviewItems.sort(
      (left, right) =>
        reviewTime(left.progress) - reviewTime(right.progress) ||
        Number(left.card.id) - Number(right.card.id),
    )
    learningItems.sort(
      (left, right) =>
        reviewTime(left.progress) - reviewTime(right.progress) ||
        Number(left.card.id) - Number(right.card.id),
    )

    const localNextItem = dueReviewItems[0] || newItems[0] || learningItems[0]
    const localNextEntry = localNextItem
      ? {
          deckId,
          card: localNextItem.card,
          progress: localNextItem.progress,
          finished: false,
          dueNowCount: reviewCount,
          newCount,
          learningCount,
          reviewCount,
        }
      : null

    return {
      counts: { newCount, learningCount, reviewCount },
      localNextEntry,
    }
  }, [deckId, entry?.learningCount, entry?.newCount, entry?.reviewCount, flashcards, visibleProgress])

  const bufferCounts = queueState.counts
  const activeEntry = entry?.finished && queueState.localNextEntry
    ? queueState.localNextEntry
    : entry

  const loadNext = useCallback(async () => {
    if (!deckId) {
      setEntry(null)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError('')
    try {
      setEntry(await getNextTrainingCard(deckId))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsLoading(false)
    }
  }, [deckId, getNextTrainingCard])

  useEffect(() => {
    loadNext()
  }, [loadNext])

  if (!deck) {
    return (
      <EmptyState
        action={<LinkButton to="/">К наборам</LinkButton>}
        text="Выберите другой набор или создайте карточки."
        title="Набор не найден"
      />
    )
  }

  if (isLoading && !entry) {
    return (
      <EmptyState
        action={<LinkButton to="/">К наборам</LinkButton>}
        text="Подбираем следующую карточку по очереди повторений."
        title="Загрузка тренировки"
      />
    )
  }

  if (error) {
    return (
      <EmptyState
        action={<Button onClick={loadNext}>Повторить</Button>}
        text={error}
        title="Не удалось загрузить тренировку"
      />
    )
  }

  if (activeEntry?.finished) {
    if (!results.length) {
      return (
        <EmptyState
          action={<LinkButton to="/">К наборам</LinkButton>}
          text="Новых карточек и повторений на сегодня нет."
          title="Тренировка на сегодня закрыта"
        />
      )
    }

    return <SessionSummary results={results} />
  }

  const currentCard = activeEntry.card
  const handleAnswer = async (quality) => {
    if (isAnswering) {
      return
    }

    setIsAnswering(true)
    setError('')
    try {
      const updatedProgress = await answerCard(currentCard.id, quality)
      setLocalProgressOverrides((current) => ({
        ...current,
        [updatedProgress.flashcardId]: updatedProgress,
      }))
      setResults((current) => [...current, quality])
      setEntry(await getNextTrainingCard(deckId))
      setAnswerVersion((current) => current + 1)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsAnswering(false)
    }
  }

  return (
    <div className="training-session">
      <section className="training-session-header">
        <h1>{deck.name}</h1>
        <div className="training-buffer-counts">
          <div className="training-buffer-count training-buffer-count--new">
            <strong>{bufferCounts.newCount}</strong>
            <span>Новые</span>
          </div>
          <div className="training-buffer-count training-buffer-count--learning">
            <strong>{bufferCounts.learningCount}</strong>
            <span>Изучаемые</span>
          </div>
          <div className="training-buffer-count training-buffer-count--review">
            <strong>{bufferCounts.reviewCount}</strong>
            <span>Повторяемые</span>
          </div>
        </div>
      </section>
      <TrainingCard
        answerMode={answerMode}
        answerOptions={activeEntry.answerOptions}
        card={currentCard}
        cardProgress={activeEntry.progress || getProgress(visibleProgress, currentCard.id)}
        directionMode={directionMode}
        isSubmitting={isAnswering}
        key={`${currentCard.id}-${answerVersion}-${activeEntry.progress?.nextReviewAt || 'new'}`}
        onAnswer={handleAnswer}
      />
    </div>
  )
}
