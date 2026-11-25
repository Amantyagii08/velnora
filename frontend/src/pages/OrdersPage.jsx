import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ArrowRight, MapPin, PackageCheck, ShoppingBag, Store } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Badge, Button, Card, Container } from '../components/ui.jsx';
import Navbar from '../components/homepage/Navbar.jsx';
import Footer from '../components/homepage/Footer.jsx';
import { useMarketplaceData } from '../context/MarketplaceDataContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useOrders } from '../context/OrderContext.jsx';

const money = (value) => `₹${Number(value || 0).toLocaleString('en-IN')}`;
const filters = [['All orders', 'ALL'], ['Order Placed', 'PLACED'], ['Accepted', 'ACCEPTED'], ['Being Prepared', 'PREPARING'], ['Ready', 'READY'], ['Completed', 'COMPLETED'], ['Cancelled', 'CANCELLED']];
const labels = { PLACED: 'Order Placed', ACCEPTED: 'Accepted by Shop', PREPARING: 'Being Prepared', READY: 'Ready', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
function dateLabel(value) { const date = new Date(value); return Number.isNaN(date.valueOf()) ? 'Date unavailable' : date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }); }
function OrderStatus({ order }) {
  const status = String(order.status || 'PLACED').toUpperCase();
  const label = status === 'READY' ? order.fulfillmentType === 'PICKUP' ? 'Ready for pickup' : 'Ready for dispatch' : status === 'COMPLETED' ? order.fulfillmentType === 'PICKUP' ? 'Collected' : 'Delivered' : labels[status] || status;
  return <Badge tone={status === 'CANCELLED' ? 'neutral' : status === 'COMPLETED' ? 'brass' : 'sapphire'}>{label}</Badge>;
}

