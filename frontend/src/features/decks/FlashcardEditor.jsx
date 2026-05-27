import { Trash2 } from 'lucide-react'
import { Button } from '../../shared/ui/Button'
import { Input } from '../../shared/ui/Input'
import { Select } from '../../shared/ui/Select'
import { Textarea } from '../../shared/ui/Textarea'

export function FlashcardEditor({ card, displayNumber, error, onChange, onRemove }) {
  const update = (field, value) => onChange({ ...card, [field]: value })

  return (
    <article className="flashcard-editor">
      <div className="flashcard-editor__head">
        <strong>Карточка {displayNumber}</strong>
        <Button icon={Trash2} onClick={onRemove} variant="danger">
          Удалить
        </Button>
      </div>
      <div className="form-grid">
        <Input
          label="Английский"
          onChange={(event) => update('englishWord', event.target.value)}
          value={card.englishWord}
        />
        <Input
          label="Перевод"
          onChange={(event) => update('translation', event.target.value)}
          value={card.translation}
        />
        <Input
          label="Транскрипция"
          onChange={(event) => update('transcription', event.target.value)}
          value={card.transcription}
        />
        <Select
          label="Тип"
          onChange={(event) => update('phraseType', event.target.value)}
          value={card.phraseType}
        >
          <option value="word">Слово</option>
          <option value="phrase">Фраза</option>
          <option value="collocation">Устойчивое сочетание</option>
          <option value="phrasal_verb">Фразовый глагол</option>
          <option value="idiom">Идиома</option>
        </Select>
        <Select
          label="Сложность"
          onChange={(event) => update('difficulty', event.target.value)}
          value={card.difficulty}
        >
          <option value="easy">Легко</option>
          <option value="medium">Средне</option>
          <option value="hard">Сложно</option>
        </Select>
      </div>
      <Textarea
        label="Пример"
        onChange={(event) => update('exampleSentence', event.target.value)}
        rows={2}
        value={card.exampleSentence}
      />
      {error ? <p className="field__error">{error}</p> : null}
    </article>
  )
}
