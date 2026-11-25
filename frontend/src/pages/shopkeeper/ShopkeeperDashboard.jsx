import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowRight, BadgePercent, Boxes, Check, ChevronDown, ClipboardList, LayoutDashboard, LogOut, MapPin, PackagePlus, Pencil, Save, Store, Tag, Trash2, Truck, UserRound, X } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Badge, Button, Card, Container, Input } from '../../components/ui.jsx';
import ImageUpload from '../../components/ImageUpload.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { useMarketplaceData } from '../../context/MarketplaceDataContext.jsx';
import { useOrders } from '../../context/OrderContext.jsx';
import { apiErrorMessage } from '../../services/apiClient.js';
import '../../App.css';
import useDataSyncTask from '../../hooks/useDataSyncTask.js';
import { getSyncGeneration, SYNC_INTERVAL } from '../../services/dataSync.js';

const money = (value) => `₹${Number(value || 0).toLocaleString('en-IN')}`;
const tabs = [['overview', 'Overview', LayoutDashboard], ['products', 'Products', PackagePlus], ['inventory', 'Inventory', Boxes], ['orders', 'Orders', ClipboardList], ['shop', 'Shop information', Store]];
const emptyProduct = { name: '', brand: '', variant: '', unit: '', price: '', wasPrice: '', stock: '', imageFile: null };
const statusLabel = { PLACED: 'Order Placed', ACCEPTED: 'Accepted by Shop', PREPARING: 'Being Prepared', READY: 'Ready', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
const nextStatus = { PLACED: 'ACCEPTED', ACCEPTED: 'PREPARING', PREPARING: 'READY', READY: 'COMPLETED' };

export default function ShopkeeperDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { products, shops, categories, getSellerShop, createShopApplication, updateProduct, addProduct, deleteProduct, setProductClearance, removeProductClearance, updateShop, isMock } = useMarketplaceData();
  const { orders, updateStatus, refreshOrders, loading: ordersLoading, error: ordersError } = useOrders();
  const [shop, setShop] = useState(null);
  const [shopLoading, setShopLoading] = useState(true);
  const [shopError, setShopError] = useState('');
  const [application, setApplication] = useState({ name: '', description: '', address: '', categoryId: '', contactPhone: '', contactEmail: '', imageFile: null });
  const [applicationBusy, setApplicationBusy] = useState(false);
  const [tab, setTab] = useState('overview');
  const [profileOpen, setProfileOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(null);
  const [clearanceProductId, setClearanceProductId] = useState('');
  const [clearanceForm, setClearanceForm] = useState({ discountAmount: '', expiresAt: '' });
  const [clearanceError, setClearanceError] = useState('');
  const [form, setForm] = useState(emptyProduct);
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState('');
  const [stockDraft, setStockDraft] = useState({});
  const [shopForm, setShopForm] = useState(null);
  const [busy, setBusy] = useState(false);

  const syncShop = useCallback(async ({ isCurrent = () => true, initial = false } = {}) => {
    const generation = getSyncGeneration('seller-shop');
    const isFresh = () => isCurrent() && generation === getSyncGeneration('seller-shop');
    if (initial) { setShopLoading(true); setShopError(''); }
    try {
      const value = await getSellerShop({ isCurrent: isFresh });
      if (isFresh()) { setShop(value); setShopError(''); }
    } catch (cause) {
      if (isFresh()) {
        if (cause.response?.status === 404) setShop(null);
        else setShopError(apiErrorMessage(cause, 'We could not load your shop. Please try again.'));
      }
    } finally { if (initial && isFresh()) setShopLoading(false); }
  }, [getSellerShop]);
  useEffect(() => { syncShop({ initial: true }); }, [syncShop, user?.id]);
  useDataSyncTask('seller-shop', syncShop, SYNC_INTERVAL, user?.role === 'shopkeeper' && !isMock);

  const ownProducts = useMemo(() => products.filter((product) => String(product.shopId) === String(shop?.id)), [products, shop]);
  const ownOrders = useMemo(() => orders.filter((order) => String(order.shopId) === String(shop?.id)), [orders, shop]);
  const filteredProducts = ownProducts.filter((product) => `${product.name} ${product.brand} ${product.variant}`.toLowerCase().includes(query.trim().toLowerCase()));
  const clearanceProduct = ownProducts.find((product) => String(product.id) === clearanceProductId);
  const lowStock = ownProducts.filter((product) => product.stock <= 5).length;
  const soldUnits = ownOrders.reduce((sum, order) => sum + order.lines.reduce((n, line) => n + line.quantity, 0), 0);

  async function submitApplication(event) {
    event.preventDefault();
    if (!application.name.trim() || !application.address.trim() || !application.categoryId) { setNotice('Enter your shop name and address, then choose a category.'); return; }
    setApplicationBusy(true); setNotice('');
    try {
      const { imageFile, ...shopDetails } = application;
      const created = await createShopApplication({ shop: { ...shopDetails, name: application.name.trim(), address: application.address.trim(), categoryId: Number(application.categoryId) }, image: imageFile });
      setShop(created); setNotice('Your shop application has been sent for review.');
    } catch (cause) { setNotice(apiErrorMessage(cause, 'We could not submit the shop application.')); }
    finally { setApplicationBusy(false); }
  }

  function startEdit(product) { setEditing(String(product.id)); setForm({ ...product, price: String(product.price), wasPrice: product.wasPrice ? String(product.wasPrice) : '', stock: String(product.stock), imageFile: null }); setErrors({}); setNotice(''); }
  function startClearance(product) {
    setClearanceProductId(String(product.id));
    setClearanceForm({ discountAmount: product.clearance ? String(product.clearanceDiscountAmount) : '', expiresAt: product.clearanceExpiresAt ? String(product.clearanceExpiresAt).slice(0, 16) : '' });
    setClearanceError(''); setNotice('');
  }
  async function saveClearance(event) {
    event.preventDefault();
    const discountAmount = Number(clearanceForm.discountAmount);
    const comparisonPrice = clearanceProduct?.clearance
      ? Number(clearanceProduct.price) + Number(clearanceProduct.clearanceDiscountAmount || 0)
      : Number(clearanceProduct?.price || 0);
    if (!clearanceProduct) { setClearanceError('Choose a product for the clearance deal.'); return; }
    if (!Number.isFinite(discountAmount) || discountAmount <= 0 || discountAmount >= comparisonPrice) {
      setClearanceError(`Enter a discount above ₹0 and below the product price of ${money(comparisonPrice)}.`); return;
    }
    setBusy(true); setClearanceError(''); setNotice('');
    try {
      await setProductClearance(clearanceProduct.id, discountAmount, clearanceForm.expiresAt || undefined);
      setNotice(`${clearanceProduct.name} is now on clearance.`); setClearanceProductId('');
    } catch (cause) { setClearanceError(apiErrorMessage(cause, 'We could not save the clearance deal.')); }
    finally { setBusy(false); }
  }
  async function clearClearance(product) {
    setBusy(true); setClearanceError(''); setNotice('');
    try { await removeProductClearance(product.id); setNotice(`Clearance deal removed from ${product.name}.`); setClearanceProductId(''); }
    catch (cause) { setClearanceError(apiErrorMessage(cause, 'We could not remove the clearance deal.')); }
    finally { setBusy(false); }
  }
  async function saveProduct(event) {
    event.preventDefault();
    const next = {};
    if (!form.name.trim()) next.name = 'Enter a product name.';
    if (!form.brand.trim()) next.brand = 'Enter a brand.';
    if (!form.variant.trim()) next.variant = 'Enter a variant.';
    if (!form.unit.trim()) next.unit = 'Enter a pack size or unit.';
    if (!Number.isFinite(Number(form.price)) || Number(form.price) <= 0) next.price = 'Enter a price above zero.';
    if (form.wasPrice && (!Number.isFinite(Number(form.wasPrice)) || Number(form.wasPrice) <= Number(form.price))) next.wasPrice = 'Original price must be higher than the selling price.';
    if (editing === 'new' && (!/^\d+$/.test(String(form.stock)) || Number(form.stock) < 0)) next.stock = 'Enter a whole stock quantity of zero or more.';
    setErrors(next); if (Object.keys(next).length) return;
    const changes = { name: form.name.trim(), brand: form.brand.trim(), variant: form.variant.trim(), unit: form.unit.trim(), price: Number(form.price), wasPrice: form.wasPrice ? Number(form.wasPrice) : null, imageFile: form.imageFile };
    setBusy(true); setNotice('');
    try {
      if (editing === 'new') await addProduct({ shopId: shop.id, ...changes, stock: Number(form.stock || 0) });
      else await updateProduct(editing, changes);
      setNotice(editing === 'new' ? 'Product added to your shop.' : 'Product details saved.'); setEditing(null); setForm(emptyProduct);
    } catch (cause) { setErrors({ form: apiErrorMessage(cause, 'We could not save the product.') }); }
    finally { setBusy(false); }
  }
  async function saveStock(product) {
    const raw = stockDraft[product.id] ?? String(product.stock);
    if (!/^\d+$/.test(String(raw))) { setNotice('Enter a whole stock quantity of zero or more.'); return; }
    setBusy(true); setNotice('');
    try { await updateProduct(product.id, { stock: Number(raw) }); setNotice(`${product.name} inventory updated.`); }
    catch (cause) { setNotice(apiErrorMessage(cause, 'We could not update the stock.')); }
    finally { setBusy(false); }
  }
  async function saveShop(event) {
    event.preventDefault();
    if (!shopForm?.name?.trim() || !shopForm?.address?.trim()) { setNotice('Complete the shop name and address.'); return; }
    setBusy(true); setNotice('');
    try { await updateShop(shop.id, { name: shopForm.name.trim(), description: shopForm.description.trim(), address: shopForm.address.trim(), area: shopForm.address.trim(), imageFile: shopForm.imageFile }); setShop(await getSellerShop()); setShopForm(null); setNotice('Shop information saved. A shop profile update can return the application to admin review.'); }
    catch (cause) { setNotice(apiErrorMessage(cause, 'We could not save shop information.')); }
    finally { setBusy(false); }
  }
  async function moveOrder(order, status) {
    const result = await updateStatus(order.id, status);
    setNotice(result.ok ? `Order #${order.id} updated.` : result.error);
  }
  async function removeProduct(product) {
    if (!window.confirm(`Remove ${product.name} from your shop?`)) return;
    setBusy(true); setNotice('');
    try { await deleteProduct(product.id); setNotice(`${product.name} was removed.`); }
    catch (cause) { setNotice(apiErrorMessage(cause, 'We could not remove this product.')); }
    finally { setBusy(false); }
  }

  if (shopLoading) return <main className="page-wait" role="status">Loading your seller workspace…</main>;
  if (shopError) return <main className="role-landing"><section className="role-card"><p className="ui-eyebrow">Seller workspace</p><h1>Shop unavailable</h1><p role="alert">{shopError}</p><Button onClick={() => window.location.reload()}>Try again</Button></section></main>;
  if (!shop) return <main className="seller-dashboard"><header className="seller-topbar"><Link to="/" className="seller-brand"><img src="/velnora-logo.svg" alt="Velnora"/></Link><div className="seller-account"><Button variant="secondary" onClick={async () => { await logout(); navigate('/'); }}><LogOut size={16}/>Log out</Button></div></header><Container><div className="seller-welcome"><div><p className="eyebrow">Seller application</p><h1>Set up your shop</h1><p>Send your shop details to Velnora for review.</p></div></div>{notice && <p className="seller-notice" role="status">{notice}</p>}<Card className="seller-shop-card"><form className="seller-shop-form" onSubmit={submitApplication}><Input label="Shop name" value={application.name} onChange={(event) => setApplication((old) => ({ ...old, name: event.target.value }))}/><Input label="Shop address" value={application.address} onChange={(event) => setApplication((old) => ({ ...old, address: event.target.value }))}/><label className="seller-textarea-label">Description<textarea value={application.description} onChange={(event) => setApplication((old) => ({ ...old, description: event.target.value }))}/></label><label className="seller-field-label">Category<select value={application.categoryId} onChange={(event) => setApplication((old) => ({ ...old, categoryId: event.target.value }))}><option value="">Choose a category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label><Input label="Shop phone" value={application.contactPhone} onChange={(event) => setApplication((old) => ({ ...old, contactPhone: event.target.value }))}/><Input label="Shop contact email" type="email" value={application.contactEmail} onChange={(event) => setApplication((old) => ({ ...old, contactEmail: event.target.value }))}/><ImageUpload label="Shop cover image" file={application.imageFile} onChange={(imageFile) => setApplication((old) => ({ ...old, imageFile }))}/><Button type="submit" disabled={applicationBusy}>{applicationBusy ? 'Sending application…' : 'Submit shop for review'}<ArrowRight size={16}/></Button></form></Card></Container></main>;

  const approved = shop.status === 'Approved';
  return <main className="seller-dashboard"><header className="seller-topbar"><Link to="/" className="seller-brand"><img src="/velnora-logo.svg" alt="Velnora"/></Link><nav className="seller-tabs" aria-label="Shop management">{tabs.map(([id, label, Icon]) => <button key={id} className={tab === id ? 'is-active' : ''} aria-current={tab === id ? 'page' : undefined} onClick={() => { setTab(id); setNotice(''); if (id === 'shop') setShopForm((current) => current || { ...shop }); }}><Icon size={17}/><span>{label}</span></button>)}</nav><div className="seller-account"><button type="button" className="seller-profile-trigger" aria-haspopup="menu" aria-expanded={profileOpen} onClick={() => setProfileOpen((open) => !open)}><Store size={17}/><span>{shop.name}</span><Badge tone={shop.status === 'Rejected' ? 'neutral' : shop.status === 'Approved' ? 'brass' : 'sapphire'}>{shop.status}</Badge><ChevronDown size={15}/></button>{profileOpen && <div className="seller-profile-menu" role="menu"><div className="seller-profile-heading">Shop profile<b>{shop.name}</b></div><button role="menuitem" type="button" onClick={() => { setProfileOpen(false); setShopForm({ ...shop }); setTab('shop'); }}><Store size={15}/>Shop information</button><Link role="menuitem" to="/account" onClick={() => setProfileOpen(false)}><UserRound size={15}/>Account settings</Link><button role="menuitem" type="button" onClick={async () => { setProfileOpen(false); await logout(); navigate('/'); }}><LogOut size={15}/>Log out</button></div>}</div></header><Container>
    {tab === 'overview' && <div className="seller-welcome"><div><p className="eyebrow">Seller workspace</p><h1>Welcome, {user.name.split(' ')[0]}.</h1><p>Shop activity and fulfilment for {shop.name}.</p></div><Badge tone={approved ? 'brass' : 'sapphire'}>{shop.status}</Badge></div>}
    {notice && <p className="seller-notice" role="status">{notice}</p>}
    {!approved && <Card className="seller-review-note"><p className="eyebrow">Application status</p><h2>{shop.status === 'Rejected' ? 'Changes requested' : 'Your shop is under review'}</h2><p>{shop.status === 'Rejected' ? shop.rejectionReason || 'Please update your shop details and resubmit for review.' : 'Product and order management becomes available after admin approval.'}</p><Button variant="secondary" onClick={() => { setTab('shop'); setShopForm({ ...shop }); }}>Review shop information<Pencil size={15}/></Button></Card>}
    {tab === 'overview' && approved && <><div className="seller-metrics"><Card><span>Products listed</span><b>{ownProducts.length}</b><small>In your shop catalogue</small></Card><Card><span>Open orders</span><b>{ownOrders.filter((order) => !['COMPLETED', 'CANCELLED'].includes(order.status)).length}</b><small>Awaiting fulfilment</small></Card><Card><span>Units ordered</span><b>{soldUnits}</b><small>Across saved orders</small></Card><Card><span>Low stock</span><b>{lowStock}</b><small>Five units or fewer</small></Card></div><Card className="seller-overview-panel"><div><p className="eyebrow">Your shop</p><h2>{shop.name}</h2><p>{shop.description}</p><small><MapPin size={13}/> {shop.address}</small></div><Button variant="secondary" onClick={() => { setShopForm({ ...shop }); setTab('shop'); }}>Edit shop information<Pencil size={15}/></Button></Card><div className="seller-section-heading"><div><p className="eyebrow">Needs attention</p><h2>Inventory watch</h2></div><button onClick={() => setTab('inventory')}>Manage inventory<ArrowRight size={15}/></button></div><div className="seller-product-grid">{ownProducts.filter((product) => product.stock <= 5).map((product) => <ProductRow key={product.id} product={product} onEdit={() => startEdit(product)} onStock={() => { setTab('inventory'); setStockDraft((current) => ({ ...current, [product.id]: String(product.stock) })); }}/>) }{!lowStock && <Card className="seller-empty"><Boxes size={22}/><p>Stock levels look healthy.</p></Card>}</div></>}
    {tab === 'products' && <>{!approved ? <Card className="seller-empty"><Store size={22}/><p>Product management is available after shop approval.</p></Card> : <><div className="seller-section-heading seller-tools"><div><p className="eyebrow">Your catalogue</p><h2>Products</h2></div><Button onClick={() => { setEditing('new'); setForm(emptyProduct); setErrors({}); }}><PackagePlus size={16}/>Add product</Button></div>{editing && <Card className="seller-form-card"><div className="seller-form-heading"><h3>{editing === 'new' ? 'Add a product' : 'Edit product details'}</h3><button type="button" aria-label="Close product form" onClick={() => setEditing(null)}><X size={18}/></button></div><form className="seller-product-form" onSubmit={saveProduct} noValidate><Input label="Product name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} error={errors.name}/><Input label="Brand" value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} error={errors.brand}/><Input label="Variant" value={form.variant} onChange={(e) => setForm({ ...form, variant: e.target.value })} error={errors.variant}/><Input label="Pack size / unit" value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })} error={errors.unit}/><Input label="Selling price (₹)" type="number" min="0.01" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} error={errors.price}/><Input label="Original price (optional)" type="number" min="0.01" step="0.01" value={form.wasPrice} onChange={(e) => setForm({ ...form, wasPrice: e.target.value })} error={errors.wasPrice}/>{editing === 'new' && <Input label="Initial stock" type="number" min="0" step="1" value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} error={errors.stock}/>}<ImageUpload label="Product image" value={form.image} file={form.imageFile} onChange={(imageFile) => setForm((current) => ({ ...current, imageFile }))}/>{errors.form && <p role="alert" className="seller-error">{errors.form}</p>}<p className="seller-form-note">This product belongs to {shop.name}. Inventory changes are managed separately.</p><Button type="submit" disabled={busy}><Save size={16}/>{busy ? 'Saving…' : editing === 'new' ? 'Add product' : 'Save product'}</Button></form></Card>}{clearanceProduct && <Card className="seller-form-card seller-clearance-form"><div className="seller-form-heading"><div><p className="eyebrow">Clearance sale</p><h3>{clearanceProduct.clearance ? 'Update clearance deal' : 'Put product on clearance'}</h3></div><button type="button" aria-label="Close clearance form" onClick={() => setClearanceProductId('')}><X size={18}/></button></div><p className="seller-clearance-product"><Tag size={16}/><span><b>{clearanceProduct.name}</b><small>Current price {money(clearanceProduct.price)}{clearanceProduct.stock > 0 ? ` · ${clearanceProduct.stock} in stock` : ' · Out of stock'}</small></span></p><form className="seller-clearance-fields" onSubmit={saveClearance} noValidate><Input label="Discount amount (₹)" type="number" min="0.01" step="0.01" value={clearanceForm.discountAmount} onChange={(event) => setClearanceForm((current) => ({ ...current, discountAmount: event.target.value }))} error={clearanceError}/><Input label="Deal expiry (optional)" type="datetime-local" min={new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)} value={clearanceForm.expiresAt} onChange={(event) => setClearanceForm((current) => ({ ...current, expiresAt: event.target.value }))}/><p className="seller-form-note">Enter the amount to take off the product price before this deal. Leave expiry blank for a deal with no expiry.</p><div className="seller-clearance-actions">{clearanceProduct.clearance && <Button type="button" variant="secondary" disabled={busy} onClick={() => clearClearance(clearanceProduct)}><X size={15}/>Remove clearance</Button>}<Button type="submit" disabled={busy || clearanceProduct.stock <= 0 || clearanceProduct.available === false}><BadgePercent size={16}/>{busy ? 'Saving…' : clearanceProduct.clearance ? 'Update clearance' : 'Start clearance sale'}</Button></div></form></Card>}<div className="seller-toolbar"><Input aria-label="Search your products" placeholder="Search your products…" value={query} onChange={(e) => setQuery(e.target.value)}/></div><div className="seller-product-grid">{filteredProducts.map((product) => <ProductRow key={product.id} product={product} onEdit={() => startEdit(product)} onStock={() => { setTab('inventory'); setStockDraft((current) => ({ ...current, [product.id]: String(product.stock) })); }} onClearance={() => startClearance(product)} onRemove={() => removeProduct(product)} busy={busy}/>) }{!filteredProducts.length && <Card className="seller-empty">No products match that search.</Card>}</div></>}</>}
    {tab === 'inventory' && <>{!approved ? <Card className="seller-empty"><Boxes size={22}/><p>Inventory is available after shop approval.</p></Card> : <><div className="seller-section-heading"><div><p className="eyebrow">Stock control</p><h2>Inventory</h2></div><span>{lowStock} low-stock item{lowStock === 1 ? '' : 's'}</span></div><div className="seller-inventory-list">{ownProducts.map((product) => <Card className="seller-inventory-row" key={product.id}><img src={product.image} alt={product.name}/><div className="seller-inventory-name"><h3>{product.name}</h3><small>{product.brand} · {product.variant} · {product.unit}</small><Badge tone={product.stock === 0 ? 'neutral' : product.stock <= 5 ? 'brass' : 'sapphire'}>{product.stock === 0 ? 'Out of stock' : product.stock <= 5 ? 'Low stock' : 'In stock'}</Badge></div><div className="seller-stock-control"><label htmlFor={`stock-${product.id}`}>Current stock</label><input id={`stock-${product.id}`} type="number" min="0" step="1" value={stockDraft[product.id] ?? product.stock} onChange={(e) => setStockDraft((current) => ({ ...current, [product.id]: e.target.value }))}/><button type="button" disabled={busy} onClick={() => saveStock(product)}>Update stock</button></div></Card>)}{!ownProducts.length && <Card className="seller-empty">No products to manage yet.</Card>}</div></>}</>}
    {tab === 'orders' && <>{!approved ? <Card className="seller-empty"><ClipboardList size={22}/><p>Shop orders are available after shop approval.</p></Card> : <><div className="seller-section-heading"><div><p className="eyebrow">Fulfilment</p><h2>Orders for {shop.name}</h2></div><span>{ownOrders.length} order{ownOrders.length === 1 ? '' : 's'}</span></div>{(ordersError || notice) && <p className="seller-notice" role={ordersError ? 'alert' : 'status'}>{notice || ordersError}</p>}{ordersLoading ? <Card className="seller-empty">Loading shop orders…</Card> : <div className="seller-orders">{ownOrders.map((order) => { const status = String(order.status || 'PLACED').toUpperCase(); const next = nextStatus[status]; const total = order.lines.reduce((sum, line) => sum + Number(line.subtotal ?? line.quantity * line.price), 0); return <Card className="seller-order-card" key={order.id}><div className="seller-order-top"><div><p className="eyebrow">Order #{order.id}</p><small>{new Date(order.placedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</small></div><Badge tone={status === 'CANCELLED' ? 'neutral' : status === 'COMPLETED' ? 'brass' : 'sapphire'}>{statusLabel[status] || status}</Badge></div><div className="seller-customer"><b>{order.address?.name || order.raw?.customerName || 'Customer'}</b>{order.fulfillmentType === 'PICKUP' ? <span>Customer pickup at {shop.address}</span> : <span><Truck size={14}/> Delivery to {order.address?.line1 || 'address not recorded'}</span>}{order.fulfillmentType !== 'PICKUP' && order.address?.phone && <small>{order.address.phone}</small>}</div><div className="seller-order-lines">{order.lines.map((line, index) => <div key={`${line.productId}-${index}`}><span>{line.quantity} × {line.productName || ownProducts.find((p) => p.id === line.productId)?.name || 'Product'}</span><b>{money(line.subtotal ?? line.quantity * line.price)}</b></div>)}</div><div className={`seller-order-bottom${next ? '' : ' seller-order-bottom--static'}`}><span className="seller-order-total"><small>Shop order total</small><strong>{money(total)}</strong></span>{next && <Button size="sm" onClick={() => moveOrder(order, next)}>{next === 'ACCEPTED' ? 'Accept order' : next === 'PREPARING' ? 'Start preparing' : next === 'READY' ? 'Mark ready' : order.fulfillmentType === 'PICKUP' ? 'Mark collected' : 'Mark delivered'}<ArrowRight size={15}/></Button>}{['PLACED', 'ACCEPTED'].includes(status) && <Button size="sm" variant="secondary" onClick={() => moveOrder(order, 'CANCELLED')}>Cancel order</Button>}</div></Card>})}{!ownOrders.length && <Card className="seller-empty"><ClipboardList size={22}/><p>No orders for your shop yet.</p></Card>}</div>}</>}</>}
    {tab === 'shop' && <><div className="seller-section-heading"><div><p className="eyebrow">Storefront</p><h2>Shop information</h2></div></div><Card className="seller-shop-card"><div className="seller-shop-cover"><Badge tone={shop.status === 'Approved' ? 'brass' : 'sapphire'}>{shop.category} · {shop.status}</Badge><h2>{shop.name}</h2><span>{shop.address}</span></div><form className="seller-shop-form" onSubmit={saveShop}><Input label="Shop name" value={shopForm?.name ?? shop.name} onChange={(e) => setShopForm((current) => ({ ...(current || shop), name: e.target.value }))}/><Input label="Shop address" value={shopForm?.address ?? shop.address} onChange={(e) => setShopForm((current) => ({ ...(current || shop), address: e.target.value }))}/><label className="seller-textarea-label">Description<textarea value={shopForm?.description ?? shop.description ?? ''} onChange={(e) => setShopForm((current) => ({ ...(current || shop), description: e.target.value }))}/></label><ImageUpload label="Shop cover image" value={shopForm?.image ?? shop.image} file={shopForm?.imageFile} onChange={(imageFile) => setShopForm((current) => ({ ...(current || shop), imageFile }))}/><p className="seller-form-note">Changing the shop name, address, or description may return an approved shop to admin review.</p><Button type="submit" disabled={busy}><Save size={16}/>{busy ? 'Saving…' : 'Save shop information'}</Button></form></Card></>}
    </Container></main>;
}

function ProductRow({ product, onEdit, onStock, onClearance, onRemove, busy }) { return <Card className="seller-product-card"><img src={product.image} alt={product.name}/><div className="seller-product-content"><span className="eyebrow">{product.brand}</span><h3>{product.name}</h3><p>{product.variant} · {product.unit}</p><div><b>{money(product.price)}</b>{product.clearance && <Badge tone="brass">Clearance</Badge>}<Badge tone={product.stock <= 5 ? 'brass' : 'sapphire'}>{product.stock <= 5 ? 'Low stock' : `${product.stock} in stock`}</Badge></div><div className="seller-product-actions"><button type="button" onClick={onEdit}><Pencil size={15}/>Edit details</button><button type="button" onClick={onStock}><Boxes size={15}/>Update stock</button>{onClearance && <button type="button" onClick={onClearance}><BadgePercent size={15}/>{product.clearance ? 'Edit clearance' : 'Add clearance'}</button>}{onRemove && <button type="button" disabled={busy} onClick={onRemove}><Trash2 size={15}/>Remove</button>}</div></div></Card>; }