export default function OrdersPage() {
  const { orderId } = useParams();
  const details = Boolean(orderId);
  const { orders, updateStatus, loadOrder, refreshOrders, loading, error } = useOrders();
  const { products, shops } = useMarketplaceData();
  const shopMap = new Map(shops.map((shop) => [String(shop.id), shop]));
  const productMap = new Map(products.map((product) => [String(product.id), product]));
  const cart = useCart();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [filter, setFilter] = useState('ALL');
  const [notice, setNotice] = useState('');
  const [detailLoading, setDetailLoading] = useState(false);
  const selected = orders.find((order) => String(order.id) === String(orderId));
  const visible = useMemo(() => [...orders].sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)).filter((order) => filter === 'ALL' || String(order.status).toUpperCase() === filter), [orders, filter]);
  const itemCount = (order) => (order.lines || []).reduce((total, line) => total + Number(line.quantity || 0), 0);

  useEffect(() => {
    refreshOrders();
  }, [refreshOrders]);

  useEffect(() => {
    if (!details || selected) return;
    let alive = true; setDetailLoading(true);
    loadOrder(orderId).finally(() => { if (alive) setDetailLoading(false); });
    return () => { alive = false; };
  }, [details, orderId, selected, loadOrder]);

  async function cancelOrder() {
    const result = await updateStatus(selected.id, 'CANCELLED');
    setNotice(result.ok ? 'Order cancelled.' : result.error);
  }

  return <div className="commerce-page"><Navbar cart={cart.items.map(({ product, quantity }) => ({ product, quantity }))} cartCount={cart.count} onQuantityChange={() => {}} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Link className="commerce-back" to={details ? '/orders' : '/'}><ArrowLeft size={16}/>{details ? 'Back to My Orders' : 'Back to Velnora'}</Link>
    {details ? detailLoading ? <Card className="commerce-empty"><p>Loading order details…</p></Card> : selected ? <><div className="commerce-heading orders-heading"><p className="eyebrow">Order details</p><h1>Order #{selected.id}</h1><p>Placed {dateLabel(selected.placedAt)}</p></div>{(error || notice) && <p className="checkout-message" role={error ? 'alert' : 'status'}>{notice || error}</p>}<div className="order-detail-layout"><section className="order-detail-lines"><div className="order-detail-title"><h2>Items in this order</h2><OrderStatus order={selected}/></div>{selected.lines.map((line, index) => { const product = productMap.get(String(line.productId)); const shop = shopMap.get(String(line.shopId)); const price = Number(line.price ?? product?.price ?? 0); return <Card className="order-detail-item" key={`${line.productId}-${index}`}><img src={product?.image || line.imagePath || ''} alt={line.productName || product?.name || 'Product'}/><div><span className="eyebrow">{line.shopName || shop?.name || selected.shopName}</span><h3>{line.productName || product?.name || 'Product unavailable'}</h3><p>{[line.brand || product?.brand, line.variant || product?.variant, line.unit || product?.unit].filter(Boolean).join(' · ') || 'Product details were not included in the order record.'}</p><small>{line.quantity} × {money(price)}</small></div><b>{money(line.subtotal ?? price * line.quantity)}</b></Card>; })}</section><aside className="order-detail-side">{selected.fulfillmentType === 'PICKUP' ? <Card className="order-detail-card"><p className="eyebrow">Customer pickup</p><h2>{selected.shopName || shopMap.get(String(selected.shopId))?.name}</h2><p>{selected.pickupAddress || selected.shopAddress || shopMap.get(String(selected.shopId))?.address || 'View the shop page for its current address.'}</p><Link to="/" className="text-link"><MapPin size={15}/>Find the shop</Link></Card> : <Card className="order-detail-card"><p className="eyebrow">Delivery address</p><h2>{selected.address?.name || user?.name}</h2><p>{selected.address?.line1 || 'Address not recorded'}<br/>{selected.address?.phone}</p></Card>}<Card className="order-detail-card"><p className="eyebrow">Price details</p><div className="summary-row"><span>Items</span><b>{itemCount(selected)}</b></div>{selected.savings > 0 && <div className="summary-row summary-savings"><span>Savings</span><b>−{money(selected.savings)}</b></div>}<div className="summary-total"><span>Order total</span><b>{money(selected.subtotal)}</b></div></Card>{selected.status === 'PLACED' && <Button variant="secondary" className="cancel-order-button" onClick={cancelOrder}>Cancel this order</Button>}</aside></div></> : <Card className="commerce-empty orders-empty order-not-found"><span><PackageCheck size={24}/></span><h1>Order not found</h1><p>{error || 'That order may not belong to this account or is no longer available.'}</p><Button onClick={() => navigate('/orders')}>View My Orders<ArrowRight size={16}/></Button></Card> : <><div className="commerce-heading orders-heading"><p className="eyebrow">Your purchases</p><h1>My Orders</h1><p>Review your orders, shops, fulfillment, and totals.</p></div>{(error || notice) && <p className="checkout-message" role={error ? 'alert' : 'status'}>{notice || error}</p>}{orders.length > 0 && <div className="order-filters" role="group" aria-label="Filter orders">{filters.map(([label, value]) => <button key={value} type="button" className={filter === value ? 'is-active' : ''} onClick={() => setFilter(value)}>{label}{value === 'ALL' && <span>{orders.length}</span>}</button>)}</div>}{loading ? <Card className="commerce-empty orders-empty"><p>Loading your orders…</p></Card> : visible.length ? <div className="orders-list">{visible.map((order) => <Card className="order-card" key={order.id}><div className="order-card-top"><div><span className="eyebrow">{dateLabel(order.placedAt)}</span><h2>Order #{order.id}</h2></div><OrderStatus order={order}/></div><p className="order-shop-line"><Store size={15}/>{order.shopName || shopMap.get(String(order.shopId))?.name || 'Velnora shop'} · {order.fulfillmentType === 'PICKUP' ? 'Customer pickup' : 'Shop delivery'}</p><div className="order-card-products">{order.lines.slice(0, 3).map((line, index) => <span key={`${line.productId}-${index}`}>{line.quantity} × {line.productName || productMap.get(String(line.productId))?.name || 'Product'}</span>)}{order.lines.length > 3 && <small>+{order.lines.length - 3} more</small>}</div><div className="order-card-bottom"><span>{itemCount(order)} item{itemCount(order) === 1 ? '' : 's'} · <b>{money(order.subtotal)}</b></span><Link to={`/orders/${encodeURIComponent(order.id)}`}>View details<ArrowRight size={15}/></Link></div></Card>)}</div> : <Card className="commerce-empty orders-empty"><span>{orders.length ? <ShoppingBag size={24}/> : <PackageCheck size={24}/>}</span><h2>{orders.length ? 'No matching orders' : 'No orders yet'}</h2><p>{orders.length ? 'Try another status filter to find your order.' : 'Your confirmed orders will appear here.'}</p>{orders.length ? <Button variant="secondary" onClick={() => setFilter('ALL')}>Show all orders</Button> : <Button onClick={() => navigate('/')}>Start Shopping<ArrowRight size={16}/></Button>}</Card>}</>}
    </Container></main><Footer/></div>;
}
