export const getTodayIso = () => new Date().toISOString().slice(0, 10)

export const addDaysIso = (baseIso, days) => {
  const date = new Date(baseIso)
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

export const isDue = (dateIso) => {
  if (!dateIso) return true
  if (String(dateIso).length <= 10) return dateIso <= getTodayIso()
  return new Date(dateIso).getTime() <= Date.now()
}

export const formatShortDate = (dateIso) =>
  new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: 'short',
  }).format(new Date(dateIso))
