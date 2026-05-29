import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { SessionSummary } from '../features/training/SessionSummary'
import { TrainingCard } from '../features/training/TrainingCard'
import { isDue } from '../shared/lib/date'
import { getProgress } from '../shared/lib/metrics'
import { isLearningDue, isReviewStatus } from '../shared/lib/queue'
import { Button } from '../shared/ui/Button'
import { EmptyState } from '../shared/ui/EmptyState'
import { LinkButton } from '../shared/ui/LinkButton'

const reviewTime = (cardProgress) => {
  const rawValue = cardProgress?.nextReviewAt || cardProgress?.nextReviewDate
  return rawValue ? new Date(rawValue).getTime() : 0
}

export function TrainingSessionPage() {
  const [searchParams] = useSearchParams()
  const sessionParams = {
    answerMode: searchParams.get('answer') || 'SELF_CHECK',
    deckIdParam: searchParams.get('deck') || '',
    directionMode: searchParams.get('direction') || 'DIRECT_TRANSLATION',
    extraLimitParam: searchParams.get('extraLimit') || '0',
    extraNewLimitParam: searchParams.get('extraNewLimit') || '0',
    extraReviewLimitParam: searchParams.get('extraReviewLimit') || '0',
    queueMode: searchParams.get('queue') || 'GOAL',
  }
  const sessionKey = Object.values(sessionParams).join('|')

  return <TrainingSessionContent key={sessionKey} {...sessionParams} />
}

