import { UserPlus } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { Button } from '../shared/ui/Button'
import { Input } from '../shared/ui/Input'

export function RegisterPage() {
  const { register } = useFlashLex()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }))

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await register(form)
      navigate('/')
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="auth-card" onSubmit={handleSubmit}>
      <h2>Регистрация</h2>
      <Input
        label="Имя"
        onChange={(event) => update('name', event.target.value)}
        required
        value={form.name}
      />
      <Input
        label="Email"
        onChange={(event) => update('email', event.target.value)}
        required
        type="email"
        value={form.email}
      />
      <Input
        label="Пароль"
        minLength="6"
        onChange={(event) => update('password', event.target.value)}
        required
        type="password"
        value={form.password}
      />
      {error ? <p className="field__error">{error}</p> : null}
      <Button disabled={isSubmitting} icon={UserPlus} type="submit">
        Создать аккаунт
      </Button>
      <p>
        Уже есть аккаунт? <Link to="/login">Войти</Link>
      </p>
    </form>
  )
}
