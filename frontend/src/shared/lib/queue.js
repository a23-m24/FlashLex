import { isDue } from './date'

export const isLearningStatus = (status) => status === 'LEARNING' || status === 'RELEARNING'

export const isReviewStatus = (status) => status === 'REVIEW' || status === 'GRADUATED'

export const isLearningDue = (progress) =>
  isLearningStatus(progress?.status) && isDue(progress.nextReviewAt || progress.nextReviewDate)

export const groupCardsByDeck = (flashcards) =>
  flashcards.reduce((groupedCards, card) => {
    const cards = groupedCards.get(card.deckId) || []
    cards.push(card)
    groupedCards.set(card.deckId, cards)
    return groupedCards
  }, new Map())

export const mapProgressByCard = (progress) =>
  new Map(progress.map((item) => [item.flashcardId, item]))

export const getDeckQueue = (deckId, cardsByDeckId, progressByCardId) => {
  const cards = cardsByDeckId.get(deckId) || []
  return cards.reduce(
    (counts, card) => {
      const cardProgress = progressByCardId.get(card.id)
      if (!cardProgress || cardProgress.status === 'NEW') {
        counts.newCount += 1
      } else if (isLearningDue(cardProgress)) {
        counts.learningCount += 1
      } else if (
        isReviewStatus(cardProgress.status) &&
        isDue(cardProgress.nextReviewAt || cardProgress.nextReviewDate)
      ) {
        counts.reviewCount += 1
      }
      return counts
    },
    { cards, newCount: 0, learningCount: 0, reviewCount: 0 },
  )
}
