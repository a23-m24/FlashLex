import { CheckCircle2, Keyboard, Languages, Repeat2 } from 'lucide-react'

const directionModes = [
  {
    value: 'DIRECT_TRANSLATION',
    icon: Languages,
    label: 'Прямой перевод',
    text: 'Английское слово или фраза, ответ на русском.',
  },
  {
    value: 'REVERSE_TRANSLATION',
    icon: Repeat2,
    label: 'Обратный перевод',
    text: 'Русский перевод, нужно вспомнить английский.',
  },
]

const answerModes = [
  {
    value: 'TEXT_INPUT',
    icon: Keyboard,
    label: 'Ввод',
    text: 'Сначала вводите ответ, затем оцениваете качество.',
  },
  {
    value: 'SELF_CHECK',
    icon: CheckCircle2,
    label: 'Самопроверка',
    text: 'Сначала показываете ответ, затем оцениваете себя.',
  },
]

function ModeGroup({ title, items, value, onChange }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="mode-grid mode-grid--two">
        {items.map((mode) => (
          <button
            className={`mode-card ${value === mode.value ? 'mode-card--active' : ''}`}
            key={mode.value}
            onClick={() => onChange(mode.value)}
            type="button"
          >
            <mode.icon aria-hidden="true" size={22} />
            <strong>{mode.label}</strong>
            <span>{mode.text}</span>
          </button>
        ))}
      </div>
    </section>
  )
}

export function TrainingModeSelector({
  answerMode,
  directionMode,
  onAnswerModeChange,
  onDirectionModeChange,
}) {
  return (
    <div className="mode-settings">
      <ModeGroup
        items={directionModes}
        onChange={onDirectionModeChange}
        title="Направление"
        value={directionMode}
      />
      <ModeGroup
        items={answerModes}
        onChange={onAnswerModeChange}
        title="Формат ответа"
        value={answerMode}
      />
    </div>
  )
}
