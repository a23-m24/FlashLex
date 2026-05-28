export const getDeckCards = (flashcards, deckId) =>
  flashcards.filter((card) => card.deckId === deckId)

export const getProgress = (progress, cardId) =>
  progress.find((item) => item.flashcardId === cardId)

export const getWeakCards = (cards, progress) =>
  cards
    .map((card) => {
      const cardProgress = getProgress(progress, card.id)
      const answers =
        (cardProgress?.correctAnswers || 0) + (cardProgress?.wrongAnswers || 0)
      const wrongRate = answers ? (cardProgress.wrongAnswers || 0) / answers : 0
      return { card, progress: cardProgress, wrongRate, answers }
    })
    .filter((item) => item.answers > 0 && item.wrongRate >= 0.3)
    .sort((a, b) => b.wrongRate - a.wrongRate)

export const getStatusSummary = (cards, progress) => {
  const summary = { NEW: 0, LEARNING: 0, REVIEW: 0 }
  cards.forEach((card) => {
    const rawStatus = getProgress(progress, card.id)?.status || 'NEW'
    const status = rawStatus === 'GRADUATED' ? 'REVIEW' : rawStatus === 'RELEARNING' ? 'LEARNING' : rawStatus
    summary[status] += 1
  })
  return summary
}

export const getAccuracy = (progress) => {
  const correct = progress.reduce((sum, item) => sum + item.correctAnswers, 0)
  const wrong = progress.reduce((sum, item) => sum + item.wrongAnswers, 0)
  const total = correct + wrong
  return total ? Math.round((correct / total) * 100) : 0
}
