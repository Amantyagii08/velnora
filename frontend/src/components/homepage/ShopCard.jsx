import { ArrowRight, Clock3, MapPin, Star } from 'lucide-react';
import { Media } from './Media.jsx';

export default function ShopCard({ shop, productCount, onBrowse, priority = false }) {
  return <article className="shop-card">
    <Media src={shop.image} alt={`${shop.name} shop`} className="shop-media" fallbackLabel={shop.name} priority={priority} />
    <div className="shop-card-copy"><div className="shop-heading"><div><p className="product-brand">{shop.category}</p><h3>{shop.name}</h3></div><span className="shop-rating"><Star size={14} fill="currentColor" strokeWidth={1.5} /> {shop.rating}</span></div>
      <div className="shop-details"><span><MapPin size={15} />{shop.area}</span><span><Clock3 size={15} />{shop.delivery}</span></div>
      <button type="button" className="shop-browse" onClick={onBrowse}>Explore storefront <ArrowRight size={16} /></button>
    </div>
  </article>;
}
