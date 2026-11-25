import { BadgePercent, ArrowRight } from 'lucide-react';
import ProductCard from './ProductCard.jsx';

export default function ClearanceCard({ product, shop, onAdd, onView, quantity, priority = false }) {
  return <div className="clearance-card"><div className="clearance-card-label"><BadgePercent size={15} /> A good find</div><ProductCard product={product} shop={shop} onAdd={onAdd} onView={onView} quantity={quantity} compact priority={priority} /><a className="clearance-more" href="#products">More good finds <ArrowRight size={15} /></a></div>;
}
