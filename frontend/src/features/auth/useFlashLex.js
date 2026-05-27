import { useContext } from 'react'
import { FlashLexContext } from './flashLexContext'

export const useFlashLex = () => {
  const value = useContext(FlashLexContext)
  if (!value) {
    throw new Error('useFlashLex must be used inside FlashLexProvider')
  }
  return value
}
