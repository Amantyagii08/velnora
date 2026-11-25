import { Heart, Plus } from 'lucide-react';
import { useState } from 'react';
import { Media } from './Media.jsx';

export default function ProductCard({ product, shop, onAdd, onView, quantity = 0, compact = false, priority = false }) {
  const [saved, setSaved] = useState(false);
  return <article className={`product-card${compact ? ' product-card--compact' : ''}`}>
    <div className="product-media-wrap">
      <button type="button" className="product-media-button" onClick={() => onView?.(product)} aria-label={`View details for ${product.name}`}><Media src={product.image} alt={product.name} fallbackLabel={product.name} className="product-media" priority={priority} /></button>
      {product.tag && <span className="product-tag">{product.tag}</span>}
      <button type="button" className={`save-product${saved ? ' is-saved' : ''}`} onClick={() => setSaved((value) => !value)} aria-label={saved ? `Remove ${product.name} from saved items` : `Save ${product.name}`} aria-pressed={saved}><Heart size={17} fill={saved ? 'currentColor' : 'none'} strokeWidth={1.8} /></button>
    </div>
    <div className="product-info">
      <div className="product-title-row"><div><p className="product-brand">{product.brand}</p><h3><button type="button" className="product-name-button" onClick={() => onView?.(product)}>{product.name}</button></h3></div></div>
      <p className="product-shop">From {shop?.name || 'a nearby shop'}</p>
      <div className="product-bottom"><div className="product-price"><b>₹{product.price}</b><span> / {product.unit}</span>{product.wasPrice && <del>₹{product.wasPrice}</del>}</div>
        <button type="button" className={`add-product${quantity ? ' has-added' : ''}`} onClick={() => onAdd(product)} disabled={product.stock < 1 || product.available === false} aria-label={product.available === false ? `${product.name} is unavailable` : `Add ${product.name} to bag`}>{quantity ? <><span>{quantity}</span><Plus size={15} /></> : <Plus size={18} />}</button>
      </div>
    </div>
  </article>;
}
