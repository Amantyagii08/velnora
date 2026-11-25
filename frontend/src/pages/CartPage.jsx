import { ArrowLeft, ArrowRight, Minus, Plus, ShoppingBag, Trash2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Card, Container } from '../components/ui.jsx';
import Navbar from '../components/homepage/Navbar.jsx';
import Footer from '../components/homepage/Footer.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useMarketplaceData } from '../context/MarketplaceDataContext.jsx';

const money = (value) => `₹${value.toLocaleString('en-IN')}`;

function Summary({ items, subtotal, originalTotal, action, actionLabel, disabled = false }) {
  const savings = originalTotal - subtotal;
  return <Card className="order-summary"><p className="eyebrow">Order summary</p><h2>Price details</h2><div className="summary-row"><span>Items ({items.reduce((sum, item) => sum + item.quantity, 0)})</span><b>{money(originalTotal)}</b></div>{savings > 0 && <div className="summary-row summary-savings"><span>Product savings</span><b>−{money(savings)}</b></div>}<div className="summary-row"><span>Delivery</span><span>Arranged by the shop</span></div><div className="summary-total"><span>Subtotal</span><b>{money(subtotal)}</b></div><p className="summary-note">The current checkout does not apply a delivery charge or collect online payment.</p>{action && <Button className="summary-action" onClick={action} disabled={disabled}>{actionLabel}<ArrowRight size={17} /></Button>}</Card>;
}

export default function CartPage() {
  const { items, count, subtotal, originalTotal, setQuantity } = useCart();
  const { shops } = useMarketplaceData();
  const shopMap = new Map(shops.map((shop) => [shop.id, shop]));
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return <div className="commerce-page"><Navbar cart={items.map(({ product, quantity }) => ({ product, quantity }))} cartCount={count} onQuantityChange={(id, change) => { const line = items.find((item) => item.product.id === id); if (line) setQuantity(line.key, line.quantity + change); }} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Link className="commerce-back" to="/"><ArrowLeft size={16} /> Continue shopping</Link><div className="commerce-heading"><p className="eyebrow">Your selection</p><h1>Your bag<span> ({count})</span></h1><p>Products stay connected to the shops they come from.</p></div>
    {!items.length ? <Card className="commerce-empty"><span><ShoppingBag size={24} /></span><h2>Your bag is empty</h2><p>Explore the shops and add something you love.</p><Button onClick={() => navigate('/')}>Explore Velnora<ArrowRight size={17} /></Button></Card> : <div className="commerce-layout"><section className="cart-items" aria-label="Items in your bag">{items.map(({ key, product, quantity }) => { const shop = shopMap.get(product.shopId); return <Card className="cart-item" key={key}><img src={product.image} alt={product.name} /><div className="cart-item-info"><span className="eyebrow">{shop?.name || 'Velnora shop'}</span><h2>{product.name}</h2><p>{product.brand} · {product.variant} · {product.unit}</p><div className="cart-item-prices"><b>{money(product.price)}</b>{product.wasPrice && <del>{money(product.wasPrice)}</del>}{product.wasPrice && <span>Save {money(product.wasPrice-product.price)}</span>}</div><small>{product.stock} available</small></div><div className="cart-item-controls"><div className="cart-stepper"><button aria-label={`Decrease ${product.name}`} onClick={() => setQuantity(key, quantity - 1)}><Minus size={15} /></button><span>{quantity}</span><button aria-label={`Increase ${product.name}`} disabled={quantity >= product.stock} onClick={() => setQuantity(key, quantity + 1)}><Plus size={15} /></button></div><button className="cart-remove" onClick={() => setQuantity(key, 0)}><Trash2 size={15} /> Remove</button></div><b className="cart-line-total">{money(product.price * quantity)}</b></Card>; })}</section><Summary items={items} subtotal={subtotal} originalTotal={originalTotal} action={() => navigate('/checkout')} actionLabel="Continue to checkout" /></div>}</Container></main><Footer /></div>;
}
