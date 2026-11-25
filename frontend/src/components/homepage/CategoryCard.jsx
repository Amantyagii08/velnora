import { ArrowUpRight } from 'lucide-react';
import { Media } from './Media.jsx';

export default function CategoryCard({ category, selected, onSelect }) {
  return <button type="button" className={`category-card${selected ? ' is-selected' : ''}`} onClick={() => onSelect(category.id)} aria-pressed={selected}>
    <Media src={category.image} alt={category.name} fallbackLabel={category.name} className="category-media" priority />
    <span className="category-card-copy"><span><b>{category.name}</b><small>{category.note}</small></span><ArrowUpRight size={18} strokeWidth={1.8} /></span>
  </button>;
}
