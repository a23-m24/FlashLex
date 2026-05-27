import { Inbox } from 'lucide-react'

export function EmptyState({ title, text, action }) {
  return (
    <div className="empty-state">
      <Inbox aria-hidden="true" size={34} />
      <h3>{title}</h3>
      {text ? <p>{text}</p> : null}
      {action}
    </div>
  )
}
