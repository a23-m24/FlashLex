import { useState } from 'react'
import { Save } from 'lucide-react'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'

export function GoalSettingsForm({ user, onSubmit }) {
  const [form, setForm] = useState({
    dailyNewLimit: user.dailyNewLimit,
    dailyReviewLimit: user.dailyReviewLimit,
  })

  const update = (field, value) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    onSubmit(form)
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
        <Button icon={Save} type="submit">
          Сохранить цели
        </Button>
      </div>
    </form>
  )
}
