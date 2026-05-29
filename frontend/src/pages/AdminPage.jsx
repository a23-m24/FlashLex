import {
  BookOpen,
  Database,
  Edit,
  Search,
  ShieldCheck,
  Trash2,
  UserCheck,
  UserX,
  Users,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { flashlexApi } from '../shared/api/flashlexApi'
import { Badge } from '../shared/ui/Badge'
import { Button } from '../shared/ui/Button'
import { EmptyState } from '../shared/ui/EmptyState'
import { Input } from '../shared/ui/Input'
import { LinkButton } from '../shared/ui/LinkButton'
import { Pagination } from '../shared/ui/Pagination'
import { StatTile } from '../shared/ui/StatTile'

const emptyPage = { content: [], totalPages: 1, totalElements: 0 }

export function AdminPage() {
  const { user } = useFlashLex()
  const [dashboard, setDashboard] = useState(null)
  const [usersPage, setUsersPage] = useState(emptyPage)
  const [decksPage, setDecksPage] = useState(emptyPage)
  const [usersLoading, setUsersLoading] = useState(true)
  const [decksLoading, setDecksLoading] = useState(true)
  const [error, setError] = useState('')
  const [usersQuery, setUsersQuery] = useState('')
  const [decksQuery, setDecksQuery] = useState('')
  const [usersPageNumber, setUsersPageNumber] = useState(1)
  const [decksPageNumber, setDecksPageNumber] = useState(1)

  useEffect(() => {
    let cancelled = false

    flashlexApi.adminDashboard()
      .then((response) => {
        if (!cancelled) setDashboard(response)
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message)
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    flashlexApi.adminUsers({
      query: usersQuery,
      page: usersPageNumber - 1,
      size: 8,
    })
      .then((response) => {
        if (!cancelled) setUsersPage(response)
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message)
      })
      .finally(() => {
        if (!cancelled) setUsersLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [usersPageNumber, usersQuery])

  useEffect(() => {
    let cancelled = false

    flashlexApi.adminDecks({
      query: decksQuery,
      page: decksPageNumber - 1,
      size: 8,
    })
      .then((response) => {
        if (!cancelled) setDecksPage(response)
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message)
      })
      .finally(() => {
        if (!cancelled) setDecksLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [decksPageNumber, decksQuery])

  if (user.role !== 'admin') {
    return <Navigate replace to="/" />
  }

  const reloadDashboard = async () => {
    setDashboard(await flashlexApi.adminDashboard())
  }

  const handleUsersQuery = (event) => {
    setUsersLoading(true)
    setError('')
    setUsersQuery(event.target.value)
    setUsersPageNumber(1)
  }

  const handleDecksQuery = (event) => {
    setDecksLoading(true)
    setError('')
    setDecksQuery(event.target.value)
    setDecksPageNumber(1)
  }

  const handlePublicationBan = async (targetUser) => {
    const nextValue = !targetUser.publicationBanned
    if (nextValue && !window.confirm(`Запретить публикацию наборов для ${targetUser.name}?`)) {
      return
    }
    setError('')
    try {
      const updated = await flashlexApi.adminSetPublicationBan(targetUser.id, nextValue)
      setUsersPage((current) => ({
        ...current,
        content: current.content.map((item) => (item.id === updated.id ? updated : item)),
      }))
      await reloadDashboard()
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  const handleDeckDelete = async (deck) => {
    if (!window.confirm(`Удалить публичный набор "${deck.name}" вместе с карточками и прогрессом?`)) return
    setError('')
    try {
      await flashlexApi.adminDeleteDeck(deck.id)
      setDecksPage((current) => ({
        ...current,
        content: current.content.filter((item) => item.id !== deck.id),
        totalElements: Math.max(0, current.totalElements - 1),
      }))
      await reloadDashboard()
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  const handleUsersPage = (nextPage) => {
    setUsersLoading(true)
    setUsersPageNumber(nextPage)
  }

  const handleDecksPage = (nextPage) => {
    setDecksLoading(true)
    setDecksPageNumber(nextPage)
  }

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Администрирование</span>
          <h1>Панель управления</h1>
        </div>
      </section>

      {error ? <EmptyState text={error} title="Действие не выполнено" /> : null}

      <div className="stats-grid">
        <StatTile icon={Users} label="Пользователи" value={dashboard?.users ?? '-'} />
        <StatTile icon={UserX} label="Запрет публикации" tone="red" value={dashboard?.publicationBannedUsers ?? '-'} />
        <StatTile icon={BookOpen} label="Публичные наборы" tone="green" value={dashboard?.publishedDecks ?? '-'} />
        <StatTile icon={ShieldCheck} label="Личные наборы" tone="amber" value={dashboard?.privateDecks ?? '-'} />
        <StatTile icon={Database} label="Карточки" tone="blue" value={dashboard?.flashcards ?? '-'} />
      </div>

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Пользователи</h2>
          </div>
          <div className="admin-search">
            <Search aria-hidden="true" size={18} />
            <Input label="Поиск" onChange={handleUsersQuery} placeholder="Имя или email" value={usersQuery} />
          </div>
        </div>
        {usersLoading ? (
          <EmptyState text="Загружаем пользователей." title="Загрузка" />
        ) : (
          <>
            <div className="table-wrap">
              <table className="data-table admin-table">
                <thead>
                  <tr>
                    <th>Пользователь</th>
                    <th>Публикация</th>
                    <th>Наборы</th>
                    <th>Прогресс</th>
                    <th>Очки</th>
                    <th>Действие</th>
                  </tr>
                </thead>
                <tbody>
                  {usersPage.content.map((item) => {
                    const privateDecks = Math.max(0, item.deckCount - item.publishedDeckCount)
                    const isCurrentAdmin = item.id === user.id || item.role === 'admin'
                    return (
                      <tr key={item.id}>
                        <td>
                          <strong>{item.name}</strong>
                          <span>{item.email}</span>
                        </td>
                        <td>
                          <Badge tone={item.publicationBanned ? 'red' : 'green'}>
                            {item.publicationBanned ? 'Запрещена' : 'Разрешена'}
                          </Badge>
                        </td>
                        <td>
                          <strong>{item.deckCount}</strong>
                          <span>{item.publishedDeckCount} публичных, {privateDecks} личных</span>
                        </td>
                        <td>
                          <strong>{item.progressCount}</strong>
                          <span>карточек</span>
                        </td>
                        <td>
                          <strong>{item.todayPoints}</strong>
                          <span>{item.weekPoints} за неделю</span>
                        </td>
                        <td>
                          <Button
                            disabled={isCurrentAdmin}
                            icon={item.publicationBanned ? UserCheck : UserX}
                            onClick={() => handlePublicationBan(item)}
                            size="sm"
                            variant={item.publicationBanned ? 'secondary' : 'danger'}
                          >
                            {item.publicationBanned ? 'Разрешить' : 'Запретить'}
                          </Button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            <Pagination onChange={handleUsersPage} page={usersPageNumber} totalPages={Math.max(1, usersPage.totalPages)} />
          </>
        )}
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Публичные наборы</h2>
          </div>
          <div className="admin-search">
            <Search aria-hidden="true" size={18} />
            <Input label="Поиск" onChange={handleDecksQuery} placeholder="Название, автор, тег" value={decksQuery} />
          </div>
        </div>
        {decksLoading ? (
          <EmptyState text="Загружаем публичные наборы." title="Загрузка" />
        ) : (
          <>
            <div className="table-wrap">
              <table className="data-table admin-table">
                <thead>
                  <tr>
                    <th>Набор</th>
                    <th>Автор</th>
                    <th>Состав</th>
                    <th>Оценки</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {decksPage.content.map((deck) => (
                    <tr key={deck.id}>
                      <td>
                        <strong>{deck.name}</strong>
                        <span>{deck.description}</span>
                        <div className="tag-row">
                          {deck.tags.slice(0, 3).map((tag) => (
                            <Badge key={tag} tone="green">{tag}</Badge>
                          ))}
                        </div>
                      </td>
                      <td>
                        <strong>{deck.authorName}</strong>
                        <span>{deck.authorEmail}</span>
                      </td>
                      <td>
                        <strong>{deck.cardCount}</strong>
                        <span>{deck.level}</span>
                      </td>
                      <td>
                        <strong>{deck.rating ? deck.rating.toFixed(1) : '-'}</strong>
                        <span>{deck.ratingsCount} оценок, {deck.clonesCount} добавлений</span>
                      </td>
                      <td>
                        <div className="admin-actions">
                          <LinkButton icon={Edit} size="sm" to={`/admin/decks/${deck.id}/edit`} variant="secondary">
                            Изменить
                          </LinkButton>
                          <Button icon={Trash2} onClick={() => handleDeckDelete(deck)} size="sm" variant="danger">
                            Удалить
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination onChange={handleDecksPage} page={decksPageNumber} totalPages={Math.max(1, decksPage.totalPages)} />
          </>
        )}
      </section>
    </div>
  )
}
