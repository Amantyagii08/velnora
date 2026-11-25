import { useEffect, useState } from 'react';
import { ArrowRight, Check, MapPin, PackageCheck, Store } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Card, Container } from '../components/ui.jsx';
import Navbar from '../components/homepage/Navbar.jsx';
import Footer from '../components/homepage/Footer.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useOrders } from '../context/OrderContext.jsx';
import { useMarketplaceData } from '../context/MarketplaceDataContext.jsx';

const money = (value) => `₹${Number(value || 0).toLocaleString('en-IN')}`;

export default function OrderConfirmationPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const cart = useCart();
  const { loadOrder, orders } = useOrders();
  const { products, shops } = useMarketplaceData();
  const { user, logout } = useAuth();
  const [order, setOrder] = useState(location.state?.order || null);
  const [loading, setLoading] = useState(!location.state?.order);
  const orderId = location.state?.order?.id || (() => { try { return sessionStorage.getItem('velnora-last-created-order-id-v1'); } catch { return ''; } })();

  useEffect(() => {
    if (!orderId) { setOrder(null); setLoading(false); return; }
    let alive = true;
    const known = orders.find((item) => String(item.id) === String(orderId));
    if (known) { setOrder(known); setLoading(false); return; }
    setLoading(true);
    loadOrder(orderId).then((result) => { if (alive) setOrder(result); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [orderId, orders, loadOrder]);

  const shop = order && shops.find((item) => String(item.id) === String(order.shopId));
  const isPickup = order?.fulfillmentType === 'PICKUP';
  return <div className="commerce-page"><Navbar cart={cart.items.map(({ product, quantity }) => ({ product, quantity }))} cartCount={cart.count} onQuantityChange={() => {}} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Card className="confirmation-card">{loading ? <><span className="confirmation-mark"><PackageCheck size={27}/></span><h1>Loading your order</h1><p className="confirmation-copy">We’re retrieving the confirmation from Velnora.</p></> : order ? <><span className="confirmation-mark"><Check size={28}/></span><p className="eyebrow">Order received</p><h1>Thank you, {order.address?.name || user?.name || 'there'}.</h1><p className="confirmation-copy">Your order is with {order.shopName || shop?.name || 'the shop'}.</p><div className="confirmation-number"><span>Order number</span><b>#{order.id}</b></div><div className="confirmation-lines">{order.lines?.map((line, index) => { const product = products.find((item) => String(item.id) === String(line.productId)); return <div className="confirmation-line" key={`${line.productId}-${index}`}><span><b>{line.quantity} × {line.productName || product?.name || 'Product'}</b><small>{order.shopName || shop?.name}</small></span><strong>{money(line.subtotal ?? line.price * line.quantity)}</strong></div>; })}</div><div className="confirmation-address">{isPickup ? <Store size={18}/> : <MapPin size={18}/>}<span><b>{isPickup ? 'Pickup from' : 'Shop delivery to'}</b><small>{isPickup ? shop?.address || order.pickupAddress || 'See the shop page for its current address.' : order.address?.line1}</small></span></div><div className="summary-total"><span>Order total</span><b>{money(order.subtotal)}</b></div><p className="summary-note">No payment was collected online. Payment handling is not available in this checkout.</p><div className="confirmation-actions"><Button onClick={() => navigate('/orders')}>View My Orders<ArrowRight size={17}/></Button><Link className="confirmation-link" to="/">Back to Velnora</Link></div></> : <><span className="confirmation-mark"><PackageCheck size={27}/></span><h1>No order confirmation found</h1><p className="confirmation-copy">This order may not belong to the current account. View your saved orders or continue shopping.</p><Button onClick={() => navigate('/orders')}>View My Orders<ArrowRight size={17}/></Button></>}</Card></Container></main><Footer/></div>;
}
