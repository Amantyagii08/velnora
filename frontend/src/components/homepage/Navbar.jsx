import { ArrowRight, ChevronDown, Menu, ShoppingBag, UserRound, X } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function Navbar({ cart, cartCount, onQuantityChange, onOpenClearance, onOpenCart, user, onLogout }) {
  const [cartOpen, setCartOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const links = [['/#categories', 'Categories'], ['/#shops', 'Shops'], ['/#products', 'Popular products'], ['/#why-velnora', 'Why Velnora']];
  return (
    <header className="site-header">
      <div className="site-header-inner ui-container">
        <a className="brand-link" href="/" aria-label="Velnora home"><img src="/velnora-logo.svg" alt="Velnora" /></a>
        <nav id="main-navigation" className={`main-nav${menuOpen ? ' is-open' : ''}`} aria-label="Main navigation">
          {links.map(([href, label]) => <a key={href} href={href} onClick={() => setMenuOpen(false)}>{label}</a>)}
          <button type="button" className="clearance-nav-button" onClick={() => { setMenuOpen(false); onOpenClearance(); }}>Clearance Deals</button>
        </nav>
        <div className="header-actions">
          {user ? <div className="account-menu"><button type="button" className="profile-trigger" aria-haspopup="menu" aria-expanded={profileOpen} onClick={() => setProfileOpen((open) => !open)}><UserRound size={18} /><span>{user.name}</span><ChevronDown size={15} /></button>{profileOpen && <div className="profile-menu" role="menu"><div className="profile-menu-heading">Signed in as <b>{user.name}</b></div>{user.role === 'customer' && <Link role="menuitem" to="/orders" onClick={() => setProfileOpen(false)}>My Orders</Link>}<Link role="menuitem" to="/account" onClick={() => setProfileOpen(false)}>Account settings</Link><button role="menuitem" type="button" onClick={() => { setProfileOpen(false); onLogout(); }}>Log out</button></div>}</div> : <a className="account-login" href="/login"><UserRound size={17} /> <span>Sign in</span></a>}
          <button type="button" className="cart-trigger" onClick={() => onOpenCart ? onOpenCart() : setCartOpen((open) => !open)} aria-expanded={cartOpen} aria-label={`Shopping bag, ${cartCount} items`}>
            <ShoppingBag size={19} strokeWidth={1.8} /><span className="cart-label">Your bag</span><span className="cart-count">{cartCount}</span>
          </button>
          <button type="button" className="mobile-menu-trigger" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen} aria-controls="main-navigation" aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}><Menu className={`menu-icon${menuOpen ? ' is-hidden' : ''}`} /><X className={`menu-icon${menuOpen ? ' is-visible' : ''}`} /></button>
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
          <p className="cart-note">Your bag stays available as you browse.</p>
        </> : <p className="cart-empty">Your bag is waiting for a good find.</p>}
          {onOpenCart ? <button type="button" className="cart-continue" onClick={() => { setCartOpen(false); onOpenCart(); }}>View your bag <ArrowRight size={16} /></button> : <a className="cart-continue" href="#products" onClick={() => setCartOpen(false)}>Continue exploring <ArrowRight size={16} /></a>}
      </div>}
    </header>
  );
}
