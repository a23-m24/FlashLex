import { Search } from 'lucide-react'
import { Input } from '../../shared/ui/Input'
import { Select } from '../../shared/ui/Select'

export function DeckFilters({ filters, onChange, tags, levels, sortOptions }) {
  const update = (field, value) => onChange({ ...filters, [field]: value })

  return (
    <section className="toolbar">
      <div className="toolbar__search">
        <Search aria-hidden="true" size={18} />
        <Input
          label="Поиск"
          onChange={(event) => update('query', event.target.value)}
        placeholder="Название, тэг, автор"
          value={filters.query}
        />
      </div>
      <Select
        label="Тэг"
        onChange={(event) => update('tag', event.target.value)}
        value={filters.tag}
      >
        <option value="">Все тэги</option>
        {tags.map((tag) => (
          <option key={tag} value={tag}>
            {tag}
          </option>
        ))}
      </Select>
      <Select
        label="Уровень"
        onChange={(event) => update('level', event.target.value)}
        value={filters.level}
      >
        <option value="">Все уровни</option>
        {levels.map((level) => (
          <option key={level} value={level}>
            {level}
          </option>
        ))}
      </Select>
      <Select
        label="Сортировка"
        onChange={(event) => update('sort', event.target.value)}
        value={filters.sort}
      >
        {sortOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </Select>
    </section>
  )
}
