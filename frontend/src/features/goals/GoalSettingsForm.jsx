import { useState } from 'react'
import { Save } from 'lucide-react'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'

export function GoalSettingsForm({ user, onSubmit }) {
  const [form, setForm] = useState({
    dailyNewLimit: user.dailyNewLimit,
    dailyReviewLimit: user.dailyReviewLimit,
  })
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  const update = (field, value) => {
    setStatus('')
    setError('')
    setForm((current) => ({
      ...current,
      [field]: value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setStatus('')
    setError('')
    setIsSaving(true)
    try {
      await onSubmit(form)
      setStatus('Цели сохранены')
    } catch (submitError) {
      setError(submitError.message || 'Не удалось сохранить цели')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form className="panel form-stack" onSubmit={handleSubmit}>
      <div className="section-heading">
        <div>
          <h2>Ежедневные цели</h2>
          <p>Лимит новых карточек и повторений.</p>
        </div>
      </div>
      <div className="form-grid">
        <Input
          label="Новых слов в день"
          min="1"
          onChange={(event) => update('dailyNewLimit', event.target.value)}
          type="number"
          value={form.dailyNewLimit}
        />
        <Input
          label="Повторений в день"
          min="1"
          onChange={(event) => update('dailyReviewLimit', event.target.value)}
          type="number"
          value={form.dailyReviewLimit}
        />
      </div>
      <div className="form-actions">
        <Button disabled={isSaving} icon={Save} type="submit">
          {isSaving ? 'Сохранение...' : 'Сохранить цели'}
        </Button>
      </div>
      {status ? <p className="field__success">{status}</p> : null}
      {error ? <p className="field__error">{error}</p> : null}
    </form>
  )
}
