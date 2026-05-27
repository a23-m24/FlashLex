import { Save } from 'lucide-react'
import { useState } from 'react'
import { useFlashLex } from '../features/auth/useFlashLex'
import { GoalSettingsForm } from '../features/goals/GoalSettingsForm'
import { Button } from '../shared/ui/Button'
import { Input } from '../shared/ui/Input'

export function SettingsPage() {
  const { user, updateGoal, updateProfile } = useFlashLex()
  const [profile, setProfile] = useState({
    name: user.name,
    email: user.email,
  })

  const handleProfileSubmit = async (event) => {
    event.preventDefault()
    await updateProfile(profile)
  }

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Профиль</span>
          <h1>Настройки</h1>
          <p>Данные пользователя и учебные лимиты.</p>
        </div>
      </section>

      <div className="dashboard-grid">
        <form className="panel form-stack" onSubmit={handleProfileSubmit}>
          <div className="section-heading">
            <div>
              <h2>Пользователь</h2>
              <p>Имя и email для авторизованного доступа.</p>
            </div>
          </div>
          <Input
            label="Имя"
            onChange={(event) => setProfile({ ...profile, name: event.target.value })}
            value={profile.name}
          />
          <Input
            label="Email"
            onChange={(event) => setProfile({ ...profile, email: event.target.value })}
            type="email"
            value={profile.email}
          />
          <div className="form-actions">
            <Button icon={Save} type="submit">
              Сохранить профиль
            </Button>
          </div>
        </form>

        <GoalSettingsForm onSubmit={updateGoal} user={user} />
      </div>
    </div>
  )
}
