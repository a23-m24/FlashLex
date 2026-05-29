import { LogIn } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { Button } from '../shared/ui/Button'
import { Input } from '../shared/ui/Input'

export function LoginPage() {
  const { login } = useFlashLex()
  const navigate = useNavigate()
  const [email, setEmail] = useState('student@flashlex.test')
  const [password, setPassword] = useState('demo12345')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="auth-card" onSubmit={handleSubmit}>
      <h2>Вход</h2>
      <Input
        label="Email"
        onChange={(event) => setEmail(event.target.value)}
        type="email"
        value={email}
      />
      <Input
        label="Пароль"
        onChange={(event) => setPassword(event.target.value)}
        type="password"
        value={password}
      />
      {error ? <p className="field__error">{error}</p> : null}
      <Button disabled={isSubmitting} icon={LogIn} type="submit">
        Войти
      </Button>
      <p>
        Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
      </p>
    </form>
  )
}
