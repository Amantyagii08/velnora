import { ArrowRight, MapPin, Menu, ShoppingBag, X } from 'lucide-react';
import { useState } from 'react';

export default function Navbar({ cart, cartCount, onQuantityChange, onOpenClearance }) {
  const [cartOpen, setCartOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const links = [['#categories', 'Categories'], ['#shops', 'Local shops'], ['#products', 'Popular products'], ['#why-velnora', 'Why Velnora']];
  return (
    <header className="site-header">
      <div className="site-header-inner ui-container">
        <a className="brand-link" href="#top" aria-label="Velnora home"><img src="/velnora-logo.svg" alt="Velnora" /><span className="brand-tagline">Your Neighbourhood. Better Choices.</span></a>
        <nav className={`main-nav${menuOpen ? ' is-open' : ''}`} aria-label="Main navigation">
          {links.map(([href, label]) => <a key={href} href={href} onClick={() => setMenuOpen(false)}>{label}</a>)}
          <button type="button" className="clearance-nav-button" onClick={() => { setMenuOpen(false); onOpenClearance(); }}>Clearance Deals</button>
        </nav>
        <div className="header-actions">
          <span className="location-label"><MapPin size={16} strokeWidth={1.8} /> Bengaluru</span>
          <button type="button" className="cart-trigger" onClick={() => setCartOpen((open) => !open)} aria-expanded={cartOpen} aria-label={`Shopping bag, ${cartCount} items`}>
            <ShoppingBag size={19} strokeWidth={1.8} /><span className="cart-label">Your bag</span><span className="cart-count">{cartCount}</span>
          </button>
          <button type="button" className="mobile-menu-trigger" onClick={() => setMenuOpen((open) => !open)} aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}>{menuOpen ? <X /> : <Menu />}</button>
        </div>
      </div>
      {cartOpen && <div className="cart-popover">
        <div className="cart-popover-head"><strong>Your bag</strong><button type="button" onClick={() => setCartOpen(false)} aria-label="Close bag"><X size={18} /></button></div>
        {cart.length ? <>
          <div className="cart-lines">{cart.map(({ product, quantity }) => <div className="cart-line" key={product.id}>
            <div><b>{product.name}</b><span>₹{product.price} · {product.unit}</span></div>
            <div className="quantity-control"><button type="button" aria-label={`Remove one ${product.name}`} onClick={() => onQuantityChange(product.id, -1)}>−</button><span>{quantity}</span><button type="button" aria-label={`Add one ${product.name}`} onClick={() => onQuantityChange(product.id, 1)}>+</button></div>
          </div>)}</div>
          <p className="cart-total"><span>Subtotal</span><b>₹{cart.reduce((sum, line) => sum + line.product.price * line.quantity, 0).toLocaleString('en-IN')}</b></p>
          <p className="cart-note">Checkout will be available in a later step.</p>
        </> : <p className="cart-empty">Your bag is waiting for a good find.</p>}
        <a className="cart-continue" href="#products" onClick={() => setCartOpen(false)}>Continue exploring <ArrowRight size={16} /></a>
      </div>}
    </header>
  );
}
