import { ArrowLeft, Clock3, MapPin, Search, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import ProductCard from './ProductCard.jsx';
import { Media } from './Media.jsx';
import { Input } from '../ui.jsx';

export default function ShopDetailView({ shop, products, cart, onAdd, onClose, onViewProduct }) {
  const [query, setQuery] = useState('');
  const filteredProducts = useMemo(() => {
    const term = query.trim().toLowerCase();
    return products.filter((product) => !term || [product.name, product.brand, product.variant, product.unit].some((value) => value?.toLowerCase().includes(term)));
  }, [products, query]);
  if (!shop) return null;
  return <div className="view-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="listing-view" role="dialog" aria-modal="true" aria-labelledby="shop-view-title">
      <div className="listing-view-top"><button type="button" className="view-back-button" onClick={onClose}><ArrowLeft size={17} /> Back to local shops</button><button type="button" className="view-close-button" onClick={onClose} aria-label="Close shop view"><X size={20} /></button></div>
      <div className="shop-detail-heading"><Media src={shop.image} alt={shop.name} fallbackLabel={shop.name} className="shop-detail-media" /><div><p className="product-brand">{shop.category}</p><h2 id="shop-view-title">{shop.name}</h2><p>{shop.description}</p><div className="shop-details">{shop.area && <span><MapPin size={15} />{shop.area}</span>}<span><Clock3 size={15} />{shop.delivery}</span></div></div></div>
      <div className="shop-products-toolbar"><div className="listing-view-heading"><div><p className="eyebrow">From this local shop</p><h3>Shop favourites</h3></div><span>{filteredProducts.length} of {products.length} products</span></div><label className="shop-product-search"><Search size={17} aria-hidden="true" /><Input aria-label={`Search products at ${shop.name}`} placeholder="Search this shop’s products..." value={query} onChange={(event) => setQuery(event.target.value)} /></label></div>
      {filteredProducts.length ? <div className="product-grid">{filteredProducts.map((product) => <ProductCard key={product.id} product={product} shop={shop} onAdd={onAdd} onView={onViewProduct} quantity={cart.find((line) => line.product.id === product.id)?.quantity || 0} />)}</div> : <p className="shop-detail-empty">{query ? `No products at ${shop.name} match “${query}”. Try another name, brand, or pack size.` : 'This shop’s products will be added soon.'}</p>}
    </section>
  </div>;
}
