import { useState } from 'react';
import { ArrowLeft, ArrowRight, Check, MapPin, PackageCheck, ShoppingBag, Store } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Card, Container } from '../components/ui.jsx';
import Navbar from '../components/homepage/Navbar.jsx';
import Footer from '../components/homepage/Footer.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useMarketplaceData } from '../context/MarketplaceDataContext.jsx';
import { useOrders } from '../context/OrderContext.jsx';

const addressKey = (userId) => `velnora-addresses-v1:${userId}`;
const money = (amount) => `₹${Number(amount || 0).toLocaleString('en-IN')}`;
function loadAddresses(userId) { try { const value = JSON.parse(localStorage.getItem(addressKey(userId)) || '[]'); return Array.isArray(value) ? value : []; } catch { return []; } }
function serializeAddress(address) {
  return [address.name, address.line1, address.line2, [address.city, address.state, address.pincode].filter(Boolean).join(', '), address.phone ? `Contact ${address.phone}` : ''].filter(Boolean).join(', ');
}

export default function OrderSummaryPage() {
  const cart = useCart();
  const { refreshOrders } = useOrders();
  const { shops } = useMarketplaceData();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const addresses = loadAddresses(user?.id || user?.email || 'customer');
  const address = addresses.find((entry) => entry.id === location.state?.addressId);
  const fulfillmentType = location.state?.fulfillmentType === 'PICKUP' ? 'PICKUP' : 'DELIVERY';
  const firstShopId = cart.items[0]?.product.shopId;
  const shop = shops.find((entry) => String(entry.id) === String(firstShopId));
  const oneShop = cart.items.every((line) => String(line.product.shopId) === String(firstShopId));
  const savings = Math.max(0, cart.originalTotal - cart.subtotal);

  async function confirmOrder() {
    if (submitting || cart.busy) return;
    if (!cart.items.length || !oneShop) { setMessage('Your bag must contain items from one shop. Return to your bag and review it.'); return; }
    if (fulfillmentType === 'DELIVERY' && !address) { setMessage('Choose a delivery address before confirming your order.'); return; }
    if (fulfillmentType === 'DELIVERY' && serializeAddress(address).length > 255) { setMessage('This delivery address is too long. Shorten it to 255 characters or fewer.'); return; }
    if (fulfillmentType === 'PICKUP' && !shop?.address && !shop?.area) { setMessage('The shop pickup address is unavailable. Return to checkout for another option.'); return; }
    setSubmitting(true); setMessage('');
    const result = await cart.placeOrder(fulfillmentType, fulfillmentType === 'DELIVERY' ? serializeAddress(address) : null);
    if (!result.ok) { setMessage(result.error); setSubmitting(false); return; }
    try { sessionStorage.setItem('velnora-last-created-order-id-v1', result.order.id); } catch { /* Route state carries the order for this session. */ }
    await refreshOrders();
    navigate('/order-confirmation', { state: { order: result.order }, replace: true });
  }

  if (!cart.items.length) return <div className="commerce-page"><Navbar cart={[]} cartCount={0} onQuantityChange={() => {}} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Card className="commerce-empty"><span><ShoppingBag size={24}/></span><h1>Your bag is empty</h1><p>Add products before reviewing your order.</p><Button onClick={() => navigate('/')}>Browse products<ArrowRight size={17}/></Button></Card></Container></main><Footer/></div>;

  return <div className="commerce-page"><Navbar cart={cart.items.map(({ product, quantity }) => ({ product, quantity }))} cartCount={cart.count} onQuantityChange={(id, change) => { const line = cart.items.find((item) => item.product.id === id); if (line) cart.setQuantity(line.key, line.quantity + change); }} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Link className="commerce-back" to="/checkout" state={{ addressId: address?.id, fulfillmentType }}><ArrowLeft size={16}/> Back to fulfillment</Link><div className="commerce-heading"><p className="eyebrow">Checkout · Step 2 of 2</p><h1>Review your order</h1><p>Confirm items from {shop?.name || 'your selected shop'}.</p></div><div className="order-review-layout"><section className="order-review-items"><div className="checkout-section-head"><span className="checkout-icon"><ShoppingBag size={19}/></span><div><h2>Items from one shop</h2><p>{cart.count} item{cart.count === 1 ? '' : 's'} · {shop?.name || 'Selected shop'}</p></div></div>{cart.items.map(({ key, product, quantity }) => <Card className="review-item" key={key}><img src={product.image} alt={product.name}/><div><span className="eyebrow">{shop?.name || 'Selected shop'}</span><h3>{product.name}</h3><p>{[product.brand, product.variant, product.unit].filter(Boolean).join(' · ')}</p><small>{quantity} × {money(product.price)}</small></div><b>{money(product.price * quantity)}</b></Card>)}</section><Card className="review-summary"><p className="eyebrow">Order summary</p><h2>Price details</h2><div className="summary-row"><span>Items total</span><b>{money(cart.originalTotal)}</b></div>{savings > 0 && <div className="summary-row summary-savings"><span>Product savings</span><b>−{money(savings)}</b></div>}<div className="summary-total"><span>Order total</span><b>{money(cart.subtotal)}</b></div><p className="summary-note">The final total and stock check are confirmed by Velnora when you place the order. No payment is collected online.</p>{fulfillmentType === 'DELIVERY' ? address ? <div className="review-address"><MapPin size={17}/><span><b>Shop delivery to {address.name}</b><small>{serializeAddress(address)}</small></span><Check size={16}/></div> : <div className="review-address-missing"><p>Select an address to continue.</p><Link to="/checkout" state={{ fulfillmentType }}>Choose delivery address<ArrowRight size={15}/></Link></div> : <div className="review-address"><Store size={17}/><span><b>Customer pickup</b><small>{shop?.name}</small><small>{shop?.address || shop?.area}</small></span><Check size={16}/></div>}{message && <p className="checkout-message" role="alert">{message}</p>}{cart.needsOrderCheck && <Link className="commerce-back" to="/orders">Check My Orders before trying again<ArrowRight size={15}/></Link>}<Button className="summary-action" onClick={confirmOrder} disabled={submitting || cart.busy || cart.needsOrderCheck || !oneShop || (fulfillmentType === 'DELIVERY' && !address)}>{submitting || cart.busy ? 'Placing order…' : 'Place order'}<PackageCheck size={17}/></Button></Card></div></Container></main><Footer/></div>;
}
