import { ArrowRight, BadgePercent, Plus } from 'lucide-react';
import { Media } from './Media.jsx';

export default function ClearanceDealCard({ product, shop, onAdd, onView, quantity = 0 }) {
  const originalPrice = product.wasPrice ?? product.price;
  const discount = originalPrice - product.price;
  const stockText = product.stock === 1 ? '1 left in stock' : `${product.stock} in stock`;
  return <article className="clearance-list-card">
    <button type="button" className="clearance-list-image" onClick={() => onView(product)} aria-label={`View details for ${product.name}`}><Media src={product.image} alt={product.name} fallbackLabel={product.name} className="clearance-list-media" /></button>
    <div className="clearance-list-copy">
      <span className="clearance-list-label"><BadgePercent size={15} /> Clearance</span>
      <p className="product-brand">{product.brand}</p>
      <h3><button type="button" className="product-name-button" onClick={() => onView(product)}>{product.name}</button></h3>
      <p className="clearance-list-shop">{shop?.name || 'Local shop'}</p>
      <div className="clearance-list-prices"><b>₹{product.price.toLocaleString('en-IN')}</b><del>₹{originalPrice.toLocaleString('en-IN')}</del><span className="clearance-flat-discount">Save ₹{discount.toLocaleString('en-IN')}</span></div>
      <p className="clearance-stock">{stockText}</p>
      <div className="clearance-list-actions"><button type="button" className="clearance-details-link" onClick={() => onView(product)}>View details <ArrowRight size={15} /></button><button type="button" className={`add-product${quantity ? ' has-added' : ''}`} onClick={() => onAdd(product)} disabled={product.stock < 1 || product.available === false} aria-label={product.available === false ? `${product.name} is unavailable` : `Add ${product.name} to bag`}>{quantity ? <><span>{quantity}</span><Plus size={15} /></> : <Plus size={18} />}</button></div>
    </div>
  </article>;
}
