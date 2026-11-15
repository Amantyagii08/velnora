import { ArrowLeft, Clock3, MapPin, X } from 'lucide-react';
import ProductCard from './ProductCard.jsx';
import { Media } from './Media.jsx';

export default function ShopDetailView({ shop, products, cart, onAdd, onClose }) {
  if (!shop) return null;
  return <div className="view-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="listing-view" role="dialog" aria-modal="true" aria-labelledby="shop-view-title">
      <div className="listing-view-top"><button type="button" className="view-back-button" onClick={onClose}><ArrowLeft size={17} /> Back to local shops</button><button type="button" className="view-close-button" onClick={onClose} aria-label="Close shop view"><X size={20} /></button></div>
      <div className="shop-detail-heading"><Media src={shop.image} alt={shop.name} fallbackLabel={shop.name} className="shop-detail-media" /><div><p className="product-brand">{shop.category}</p><h2 id="shop-view-title">{shop.name}</h2><p>{shop.description}</p><div className="shop-details"><span><MapPin size={15} />{shop.area}, Bengaluru</span><span><Clock3 size={15} />{shop.delivery}</span></div></div></div>
      <div className="listing-view-heading"><div><p className="eyebrow">From this local shop</p><h3>Shop favourites</h3></div><span>{products.length} products</span></div>
      {products.length ? <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} shop={shop} onAdd={onAdd} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div> : <p className="shop-detail-empty">This shop’s products will be added soon.</p>}
    </section>
  </div>;
}
