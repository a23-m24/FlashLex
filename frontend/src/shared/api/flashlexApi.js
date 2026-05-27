const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'flashlex-token'

const phraseTypeToApi = {
  word: 'WORD',
  collocation: 'COLLOCATION',
  phrasal_verb: 'PHRASAL_VERB',
  idiom: 'IDIOM',
  phrase: 'PHRASE',
}

const phraseTypeFromApi = {
  WORD: 'word',
  COLLOCATION: 'collocation',
  PHRASAL_VERB: 'phrasal_verb',
  IDIOM: 'idiom',
  PHRASE: 'phrase',
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY)

export const storeToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token)
}

export const clearStoredToken = () => {
  localStorage.removeItem(TOKEN_KEY)
}

const toDate = (value) => (value ? String(value).slice(0, 10) : '')

const toDateTime = (value) => (value ? String(value) : '')

const toId = (value) => String(value)

const unwrapPage = (response) => response?.content || response || []

const parseErrorMessage = (payload, fallback) => {
  if (!payload) return fallback
  if (typeof payload === 'string') return payload
  return payload.message || payload.error || payload.detail || fallback
}

const request = async (path, { method = 'GET', body, token = getStoredToken() } = {}) => {
  const headers = {
    Accept: 'application/json',
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  let payload
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = text
  }

  if (!response.ok) {
    if (response.status === 401) {
      clearStoredToken()
    }
    throw new ApiError(parseErrorMessage(payload, 'Request failed'), response.status)
  }

  return payload
}

export const normalizeUser = (user) => ({
  id: toId(user.id),
  name: user.name,
  email: user.email,
  role: user.role,
  dailyNewLimit: user.dailyNewLimit,
  dailyReviewLimit: user.dailyReviewLimit,
  registeredAt: toDate(user.registeredAt),
})

export const normalizeCard = (card, deckId) => ({
  id: toId(card.id),
  deckId: toId(deckId),
  englishWord: card.englishWord,
  translation: card.translation,
  transcription: card.transcription || '',
  exampleSentence: card.exampleSentence || '',
  phraseType: phraseTypeFromApi[card.phraseType] || String(card.phraseType).toLowerCase(),
  difficulty: String(card.difficulty).toLowerCase(),
})

export const normalizeDeck = (deck) => ({
  id: toId(deck.id),
  name: deck.name,
  description: deck.description,
  authorId: toId(deck.authorId),
  authorName: deck.authorName,
  isPublished: Boolean(deck.published),
  tags: deck.tags || [],
  level: deck.level,
  rating: deck.rating || 0,
  ratingsCount: deck.ratingsCount || 0,
  clones: deck.clonesCount || 0,
  createdAt: toDate(deck.createdAt),
  metrics: deck.metrics || null,
})

export const normalizeProgress = (progress) => ({
  id: toId(progress.id),
  userId: toId(progress.userId),
  flashcardId: toId(progress.flashcardId),
  status: progress.status,
  intervalDays: progress.intervalDays,
  intervalMinutes: progress.intervalMinutes,
  intervalSeconds: progress.intervalSeconds,
  easeFactor: progress.easeFactor,
  nextReviewDate: toDate(progress.nextReviewDate),
  nextReviewAt: toDateTime(progress.nextReviewAt),
  correctAnswers: progress.correctAnswers,
  wrongAnswers: progress.wrongAnswers,
  remainingSteps: progress.remainingSteps || 0,
  lapseCount: progress.lapseCount || 0,
  leeched: Boolean(progress.leeched),
  lastReviewedAt: toDateTime(progress.lastReviewedAt),
  lastAnswerQuality: progress.lastAnswerQuality,
})

export const normalizeDailyStats = (stats) => ({
  date: toDate(stats.date),
  reviewed: stats.reviewed || 0,
  learned: stats.learned || 0,
  correct: stats.correct || 0,
  points: stats.points || 0,
  streakDays: stats.streakDays || 0,
})

export const normalizeLeaderboardRow = (row) => ({
  userId: toId(row.userId),
  name: row.name,
  learnedToday: row.learnedToday || 0,
  streakDays: row.streakDays || 0,
  accuracy: row.accuracy || 0,
  points: row.points || 0,
})

