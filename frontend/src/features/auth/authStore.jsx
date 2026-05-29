import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  clearStoredToken,
  flashlexApi,
  getStoredToken,
  normalizeDailyStats,
  normalizeDeck,
  normalizeDeckCollection,
  normalizeLeaderboardRow,
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
  dailyStatsHistory: [],
  leaderboard: [],
  isLoading: true,
  error: null,
}

const mergeDeck = (decks, deck) => [
  deck,
  ...decks.filter((item) => item.id !== deck.id),
]

const mergeDecks = (currentDecks, nextDecks) => {
  const nextDeckIds = new Set(nextDecks.map((deck) => deck.id))
  return [
    ...nextDecks,
    ...currentDecks.filter((deck) => !nextDeckIds.has(deck.id)),
  ]
}

const replaceDeckCards = (flashcards, deckId, cards) => [
  ...flashcards.filter((card) => card.deckId !== deckId),
  ...cards,
]

const mergeRatedDeck = (decks, deck) => {
  const hasDeck = decks.some((item) => item.id === deck.id)
  const mergedDecks = decks.map((item) => {
    if (item.id === deck.id) {
      return deck
    }
    if (item.id === deck.ratingTargetId) {
      return {
        ...item,
        rating: deck.rating,
        ratingsCount: deck.ratingsCount,
        userRating: deck.userRating,
      }
    }
    return item
  })
  return hasDeck ? mergedDecks : [deck, ...mergedDecks]
}

const mergeDailyStatsHistory = (history, dailyStats) => {
  if (!dailyStats) {
    return history
  }
  const hasDate = history.some((item) => item.date === dailyStats.date)
  if (hasDate) {
    return history.map((item) => (item.date === dailyStats.date ? dailyStats : item))
  }
  return [...history, dailyStats].slice(-7)
}

const loadSessionData = async (token) => {
  const [
    user,
    myDecks,
    progress,
    dailyStats,
    dailyStatsHistory,
    leaderboard,
  ] = await Promise.all([
    flashlexApi.me(token),
    flashlexApi.myDecks(token),
    flashlexApi.progress(token),
    flashlexApi.dailyStats(token),
    flashlexApi.dailyStatsHistory(7, token),
    flashlexApi.leaderboard(),
  ])

  const { decks, flashcards } = normalizeDeckCollection(myDecks)

  return {
    user: normalizeUser(user),
    decks,
    flashcards,
    progress: progress.map(normalizeProgress),
    dailyStats: normalizeDailyStats(dailyStats),
    dailyStatsHistory: dailyStatsHistory.map(normalizeDailyStats),
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
        setState((current) => {
          const previousDeckCardIds = new Set(
            current.flashcards
              .filter((card) => card.deckId === deck.id)
              .map((card) => card.id),
          )
          const nextDeckCardIds = new Set(cards.map((card) => card.id))

          return {
            ...current,
            decks: mergeDeck(current.decks, deck),
            flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
            progress: current.progress.filter((item) =>
                !previousDeckCardIds.has(item.flashcardId) ||
                nextDeckCardIds.has(item.flashcardId),
            ),
          }
        })
      },

      async deleteDeck(deckId) {
        await flashlexApi.deleteDeck(deckId)
        setState((current) => {
          const deletedCardIds = new Set(
            current.flashcards
              .filter((card) => card.deckId === deckId)
              .map((card) => card.id),
          )

          return {
            ...current,
            decks: current.decks.filter((deck) => deck.id !== deckId),
            flashcards: current.flashcards.filter((card) => card.deckId !== deckId),
            progress: current.progress.filter((item) => !deletedCardIds.has(item.flashcardId)),
          }
        })
      },

      async publishDeck(deckId) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.publishDeck(deckId))
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
      },

      async loadDeck(deckId) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.deck(deckId))
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
        return deck
      },

      async cloneDeck(deckId) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.cloneDeck(deckId))
        setState((current) => ({
          ...current,
          decks: mergeDeck(current.decks, deck).map((item) => {
            const wasAlreadyLoaded = current.decks.some((currentDeck) => currentDeck.id === deck.id)
            if (!wasAlreadyLoaded && item.id === deck.sourceDeckId) {
              return { ...item, clones: item.clones + 1 }
            }
            return item
          }),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
        return deck.id
      },

      async rateDeck(deckId, value) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.rateDeck(deckId, value))
        setState((current) => ({
          ...current,
          decks: mergeRatedDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
      },

      async removeDeckRating(deckId) {
        const { deck, cards } = normalizeDeckWithCards(await flashlexApi.removeDeckRating(deckId))
        setState((current) => ({
          ...current,
          decks: mergeRatedDeck(current.decks, deck),
          flashcards: replaceDeckCards(current.flashcards, deck.id, cards),
        }))
      },

      async answerCard(cardId, quality) {
        const { progress: updatedProgress, dailyStats } = await flashlexApi.answerCard(cardId, quality)
        setState((current) => ({
          ...current,
          progress: [
            updatedProgress,
            ...current.progress.filter((item) => item.flashcardId !== updatedProgress.flashcardId),
          ],
          dailyStats: dailyStats || current.dailyStats,
          dailyStatsHistory: mergeDailyStatsHistory(current.dailyStatsHistory, dailyStats),
        }))

        return updatedProgress
      },

      async loadLeaderboard(period = 'DAY') {
        const leaderboard = await flashlexApi.leaderboard(period)
        setState((current) => ({
          ...current,
          leaderboard: leaderboard.map(normalizeLeaderboardRow),
        }))
      },

      async loadPublicDecks(filters) {
        const page = await flashlexApi.publicDecks(filters)
        const decks = flashlexApi.unwrapPage(page).map(normalizeDeck)
        setState((current) => ({
          ...current,
          decks: mergeDecks(current.decks, decks),
        }))
        return {
          ...page,
          decks,
          flashcards: [],
        }
      },

      async loadPublicDeckFacets() {
        return flashlexApi.publicDeckFacets()
      },

      async getNextTrainingCard(
        deckId,
        queueMode = 'GOAL',
        extraLimit = 0,
        extraNewLimit = 0,
        extraReviewLimit = 0,
      ) {
        return normalizeTrainingNext(
          await flashlexApi.trainingNext(
            deckId,
            queueMode,
            extraLimit,
            extraNewLimit,
            extraReviewLimit,
          ),
        )
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
