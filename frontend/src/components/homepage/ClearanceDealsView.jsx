import { ArrowLeft, BadgePercent, X } from 'lucide-react';
import ProductCard from './ProductCard.jsx';

export default function ClearanceDealsView({ products, shopById, cart, onAdd, onClose }) {
  return <div className="view-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="listing-view" role="dialog" aria-modal="true" aria-labelledby="clearance-view-title">
      <div className="listing-view-top"><button type="button" className="view-back-button" onClick={onClose}><ArrowLeft size={17} /> Back to Velnora</button><button type="button" className="view-close-button" onClick={onClose} aria-label="Close clearance deals"><X size={20} /></button></div>
      <div className="clearance-view-heading"><span className="clearance-view-icon"><BadgePercent size={21} /></span><div><p className="eyebrow">Thoughtful local value</p><h2 id="clearance-view-title">Clearance Deals</h2><p>Good local finds at a little better price. While these offers last.</p></div></div>
      <div className="listing-view-heading"><div><p className="eyebrow">A good find, close by</p><h3>Current local offers</h3></div><span>{products.length} offers</span></div>
      {products.length ? <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} shop={shopById.get(product.shopId)} onAdd={onAdd} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div> : <p className="shop-detail-empty">There are no clearance deals at the moment. Check back soon.</p>}
    </section>
  </div>;
}
