import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  clearStoredToken,
  flashlexApi,
  getStoredToken,
  normalizeDailyStats,
  normalizeDeck,
  normalizeDeckCollection,
  normalizeLeaderboardRow,
  normalizeLearningStats,
  normalizeProgress,
  normalizeTrainingNext,
  normalizeUser,
  storeToken,
} from '../../shared/api/flashlexApi'
import { getTodayIso } from '../../shared/lib/date'
import { FlashLexContext } from './flashLexContext'

const emptyDailyStats = () => ({
  date: getTodayIso(),
  reviewed: 0,
  learned: 0,
  correct: 0,
  points: 0,
  streakDays: 0,
})

const initialState = {
  user: null,
  decks: [],
  flashcards: [],
  progress: [],
  dailyStats: emptyDailyStats(),
  leaderboard: [],
  learningStats: null,
  isLoading: true,
  error: null,
}

const mergeDeck = (decks, deck) => [
  deck,
  ...decks.filter((item) => item.id !== deck.id),
]

const replaceDeckCards = (flashcards, deckId, cards) => [
  ...flashcards.filter((card) => card.deckId !== deckId),
  ...cards,
]

const loadSessionData = async (token) => {
  const [
    user,
    myDecks,
    publicDeckPage,
    progress,
    dailyStats,
    learningStats,
    leaderboard,
  ] = await Promise.all([
    flashlexApi.me(token),
    flashlexApi.myDecks(token),
    flashlexApi.publicDecks(),
    flashlexApi.progress(token),
    flashlexApi.dailyStats(token),
    flashlexApi.learningStats(token),
    flashlexApi.leaderboard(),
  ])

  const publicDecks = flashlexApi.unwrapPage(publicDeckPage)
  const { decks, flashcards } = normalizeDeckCollection([...publicDecks, ...myDecks])

  return {
    user: normalizeUser(user),
    decks,
    flashcards,
    progress: progress.map(normalizeProgress),
    dailyStats: normalizeDailyStats(dailyStats),
    learningStats: normalizeLearningStats(learningStats),
    leaderboard: leaderboard.map(normalizeLeaderboardRow),
    isLoading: false,
    error: null,
  }
}

const normalizeDeckWithCards = (deckResponse) => {
  const deck = normalizeDeck(deckResponse)
  const { flashcards } = normalizeDeckCollection([deckResponse])
  return { deck, cards: flashcards }
}

export function FlashLexProvider({ children }) {
  const [state, setState] = useState(() => ({
    ...initialState,
    isLoading: Boolean(getStoredToken()),
  }))

  const refreshSession = useCallback(async (token = getStoredToken()) => {
    if (!token) {
      setState((current) => ({
        ...current,
        user: null,
        isLoading: false,
        error: null,
      }))
      return null
    }

    setState((current) => ({ ...current, isLoading: true, error: null }))
    try {
      const session = await loadSessionData(token)
      setState(session)
      return session
    } catch (error) {
      clearStoredToken()
      setState({
        ...initialState,
        isLoading: false,
        error: error.message,
      })
      throw error
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    const token = getStoredToken()

    if (!token) {
      return () => {
        cancelled = true
      }
    }

    loadSessionData(token)
      .then((session) => {
        if (!cancelled) {
          setState(session)
        }
      })
      .catch((error) => {
        clearStoredToken()
        if (!cancelled) {
          setState({
            ...initialState,
            isLoading: false,
            error: error.message,
          })
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const actions = useMemo(
    () => ({
      async login(email, password) {
        const response = await flashlexApi.login(email, password)
        storeToken(response.token)
        const session = await loadSessionData(response.token)
        setState(session)
      },

      async register(form) {
        const response = await flashlexApi.register(form)
        storeToken(response.token)
        const session = await loadSessionData(response.token)
        setState(session)
      },

      logout() {
        clearStoredToken()
        setState({
          ...initialState,
          isLoading: false,
        })
      },

      async updateGoal(settings) {
        const user = normalizeUser(await flashlexApi.updateGoals(settings))
        setState((current) => ({ ...current, user }))
      },

      async updateProfile(profile) {
        const user = normalizeUser(await flashlexApi.updateProfile(profile))
        setState((current) => ({ ...current, user }))
      },

      async createDeck(payload) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.createDeck(payload))
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
        return deck.id
      },

      async updateDeck(deckId, payload) {
        const { deck, cards } = normalizeDeckWithCards(
          await flashlexApi.updateDeck(deckId, payload),
        )
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
          progress: current.progress.filter((item) => {
            const previousDeckCardIds = current.flashcards
              .filter((card) => card.deckId === deck.id)
              .map((card) => card.id)
            const nextDeckCardIds = cards.map((card) => card.id)
            return (
              !previousDeckCardIds.includes(item.flashcardId) ||
              nextDeckCardIds.includes(item.flashcardId)
            )
          }),
        }))
      },

      async deleteDeck(deckId) {
        await flashlexApi.deleteDeck(deckId)
        setState((current) => ({
          ...current,
          decks: current.decks.filter((deck) => deck.id !== deckId),
          flashcards: current.flashcards.filter((card) => card.deckId !== deckId),
          progress: current.progress.filter((item) => {
            const card = current.flashcards.find(
              (flashcard) => flashcard.id === item.flashcardId,
            )
            return !card || card.deckId !== deckId
          }),
        }))
      },

      async publishDeck(deckId) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.publishDeck(deckId))
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
      },

      async cloneDeck(deckId) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.cloneDeck(deckId))
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
        return deck.id
      },

      async answerCard(cardId, quality) {
        const updatedProgress = normalizeProgress(await flashlexApi.answerCard(cardId, quality))
        const [dailyStats, leaderboard] = await Promise.all([
          flashlexApi.dailyStats(),
          flashlexApi.leaderboard(),
        ])

        setState((current) => ({
          ...current,
          progress: [
            updatedProgress,
            ...current.progress.filter((item) => item.flashcardId !== updatedProgress.flashcardId),
          ],
          dailyStats: normalizeDailyStats(dailyStats),
          leaderboard: leaderboard.map(normalizeLeaderboardRow),
        }))

        return updatedProgress
      },

      async getNextTrainingCard(deckId) {
        return normalizeTrainingNext(await flashlexApi.trainingNext(deckId))
      },

      refreshSession,
    }),
    [refreshSession],
  )

  const value = useMemo(
    () => ({
      ...state,
      ...actions,
    }),
    [actions, state],
  )

  return <FlashLexContext.Provider value={value}>{children}</FlashLexContext.Provider>
}
