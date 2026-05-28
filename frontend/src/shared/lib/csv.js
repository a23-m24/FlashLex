const headers = [
  'englishWord',
  'translation',
  'transcription',
  'exampleSentence',
  'phraseType',
  'difficulty',
]

const phraseTypes = new Set(['word', 'phrase', 'collocation', 'phrasal_verb', 'idiom'])
const difficulties = new Set(['easy', 'medium', 'hard'])

const normalizeHeader = (value) => value.trim().replace(/^\uFEFF/, '')

const escapeCsvValue = (value) => {
  const text = String(value ?? '')
  if (!/[",\r\n]/.test(text)) {
    return text
  }
  return `"${text.replace(/"/g, '""')}"`
}

const parseCsvRows = (text) => {
  const rows = []
  let row = []
  let value = ''
  let isQuoted = false

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    const nextChar = text[index + 1]

    if (isQuoted) {
      if (char === '"' && nextChar === '"') {
        value += '"'
        index += 1
      } else if (char === '"') {
        isQuoted = false
      } else {
        value += char
      }
    } else if (char === '"') {
      isQuoted = true
    } else if (char === ',') {
      row.push(value)
      value = ''
    } else if (char === '\n') {
      row.push(value)
      rows.push(row)
      row = []
      value = ''
    } else if (char !== '\r') {
      value += char
    }
  }

  row.push(value)
  if (row.some((cell) => cell.trim())) {
    rows.push(row)
  }
  return rows
}

const createDraftId = () => `csv-${Date.now()}-${Math.floor(Math.random() * 1000000)}`

export const cardsToCsv = (cards) => {
  const rows = [
    headers.join(','),
    ...cards.map((card) =>
      headers.map((field) => escapeCsvValue(card[field])).join(','),
    ),
  ]
  return `${rows.join('\r\n')}\r\n`
}

export const parseCardsCsv = (text) => {
  const rows = parseCsvRows(text)
  if (rows.length < 2) {
    throw new Error('CSV должен содержать заголовок и хотя бы одну карточку')
  }

  const headerRow = rows[0].map(normalizeHeader)
  const columnIndex = Object.fromEntries(headerRow.map((header, index) => [header, index]))
  const missingHeaders = headers.filter((header) => columnIndex[header] === undefined)
  if (missingHeaders.length) {
    throw new Error(`В CSV нет колонок: ${missingHeaders.join(', ')}`)
  }

  const cards = rows.slice(1)
    .map((row) => {
      const phraseType = (row[columnIndex.phraseType] || 'word').trim().toLowerCase()
      const difficulty = (row[columnIndex.difficulty] || 'medium').trim().toLowerCase()
      return {
        draftId: createDraftId(),
        englishWord: (row[columnIndex.englishWord] || '').trim(),
        translation: (row[columnIndex.translation] || '').trim(),
        transcription: (row[columnIndex.transcription] || '').trim(),
        exampleSentence: (row[columnIndex.exampleSentence] || '').trim(),
        phraseType: phraseTypes.has(phraseType) ? phraseType : 'word',
        difficulty: difficulties.has(difficulty) ? difficulty : 'medium',
      }
    })
    .filter((card) => card.englishWord || card.translation)

  if (!cards.length) {
    throw new Error('В CSV нет заполненных карточек')
  }
  const invalidRow = cards.find((card) => !card.englishWord || !card.translation)
  if (invalidRow) {
    throw new Error('У каждой карточки должны быть английский текст и перевод')
  }

  return cards
}

export const downloadCsv = (filename, csv) => {
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export const safeCsvFilename = (name) =>
  `${String(name || 'deck')
    .trim()
    .replace(/[\\/:*?"<>|]+/g, '-')
    .replace(/\s+/g, '_') || 'deck'}.csv`