export const normalizeLearningStats = (stats) => ({
  ...stats,
  dailyStats: normalizeDailyStats(stats.dailyStats),
  weakCards: (stats.weakCards || []).map((item) => ({
    ...item,
    card: normalizeCard(item.card, item.card.deckId || ''),
    progress: normalizeProgress(item.progress),
  })),
})

export const normalizeTrainingNext = (entry) => ({
  deckId: toId(entry.deckId),
  card: entry.card ? normalizeCard(entry.card, entry.deckId) : null,
  progress: entry.progress ? normalizeProgress(entry.progress) : null,
  finished: Boolean(entry.finished),
  dueNowCount: entry.dueNowCount || 0,
  newCount: entry.newCount || 0,
  learningCount: entry.learningCount || 0,
  reviewCount: entry.reviewCount || 0,
  answerOptions: (entry.answerOptions || []).map((option) => ({
    quality: option.quality,
    intervalMinutes: option.intervalMinutes || 0,
    intervalDays: option.intervalDays || 0,
    nextStatus: option.nextStatus,
  })),
})

export const normalizeDeckCollection = (deckResponses) => {
  const decksById = new Map()
  const flashcardsById = new Map()

  deckResponses.forEach((deckResponse) => {
    const deck = normalizeDeck(deckResponse)
    decksById.set(deck.id, deck)
    ;(deckResponse.cards || []).forEach((card) => {
      const normalizedCard = normalizeCard(card, deck.id)
      flashcardsById.set(normalizedCard.id, normalizedCard)
    })
  })

  return {
    decks: [...decksById.values()],
    flashcards: [...flashcardsById.values()],
  }
}

const toDeckRequest = (payload) => ({
  name: payload.name,
  description: payload.description,
  level: payload.level,
  published: Boolean(payload.isPublished),
  tags: payload.tags || [],
  cards: (payload.cards || []).map((card) => ({
    englishWord: card.englishWord,
    translation: card.translation,
    transcription: card.transcription || null,
    exampleSentence: card.exampleSentence || null,
    phraseType: phraseTypeToApi[card.phraseType] || String(card.phraseType).toUpperCase(),
    difficulty: String(card.difficulty).toUpperCase(),
  })),
})

export const flashlexApi = {
  login: (email, password) =>
    request('/auth/login', {
      method: 'POST',
      body: { email, password },
      token: null,
    }),

  register: (form) =>
    request('/auth/register', {
      method: 'POST',
      body: {
        name: form.name,
        email: form.email,
        password: form.password,
        dailyNewLimit: Number(form.dailyNewLimit || 10),
        dailyReviewLimit: Number(form.dailyReviewLimit || 25),
      },
      token: null,
    }),

  me: (token) => request('/users/me', { token }),
  myDecks: (token) => request('/decks/my', { token }),
  publicDecks: () => request('/decks/public?size=100'),
  progress: (token) => request('/progress', { token }),
  dailyStats: (token) => request('/stats/daily', { token }),
  learningStats: (token) => request('/stats/learning', { token }),
  leaderboard: () => request('/leaderboard'),
  trainingNext: (deckId) => request(`/training/next?deckId=${Number(deckId)}`),

  createDeck: (payload) =>
    request('/decks', {
      method: 'POST',
      body: toDeckRequest(payload),
    }),

  updateDeck: (deckId, payload) =>
    request(`/decks/${deckId}`, {
      method: 'PUT',
      body: toDeckRequest(payload),
    }),

  deleteDeck: (deckId) =>
    request(`/decks/${deckId}`, {
      method: 'DELETE',
    }),

  publishDeck: (deckId) =>
    request(`/decks/${deckId}/publish`, {
      method: 'POST',
    }),

  cloneDeck: (deckId) =>
    request(`/decks/${deckId}/clone`, {
      method: 'POST',
    }),

  answerCard: (flashcardId, quality) =>
    request('/progress/answers', {
      method: 'POST',
      body: { flashcardId: Number(flashcardId), quality },
    }),

  updateProfile: (profile) =>
    request('/users/me', {
      method: 'PUT',
      body: profile,
    }),

  updateGoals: (settings) =>
    request('/users/me/goals', {
      method: 'PUT',
      body: {
        dailyNewLimit: Number(settings.dailyNewLimit),
        dailyReviewLimit: Number(settings.dailyReviewLimit),
      },
    }),

  unwrapPage,
}
