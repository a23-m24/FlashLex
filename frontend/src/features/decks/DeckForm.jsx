import { Plus, Save, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'
import { Select } from '../../shared/ui/Select'
import { Textarea } from '../../shared/ui/Textarea'
import { FlashcardEditor } from './FlashcardEditor'

const blankCard = {
  englishWord: '',
  translation: '',
  transcription: '',
  exampleSentence: '',
  phraseType: 'word',
  difficulty: 'medium',
}

const createBlankCard = () => ({
  ...blankCard,
  draftId: `draft-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
})

export function DeckForm({ initialDeck, initialCards, onSubmit }) {
  const [form, setForm] = useState(() => ({
    name: initialDeck?.name || '',
    description: initialDeck?.description || '',
    tags: initialDeck?.tags || [],
    level: initialDeck?.level || 'A2',
    isPublished: initialDeck?.isPublished || false,
    cards: initialCards?.length ? initialCards : [createBlankCard()],
  }))
  const [tagInput, setTagInput] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const errors = useMemo(() => {
    const result = {}
    if (!form.name.trim()) result.name = 'Введите название'
    if (!form.tags.length) result.tags = 'Добавьте хотя бы один тег'
    if (!form.description.trim()) result.description = 'Добавьте описание'
    if (form.cards.length === 0) result.cards = 'Добавьте хотя бы одну карточку'
    form.cards.forEach((card, index) => {
      if (!card.englishWord.trim() || !card.translation.trim()) {
        result[`card-${index}`] = 'Заполните слово и перевод'
      }
    })
    return result
  }, [form])

  const update = (field, value) => {
    setSubmitError('')
    setForm((current) => ({ ...current, [field]: value }))
  }

  const addTag = () => {
    const tag = tagInput.trim().toLowerCase()
    if (!tag) return
    setSubmitError('')
    setForm((current) => ({
      ...current,
      tags: current.tags.includes(tag) ? current.tags : [...current.tags, tag],
    }))
    setTagInput('')
  }

  const removeTag = (tag) => {
    setSubmitError('')
    setForm((current) => ({
      ...current,
      tags: current.tags.filter((item) => item !== tag),
    }))
  }

  const handleTagKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      addTag()
    }
  }

  const handleCardChange = (index, nextCard) => {
    setSubmitError('')
    setForm((current) => ({
      ...current,
      cards: current.cards.map((card, cardIndex) =>
        cardIndex === index ? nextCard : card,
      ),
    }))
  }

  const addCard = () => {
    setSubmitError('')
    setForm((current) => ({
      ...current,
      cards: [createBlankCard(), ...current.cards],
    }))
  }

  const removeCard = (index) => {
    setSubmitError('')
    setForm((current) => ({
      ...current,
      cards: current.cards.filter((_, cardIndex) => cardIndex !== index),
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSubmitted(true)
    setSubmitError('')
    if (Object.keys(errors).length) return

    setIsSaving(true)
    try {
      await onSubmit(form)
    } catch (error) {
      setSubmitError(error.message || 'Не удалось сохранить набор')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form className="form-stack" onSubmit={handleSubmit}>
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Информация о наборе</h2>
            <p>Название, тема, уровень и доступность.</p>
          </div>
          <label className="switch">
            <input
              checked={form.isPublished}
              onChange={(event) => update('isPublished', event.target.checked)}
              type="checkbox"
            />
            <span>Публичный набор</span>
          </label>
        </div>

        <div className="form-grid">
          <Input
            error={submitted ? errors.name : ''}
            label="Название"
            onChange={(event) => update('name', event.target.value)}
            value={form.name}
          />
          <Select
            label="Уровень"
            onChange={(event) => update('level', event.target.value)}
            value={form.level}
          >
            <option value="A1">A1</option>
            <option value="A2">A2</option>
            <option value="B1">B1</option>
            <option value="B2">B2</option>
            <option value="C1">C1</option>
          </Select>
        </div>

        <Textarea
          error={submitted ? errors.description : ''}
          label="Описание"
          onChange={(event) => update('description', event.target.value)}
          rows={3}
          value={form.description}
        />

        <div className="field">
          <label>Теги</label>
          <div className="tag-row">
            {form.tags.map((tag) => (
              <button
                className="badge badge--green"
                key={tag}
                onClick={() => removeTag(tag)}
                type="button"
              >
                {tag} <X aria-hidden="true" size={14} />
              </button>
            ))}
          </div>
          <div className="form-grid">
            <Input
              error={submitted ? errors.tags : ''}
              label="Новый тег"
              onChange={(event) => setTagInput(event.target.value)}
              onKeyDown={handleTagKeyDown}
              placeholder="например: работа"
              value={tagInput}
            />
            <Button icon={Plus} onClick={addTag} type="button" variant="secondary">
              Добавить тег
            </Button>
          </div>
        </div>
      </section>

      <div className="form-actions">
        <Button disabled={isSaving} icon={Save} type="submit">
          {isSaving ? 'Сохранение...' : 'Сохранить набор'}
        </Button>
      </div>

      {submitError ? <p className="field__error">{submitError}</p> : null}
      {submitted && errors.cards ? <p className="field__error">{errors.cards}</p> : null}

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Карточки</h2>
            <p>Слова, устойчивые обороты, примеры и сложность.</p>
          </div>
          <Button onClick={addCard} type="button" variant="secondary">
            Добавить карточку
          </Button>
        </div>
        <div className="flashcard-editor-list">
          {form.cards.map((card, index) => (
            <FlashcardEditor
              card={card}
              displayNumber={form.cards.length - index}
              error={submitted ? errors[`card-${index}`] : ''}
              key={card.id || card.draftId || index}
              onChange={(nextCard) => handleCardChange(index, nextCard)}
              onRemove={() => removeCard(index)}
            />
          ))}
        </div>
      </section>
    </form>
  )
}
