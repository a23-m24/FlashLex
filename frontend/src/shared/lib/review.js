export const qualityOptions = [
  { quality: 'AGAIN_0', label: 'Снова', variant: 'danger' },
  { quality: 'HARD_3', label: 'Трудно', variant: 'secondary' },
  { quality: 'GOOD_4', label: 'Хорошо', variant: 'primary' },
  { quality: 'EASY_5', label: 'Легко', variant: 'success' },
]

export const formatReviewTime = (minutes) => {
  if (minutes < 60) return `${minutes} мин`
  if (minutes < 24 * 60) return `${Math.round(minutes / 60)} ч`
  const days = Math.round(minutes / (24 * 60))
  if (days === 1) return '1 день'
  if (days > 1 && days < 5) return `${days} дня`
  return `${days} дней`
}

export const calculateReview = (_currentProgress, quality) => {
  const currentProgress = _currentProgress || {}
  const isReview = currentProgress.status === 'REVIEW' || currentProgress.status === 'GRADUATED'
  const isLearning = currentProgress.status === 'LEARNING' || currentProgress.status === 'RELEARNING'
  const isNew = !currentProgress.status || currentProgress.status === 'NEW'
  const easeFactor = Math.max(Number(currentProgress.easeFactor || 2.5), 1.3)
  const rawIntervalDays = Number(currentProgress.intervalDays || 0)
  const rawIntervalMinutes = Number(currentProgress.intervalMinutes || 0)
  const currentIntervalDays = Math.max(
    rawIntervalMinutes > 0 && rawIntervalMinutes < 24 * 60 ? 0 : rawIntervalDays,
    rawIntervalMinutes >= 24 * 60 ? Math.ceil(rawIntervalMinutes / (24 * 60)) : 0,
    1,
  )

  if (isReview) {
    const hardDays = Math.max(currentIntervalDays + 1, Math.ceil(currentIntervalDays * 1.2))
    const goodDays = Math.max(hardDays + 1, Math.round(currentIntervalDays * easeFactor))
    const easyDays = Math.max(goodDays + 1, Math.round(goodDays * 1.3))
    const intervalMinutes =
      quality === 'AGAIN_0'
        ? 10
        : quality === 'HARD_3'
          ? hardDays * 24 * 60
          : quality === 'GOOD_4'
            ? goodDays * 24 * 60
            : easyDays * 24 * 60

    return {
      intervalDays: Math.floor(intervalMinutes / (24 * 60)),
      intervalMinutes,
      status: intervalMinutes >= 24 * 60 ? 'REVIEW' : 'LEARNING',
    }
  }

  if (isNew) {
    const intervalMinutes =
      quality === 'AGAIN_0'
        ? 10
        : quality === 'HARD_3'
          ? 24 * 60
          : quality === 'GOOD_4'
            ? 3 * 24 * 60
            : 4 * 24 * 60

    return {
      intervalDays: Math.floor(intervalMinutes / (24 * 60)),
      intervalMinutes,
      status: intervalMinutes >= 24 * 60 ? 'REVIEW' : 'LEARNING',
    }
  }

  const isRelearning = isLearning && Number(currentProgress.lapseCount || 0) > 0 && currentIntervalDays > 0
  if (isRelearning) {
    const relearnedDays = rawIntervalMinutes > 0 && rawIntervalMinutes < 24 * 60
      ? 1
      : Math.max(1, currentIntervalDays)
    const intervalMinutes =
      quality === 'AGAIN_0'
        ? 1
        : quality === 'HARD_3'
          ? 10
          : quality === 'GOOD_4'
            ? relearnedDays * 24 * 60
            : Math.max(4, relearnedDays + 1) * 24 * 60

    return {
      intervalDays: Math.floor(intervalMinutes / (24 * 60)),
      intervalMinutes,
      status: intervalMinutes >= 24 * 60 ? 'REVIEW' : 'LEARNING',
    }
  }

  const step = Number(currentProgress.remainingSteps || 0) > 0 || (isLearning && rawIntervalMinutes >= 10)
  const intervalMinutes =
    quality === 'AGAIN_0'
      ? 1
      : quality === 'HARD_3'
        ? step ? 10 : 6
        : quality === 'GOOD_4'
          ? step ? 24 * 60 : 10
          : 4 * 24 * 60

  return {
    intervalDays: Math.floor(intervalMinutes / (24 * 60)),
    intervalMinutes,
    status: intervalMinutes >= 24 * 60 ? 'REVIEW' : 'LEARNING',
  }
}

export const getReviewOptions = (currentProgress, serverOptions = []) => {
  const options = serverOptions.length
    ? serverOptions.map((serverOption) => ({
        ...qualityOptions.find((option) => option.quality === serverOption.quality),
        ...serverOption,
      }))
    : qualityOptions.map((option) => ({
        ...option,
        ...calculateReview(currentProgress, option.quality),
      }))

  return options.map((option) => {
    const intervalMinutes = option.intervalMinutes || 0
    return {
      ...option,
      time: formatReviewTime(intervalMinutes),
    }
  })
}