function TrainingSessionContent({
  answerMode,
  deckIdParam,
  directionMode,
  extraLimitParam,
  extraNewLimitParam,
  extraReviewLimitParam,
  queueMode,
}) {
  const { user, decks, flashcards, progress, answerCard, getNextTrainingCard } = useFlashLex()
  const fallbackDeck = decks.find((deck) => deck.authorId === user.id)
  const deckId = deckIdParam || fallbackDeck?.id || ''
  const rawExtraLimit = Number(extraLimitParam)
  const extraLimit = Number.isFinite(rawExtraLimit) ? Math.max(0, Math.floor(rawExtraLimit)) : 0
  const rawExtraNewLimit = Number(extraNewLimitParam)
  const rawExtraReviewLimit = Number(extraReviewLimitParam)
  const extraNewLimit = Number.isFinite(rawExtraNewLimit)
    ? Math.max(0, Math.floor(rawExtraNewLimit))
    : 0
  const extraReviewLimit = Number.isFinite(rawExtraReviewLimit)
    ? Math.max(0, Math.floor(rawExtraReviewLimit))
    : 0
  const isLimitedExtraSession =
    ((queueMode === 'EXTRA_NEW' || queueMode === 'EXTRA_REVIEW') && extraLimit > 0) ||
    (queueMode === 'EXTRA_MIXED' && extraNewLimit + extraReviewLimit > 0)
  const deck = decks.find((item) => item.id === deckId)
  const [entry, setEntry] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isAnswering, setIsAnswering] = useState(false)
  const [answerVersion, setAnswerVersion] = useState(0)
  const [error, setError] = useState('')
  const [extraAnswered, setExtraAnswered] = useState(0)
  const [extraNewAnswered, setExtraNewAnswered] = useState(0)
  const [extraReviewAnswered, setExtraReviewAnswered] = useState(0)
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
      } else if (isLearningDue(cardProgress)) {
        learningCount += 1
        learningItems.push({ card, progress: cardProgress })
      } else if (isReviewStatus(cardProgress.status) && isDue(cardProgress.nextReviewDate || cardProgress.nextReviewAt)) {
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

  const activeEntry = useMemo(() => {
    if (!entry || !isLimitedExtraSession) {
      return entry
    }

    const remainingExtra = Math.max(0, extraLimit - extraAnswered)
    const remainingExtraNew =
      queueMode === 'EXTRA_MIXED'
        ? Math.max(0, extraNewLimit - extraNewAnswered)
        : remainingExtra
    const remainingExtraReview =
      queueMode === 'EXTRA_MIXED'
        ? Math.max(0, extraReviewLimit - extraReviewAnswered)
        : remainingExtra
    return {
      ...entry,
      newCount:
        queueMode === 'EXTRA_NEW' || queueMode === 'EXTRA_MIXED'
          ? Math.min(entry.newCount || 0, remainingExtraNew)
          : 0,
      reviewCount:
        queueMode === 'EXTRA_REVIEW' || queueMode === 'EXTRA_MIXED'
          ? Math.min(entry.reviewCount || 0, remainingExtraReview)
          : 0,
    }
  }, [
    entry,
    extraAnswered,
    extraLimit,
    extraNewAnswered,
    extraNewLimit,
    extraReviewAnswered,
    extraReviewLimit,
    isLimitedExtraSession,
    queueMode,
  ])

  const bufferCounts = activeEntry
    ? {
        newCount: activeEntry.newCount,
        learningCount: activeEntry.learningCount,
        reviewCount: activeEntry.reviewCount,
      }
    : queueState.counts

  const loadNext = useCallback(async () => {
    if (!deckId) {
      setEntry(null)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError('')
    try {
      setEntry(
        await getNextTrainingCard(deckId, queueMode, extraLimit, extraNewLimit, extraReviewLimit),
      )
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsLoading(false)
    }
  }, [deckId, extraLimit, extraNewLimit, extraReviewLimit, getNextTrainingCard, queueMode])

  useEffect(() => {
    // The session page loads its first card when route parameters identify a new session.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadNext()
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
    return (
      <SessionSummary
        answerMode={answerMode}
        deckId={deckId}
        directionMode={directionMode}
        remainingNewCount={activeEntry.newBufferCount}
        remainingReviewCount={activeEntry.reviewBufferCount}
      />
    )
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
      const wasExtraNewCard = isLimitedExtraSession && !activeEntry.progress
      const wasExtraReviewCard =
        isLimitedExtraSession && isReviewStatus(activeEntry.progress?.status)
      const nextExtraAnswered = isLimitedExtraSession ? extraAnswered + 1 : extraAnswered
      const nextExtraNewAnswered = wasExtraNewCard ? extraNewAnswered + 1 : extraNewAnswered
      const nextExtraReviewAnswered = wasExtraReviewCard
        ? extraReviewAnswered + 1
        : extraReviewAnswered
      if (isLimitedExtraSession) {
        setExtraAnswered(nextExtraAnswered)
        setExtraNewAnswered(nextExtraNewAnswered)
        setExtraReviewAnswered(nextExtraReviewAnswered)
      }

      const nextExtraLimit = isLimitedExtraSession
        ? Math.max(0, extraLimit - nextExtraAnswered)
        : extraLimit
      const nextExtraNewLimit =
        queueMode === 'EXTRA_MIXED'
          ? Math.max(0, extraNewLimit - nextExtraNewAnswered)
          : extraNewLimit
      const nextExtraReviewLimit =
        queueMode === 'EXTRA_MIXED'
          ? Math.max(0, extraReviewLimit - nextExtraReviewAnswered)
          : extraReviewLimit
      const extraLimitsAreSpent =
        (queueMode === 'EXTRA_NEW' || queueMode === 'EXTRA_REVIEW') && nextExtraLimit <= 0
          ? true
          : queueMode === 'EXTRA_MIXED' && nextExtraNewLimit + nextExtraReviewLimit <= 0

      if (isLimitedExtraSession && extraLimitsAreSpent) {
        setEntry({
          ...activeEntry,
          card: null,
          progress: null,
          finished: true,
          dueNowCount: 0,
          newCount: 0,
          learningCount: 0,
          reviewCount: 0,
          newBufferCount: 0,
          reviewBufferCount: 0,
          answerOptions: [],
        })
      } else {
        const nextEntry = await getNextTrainingCard(
          deckId,
          queueMode,
          nextExtraLimit,
          nextExtraNewLimit,
          nextExtraReviewLimit,
        )
        setEntry(nextEntry)
      }
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
