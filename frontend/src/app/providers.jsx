import { FlashLexProvider } from '../features/auth/authStore'

export function AppProviders({ children }) {
  return <FlashLexProvider>{children}</FlashLexProvider>
}
