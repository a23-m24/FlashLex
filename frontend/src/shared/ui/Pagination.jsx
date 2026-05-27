import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'

export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="pagination">
      <Button
        disabled={page === 1}
        icon={ChevronLeft}
        onClick={() => onChange(page - 1)}
        variant="ghost"
      >
        Назад
      </Button>
      <span>
        {page} из {totalPages}
      </span>
      <Button
        disabled={page === totalPages}
        icon={ChevronRight}
        onClick={() => onChange(page + 1)}
        variant="ghost"
      >
        Вперед
      </Button>
    </div>
  )
}
