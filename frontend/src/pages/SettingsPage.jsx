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
  const [profileStatus, setProfileStatus] = useState('')
  const [profileError, setProfileError] = useState('')
  const [isProfileSaving, setIsProfileSaving] = useState(false)

  const handleProfileSubmit = async (event) => {
    event.preventDefault()
    setProfileStatus('')
    setProfileError('')
    setIsProfileSaving(true)
    try {
      await updateProfile(profile)
      setProfileStatus('Профиль сохранен')
    } catch (error) {
      setProfileError(error.message || 'Не удалось сохранить профиль')
    } finally {
      setIsProfileSaving(false)
    }
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
            onChange={(event) => {
              setProfile({ ...profile, name: event.target.value })
              setProfileStatus('')
              setProfileError('')
            }}
            value={profile.name}
          />
          <Input
            label="Email"
            onChange={(event) => {
              setProfile({ ...profile, email: event.target.value })
              setProfileStatus('')
              setProfileError('')
            }}
            type="email"
            value={profile.email}
          />
          <div className="form-actions">
            <Button disabled={isProfileSaving} icon={Save} type="submit">
              {isProfileSaving ? 'Сохранение...' : 'Сохранить профиль'}
            </Button>
          </div>
          {profileStatus ? <p className="field__success">{profileStatus}</p> : null}
          {profileError ? <p className="field__error">{profileError}</p> : null}
        </form>

        <GoalSettingsForm onSubmit={updateGoal} user={user} />
      </div>
    </div>
  )
}
