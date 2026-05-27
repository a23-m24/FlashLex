import { Outlet } from 'react-router-dom'

export function AuthLayout() {
  return (
    <main className="auth-page">
      <section className="auth-page__brand">
        <img alt="FlashLex" className="auth-page__logo" src="/flashlex-logo.png" />
        <div className="auth-page__message">
          <p>Учите слова и выражения в удобном ритме.</p>
        </div>
      </section>
      <Outlet />
    </main>
  )
}
