import { useMemo, useState } from 'react';
import { ArrowLeft, ArrowRight, Check, MapPin, Plus, ShoppingBag, Store, Truck } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Card, Container, Input } from '../components/ui.jsx';
import Navbar from '../components/homepage/Navbar.jsx';
import Footer from '../components/homepage/Footer.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useMarketplaceData } from '../context/MarketplaceDataContext.jsx';

const SELECTED_ADDRESS_KEY = 'velnora-selected-address-v1';
const addressKey = (userId) => `velnora-addresses-v1:${userId}`;
function readAddresses(userId) { try { const value = JSON.parse(localStorage.getItem(addressKey(userId)) || '[]'); return Array.isArray(value) ? value : []; } catch { return []; } }

export default function CheckoutPage() {
  const cart = useCart();
  const { user, logout } = useAuth();
  const { shops } = useMarketplaceData();
  const navigate = useNavigate();
  const addressesKey = addressKey(user?.id || user?.email || 'customer');
  const [addresses, setAddresses] = useState(() => readAddresses(user?.id || user?.email || 'customer'));
  const [selected, setSelected] = useState(() => {
    try { return localStorage.getItem(`${SELECTED_ADDRESS_KEY}:${user?.id || user?.email}`) || addresses[0]?.id || ''; } catch { return addresses[0]?.id || ''; }
  });
  const [fulfillment, setFulfillment] = useState('DELIVERY');
  const [showForm, setShowForm] = useState(!addresses.length);
  const [values, setValues] = useState({ name: user?.name || '', phone: '', line1: '', line2: '', city: '', state: '', pincode: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const set = (key) => (event) => setValues((current) => ({ ...current, [key]: event.target.value }));
  const shopId = cart.items[0]?.product.shopId;
  const shop = useMemo(() => shops.find((entry) => String(entry.id) === String(shopId)), [shops, shopId]);

  function saveAddress(event) {
    event.preventDefault();
    const next = {};
    if (!values.name.trim()) next.name = 'Please enter the recipient’s name.';
    if (!/^[6-9]\d{9}$/.test(values.phone.replace(/\D/g, ''))) next.phone = 'Enter a valid 10 digit mobile number.';
    if (!values.line1.trim()) next.line1 = 'Enter a house number and street.';
    if (!values.city.trim()) next.city = 'Enter a city.';
    if (!values.state.trim()) next.state = 'Enter a state.';
    if (!/^\d{6}$/.test(values.pincode.trim())) next.pincode = 'Enter a 6 digit PIN code.';
    setErrors(next);
    if (Object.keys(next).length) return;
    const address = { ...values, id: crypto.randomUUID() };
    const updated = [...addresses, address];
    setAddresses(updated); setSelected(address.id); setShowForm(false); setMessage('');
    try { localStorage.setItem(addressesKey, JSON.stringify(updated)); localStorage.setItem(`${SELECTED_ADDRESS_KEY}:${user?.id || user?.email}`, address.id); }
    catch { setMessage('The address is available for this checkout, but could not be saved for next time.'); }
  }

  function continueToSummary() {
    if (!cart.items.length) { setMessage('Your bag is empty. Add items before checkout.'); return; }
    if (!shopId || cart.items.some((line) => String(line.product.shopId) !== String(shopId))) { setMessage('Your bag must contain products from one shop. Review your bag and try again.'); return; }
    if (fulfillment === 'DELIVERY') {
      const address = addresses.find((entry) => entry.id === selected);
      if (!address) { setMessage('Choose or add a delivery address to continue.'); return; }
      try { localStorage.setItem(`${SELECTED_ADDRESS_KEY}:${user?.id || user?.email}`, selected); } catch { /* Route state preserves this checkout selection. */ }
      navigate('/order-summary', { state: { addressId: selected, fulfillmentType: fulfillment } });
      return;
    }
    if (!shop?.address && !shop?.area) { setMessage('This shop does not have a pickup address available yet.'); return; }
    navigate('/order-summary', { state: { fulfillmentType: fulfillment } });
  }

  if (!cart.items.length) return <div className="commerce-page"><Navbar cart={[]} cartCount={0} onQuantityChange={() => {}} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Link className="commerce-back" to="/"><ArrowLeft size={16} /> Continue shopping</Link><Card className="commerce-empty"><span><ShoppingBag size={24} /></span><h1>Your bag is empty</h1><p>Add a few products before heading to checkout.</p><Button onClick={() => navigate('/')}>Browse products<ArrowRight size={17} /></Button></Card></Container></main><Footer /></div>;

  return <div className="commerce-page"><Navbar cart={cart.items.map(({ product, quantity }) => ({ product, quantity }))} cartCount={cart.count} onQuantityChange={(id, change) => { const line = cart.items.find((item) => item.product.id === id); if (line) cart.setQuantity(line.key, line.quantity + change); }} onOpenClearance={() => navigate('/clearance-deals')} user={user} onLogout={() => { logout(); navigate('/'); }} /><main className="commerce-main"><Container><Link className="commerce-back" to="/cart"><ArrowLeft size={16} /> Back to your bag</Link><div className="commerce-heading checkout-heading"><p className="eyebrow">Checkout · Step 1 of 2</p><h1>How would you like to receive it?</h1><p>Choose delivery to your address or pickup from {shop?.name || 'your shop'}.</p></div>
    <section className="checkout-address"><div className="fulfillment-options" role="radiogroup" aria-label="Choose a fulfillment method"><button type="button" role="radio" aria-checked={fulfillment === 'DELIVERY'} className={fulfillment === 'DELIVERY' ? 'is-selected' : ''} onClick={() => { setFulfillment('DELIVERY'); setMessage(''); }}><Truck size={20}/><span><b>Shop Delivery</b><small>The shop delivers your order to you.</small></span>{fulfillment === 'DELIVERY' && <Check size={18}/>}</button><button type="button" role="radio" aria-checked={fulfillment === 'PICKUP'} className={fulfillment === 'PICKUP' ? 'is-selected' : ''} onClick={() => { setFulfillment('PICKUP'); setMessage(''); }}><Store size={20}/><span><b>Customer Pickup</b><small>Collect your order from the shop.</small></span>{fulfillment === 'PICKUP' && <Check size={18}/>}</button></div>
    {fulfillment === 'DELIVERY' ? <><div className="checkout-section-head"><span className="checkout-icon"><MapPin size={19}/></span><div><h2>Your delivery address</h2><p>Select a saved address or add a new one.</p></div></div>{addresses.map((address) => <label className={`address-option${selected === address.id ? ' is-selected' : ''}`} key={address.id}><input type="radio" name="delivery-address" checked={selected === address.id} onChange={() => { setSelected(address.id); setMessage(''); }}/><span><b>{address.name}</b><small>{address.line1}{address.line2 ? `, ${address.line2}` : ''}</small><small>{address.city}, {address.state} {address.pincode} · {address.phone}</small></span><Check size={18}/></label>)}<Button variant="secondary" className="add-address-toggle" onClick={() => setShowForm((value) => !value)}><Plus size={17}/>{showForm ? 'Close address form' : 'Add a new address'}</Button>{showForm && <form className="address-form" onSubmit={saveAddress} noValidate><Input label="Full name" value={values.name} onChange={set('name')} error={errors.name} autoComplete="name" placeholder="Name on the delivery"/><Input label="Mobile number" value={values.phone} onChange={set('phone')} error={errors.phone} autoComplete="tel" inputMode="tel" placeholder="10 digit mobile number"/><Input label="House / flat and street" value={values.line1} onChange={set('line1')} error={errors.line1} autoComplete="address-line1" placeholder="House number, street"/><Input label="Landmark or area (optional)" value={values.line2} onChange={set('line2')} autoComplete="address-line2" placeholder="Area or landmark"/><div className="address-grid"><Input label="City" value={values.city} onChange={set('city')} error={errors.city} autoComplete="address-level2"/><Input label="State" value={values.state} onChange={set('state')} error={errors.state} autoComplete="address-level1"/><Input label="PIN code" value={values.pincode} onChange={set('pincode')} error={errors.pincode} autoComplete="postal-code" inputMode="numeric" maxLength={6} placeholder="6 digits"/></div><Button type="submit"><MapPin size={16}/>Save and select address</Button></form>}</> : <div className="pickup-location"><span className="checkout-icon"><MapPin size={19}/></span><div><p className="eyebrow">Pickup location</p><h2>{shop?.name || 'Selected shop'}</h2><p>{shop?.address || shop?.area}</p></div></div>}
    {message && <p className="checkout-message" role="alert">{message}</p>}<div className="address-continue"><Button onClick={continueToSummary}>Continue to order summary<ArrowRight size={17}/></Button></div></section></Container></main><Footer/></div>;
}
