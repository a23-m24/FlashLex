import {
  BarChart3,
  Brain,
  GalleryVerticalEnd,
  Home,
  LogOut,
  Search,
  Settings,
  Trophy,
} from 'lucide-react'
import { Navigate, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { Button } from '../shared/ui/Button'

const navigation = [
  { to: '/', label: 'Обзор', icon: Home },
  { to: '/training', label: 'Тренировка', icon: Brain },
  { to: '/decks/public', label: 'Каталог', icon: Search },
  { to: '/stats', label: 'Статистика', icon: BarChart3 },
  { to: '/leaderboard', label: 'Рейтинг', icon: Trophy },
  { to: '/settings', label: 'Профиль', icon: Settings },
]

export function AppLayout() {
  const { user, logout, isLoading } = useFlashLex()
  const navigate = useNavigate()

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

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="shell">
      <aside className="sidebar">
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
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar__title">
            <GalleryVerticalEnd aria-hidden="true" size={20} />
            <span>Учебная панель</span>
          </div>
          <div className="topbar__user">
            <div>
              <strong>{user?.name || 'Гость'}</strong>
              <span>{user?.email || 'demo'}</span>
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
