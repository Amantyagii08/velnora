import { ArrowLeft, BadgePercent, MapPin, PackageCheck, Star, X } from 'lucide-react';
import { Media } from './Media.jsx';

export default function ProductDetailView({ product, shop, comparisons, comparisonLoading = false, comparisonError = '', shopsById, onBack, backLabel, onSelectProduct, onAdd, quantity = 0 }) {
  if (!product) return null;
  const comparable = comparisons.length > 1;
  const lowestPrice = Math.min(...comparisons.map((item) => item.price));
  const discount = product.wasPrice ? product.wasPrice - product.price : 0;
  return <div className="view-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onBack(); }}>
    <section className="listing-view product-detail-view" role="dialog" aria-modal="true" aria-labelledby="product-detail-title">
      <div className="listing-view-top"><button type="button" className="view-back-button" onClick={onBack}><ArrowLeft size={17} /> {backLabel}</button><button type="button" className="view-close-button" onClick={onBack} aria-label="Close product details"><X size={20} /></button></div>
      <div className="product-detail-main">
        <Media src={product.image} alt={product.name} fallbackLabel={product.name} className="product-detail-media" />
        <div className="product-detail-copy">
          {product.clearance && <span className="clearance-list-label"><BadgePercent size={15} /> Clearance</span>}
          <p className="product-brand">{product.brand}</p>
          <h2 id="product-detail-title">{product.name}</h2>
          <p className="product-detail-shop"><MapPin size={16} />{shop?.name || 'Local shop'}{shop?.area ? ` · ${shop.area}` : ''}</p>
          <p className="product-detail-description">{product.variant} · {product.unit}</p>
          <div className="product-detail-price"><b>₹{product.price.toLocaleString('en-IN')}</b>{product.wasPrice && <><del>₹{product.wasPrice.toLocaleString('en-IN')}</del><span>Save ₹{discount.toLocaleString('en-IN')}</span></>}</div>
          <p className="product-detail-stock"><PackageCheck size={16} /> {product.available === false ? 'Currently unavailable from this shop' : product.stock > 0 ? `${product.stock} available from this shop` : 'Currently out of stock'}</p>
          <button type="button" className="product-detail-add" onClick={() => onAdd(product)} disabled={product.stock < 1 || product.available === false}>{quantity ? `In your bag · ${quantity}` : 'Add to bag'}</button>
        </div>
      </div>
      <section className="comparison-section" aria-labelledby="comparison-heading">
        <div className="listing-view-heading"><div><p className="eyebrow">Compare local prices</p><h3 id="comparison-heading">Same product, nearby shops</h3></div>{comparable && <span>{comparisons.length} shops</span>}</div>
        {comparisonLoading ? <p className="comparison-empty" role="status">Checking prices across local shops…</p> : comparable ? <div className="comparison-grid">{comparisons.map((item) => {
          const itemShop = shopsById.get(item.shopId);
          const isSelected = item.id === product.id;
          const isLowest = comparable && item.price === lowestPrice;
          return <article className={`comparison-card${isSelected ? ' is-selected' : ''}${isLowest ? ' is-lowest' : ''}`} key={item.id}>
            <div className="comparison-card-top"><span>{itemShop?.name || 'Local shop'}</span>{isLowest && <span className="lowest-price-badge"><Star size={13} fill="currentColor" /> Lowest price</span>}</div>
            <p>{item.brand} · {item.variant} · {item.unit}</p>
            <div className="comparison-price"><b>₹{item.price.toLocaleString('en-IN')}</b>{item.wasPrice && <del>₹{item.wasPrice.toLocaleString('en-IN')}</del>}</div>
            <small>{item.stock > 0 ? `${item.stock} available` : 'Out of stock'}</small>
            {!isSelected && <button type="button" className="comparison-open" onClick={() => onSelectProduct(item)}>View this shop <ArrowLeft size={14} /></button>}
          </article>;
        })}</div> : <p className="comparison-empty" role={comparisonError ? 'alert' : undefined}>{comparisonError || 'No direct price comparison is available for this product yet.'}</p>}
      </section>
    </section>
  </div>;
}
