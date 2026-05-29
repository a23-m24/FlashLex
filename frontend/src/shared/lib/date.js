export const getTodayIso = () => new Date().toISOString().slice(0, 10)

export const isDue = (dateIso) => {
  if (!dateIso) return true
  if (String(dateIso).length <= 10) return dateIso <= getTodayIso()
  return new Date(dateIso).getTime() <= Date.now()
}
