import { LinkButton } from '../shared/ui/LinkButton'

export function NotFoundPage() {
  return (
    <main className="not-found">
      <section className="panel centered-panel">
        <h1>Страница не найдена</h1>
        <p>Запрошенный адрес отсутствует в приложении.</p>
        <LinkButton to="/">На главную</LinkButton>
      </section>
    </main>
  )
}
