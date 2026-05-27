import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppLayout } from '../layouts/AppLayout'
import { AuthLayout } from '../layouts/AuthLayout'
import { DashboardPage } from '../pages/DashboardPage'
import { DeckDetailsPage } from '../pages/DeckDetailsPage'
import { DeckEditorPage } from '../pages/DeckEditorPage'
import { LeaderboardPage } from '../pages/LeaderboardPage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { PublicDecksPage } from '../pages/PublicDecksPage'
import { RegisterPage } from '../pages/RegisterPage'
import { SettingsPage } from '../pages/SettingsPage'
import { StatsPage } from '../pages/StatsPage'
import { TrainingPage } from '../pages/TrainingPage'
import { TrainingSessionPage } from '../pages/TrainingSessionPage'

export const router = createBrowserRouter([
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
    ],
  },
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/decks', element: <Navigate to="/" replace /> },
      { path: '/decks/public', element: <PublicDecksPage /> },
      { path: '/decks/new', element: <DeckEditorPage /> },
      { path: '/decks/:deckId', element: <DeckDetailsPage /> },
      { path: '/decks/:deckId/edit', element: <DeckEditorPage /> },
      { path: '/training', element: <TrainingPage /> },
      { path: '/training/session', element: <TrainingSessionPage /> },
      { path: '/stats', element: <StatsPage /> },
      { path: '/leaderboard', element: <LeaderboardPage /> },
      { path: '/settings', element: <SettingsPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
