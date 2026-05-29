import {
  BarChart3,
  Brain,
  GalleryVerticalEnd,
  Home,
  LogOut,
  Search,
  Settings,
  ShieldCheck,
  Trophy,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Navigate, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { Button } from '../shared/ui/Button'

const ADMIN_MODE_KEY = 'flashlex-admin-mode'

const navigation = [
  { to: '/', label: 'Обзор', icon: Home },
  { to: '/training', label: 'Тренировка', icon: Brain },
  { to: '/decks/public', label: 'Каталог', icon: Search },
  { to: '/stats', label: 'Статистика', icon: BarChart3 },
  { to: '/leaderboard', label: 'Рейтинг', icon: Trophy },
  { to: '/settings', label: 'Профиль', icon: Settings },
]

const roleLabels = {
  admin: 'Администратор',
}

export function AppLayout() {
  const { user, logout, isLoading } = useFlashLex()
  const navigate = useNavigate()
  const location = useLocation()
  const [adminMode, setAdminMode] = useState(
    () => localStorage.getItem(ADMIN_MODE_KEY) === 'true',
  )

  const canUseAdminMode = user?.role === 'admin'
  const isAdminPath = location.pathname.startsWith('/admin')

  useEffect(() => {
    if (!canUseAdminMode && adminMode) {
      localStorage.removeItem(ADMIN_MODE_KEY)
      return
    }

    if (canUseAdminMode && isAdminPath && !adminMode) {
      localStorage.setItem(ADMIN_MODE_KEY, 'true')
      return
    }

    if (canUseAdminMode && adminMode && !isAdminPath) {
      navigate('/admin', { replace: true })
    }
  }, [adminMode, canUseAdminMode, isAdminPath, navigate])

  if (isLoading) {
    return (
      <main className="auth-page">
        <section className="panel centered-panel">
          <h1>Загрузка...</h1>
        </section>
      </main>
    )
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const activeAdminMode = canUseAdminMode && (adminMode || isAdminPath)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleModeSwitch = () => {
    const nextMode = !activeAdminMode
    setAdminMode(nextMode)
    localStorage.setItem(ADMIN_MODE_KEY, String(nextMode))
    navigate(nextMode ? '/admin' : '/')
  }

  return (
    <div className={activeAdminMode ? 'shell shell--admin' : 'shell'}>
      <aside className="sidebar">
        <div>
          <NavLink className="sidebar__brand" to="/">
            <img alt="FlashLex" className="brand-mark brand-mark--image" src="/flashlex-icon.png" />
            <span>FlashLex</span>
          </NavLink>
          <nav className="sidebar__nav" aria-label="Основная навигация">
            {navigation.map((item) => (
              <NavLink
                className={({ isActive }) =>
                  `sidebar__link ${isActive ? 'sidebar__link--active' : ''}`
                }
                end={item.to === '/'}
                key={item.to}
                to={item.to}
              >
                <item.icon aria-hidden="true" size={19} />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        {canUseAdminMode ? (
          <div className="sidebar__footer">
            <button
              className="admin-mode-switch"
              onClick={handleModeSwitch}
              type="button"
            >
              <span className="admin-mode-switch__icon">
                <ShieldCheck aria-hidden="true" size={18} />
              </span>
              <span className="admin-mode-switch__text">
                <strong>Администрирование</strong>
                <small>{activeAdminMode ? 'Режим включен' : 'Режим отключен'}</small>
              </span>
              <span
                aria-hidden="true"
                className={
                  activeAdminMode
                    ? 'admin-mode-switch__indicator admin-mode-switch__indicator--active'
                    : 'admin-mode-switch__indicator'
                }
              />
            </button>
          </div>
        ) : null}
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar__title">
            {activeAdminMode ? (
              <ShieldCheck aria-hidden="true" size={20} />
            ) : (
              <GalleryVerticalEnd aria-hidden="true" size={20} />
            )}
            <span>{activeAdminMode ? 'Административный режим' : 'Учебная панель'}</span>
          </div>
          <div className="topbar__user">
            <div>
              <strong>{user?.name || 'Гость'}</strong>
              <span>{user?.email || 'demo'}</span>
              {roleLabels[user?.role] ? (
                <span className="topbar__role">{roleLabels[user.role]}</span>
              ) : null}
            </div>
            <Button icon={LogOut} onClick={handleLogout} variant="ghost">
              Выйти
            </Button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
