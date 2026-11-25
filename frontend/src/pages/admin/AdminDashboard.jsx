import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowDownRight, ArrowRight, Check, Eye, ImagePlus, Package, Plus, Search, Store, Tags, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Badge, Button, Card, Input } from '../../components/ui.jsx';
import ImageUpload from '../../components/ImageUpload.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { useMarketplaceData } from '../../context/MarketplaceDataContext.jsx';
import { useOrders } from '../../context/OrderContext.jsx';
import AdminLayout from '../../layouts/AdminLayout.jsx';
import '../../App.css';
import { getAdminDashboard, getAdminUsers, setUserSuspended } from '../../services/adminApi.js';
import { apiErrorMessage } from '../../services/apiClient.js';
import useDataSyncTask from '../../hooks/useDataSyncTask.js';
import { getSyncGeneration, requestDataSync, singleFlight, SYNC_INTERVAL } from '../../services/dataSync.js';

const money = (value) => `₹${Number(value || 0).toLocaleString('en-IN')}`;
const statusTone = (status) => ['Approved', 'COMPLETED', 'Delivered'].includes(status) ? 'brass' : ['Rejected', 'CANCELLED', 'Cancelled'].includes(status) ? 'neutral' : 'sapphire';
const orderStatusLabels = { PLACED: 'Order Placed', ACCEPTED: 'Accepted', PREPARING: 'Being Prepared', READY: 'Ready', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
const dateLabel = (value) => {
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? 'Date unavailable' : date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
};
const slug = (value) => value.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

function CategoryImage({ category }) {
  const [failed, setFailed] = useState(!category.image);
  useEffect(() => setFailed(!category.image), [category.image]);
  return failed ? <div className="admin-category-image-fallback"><ImagePlus size={24}/><span>Add a category image</span></div>
    : <img src={category.image} alt="" onError={() => setFailed(true)} />;
}

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { shops, products, categories, updateShop, addCategory, updateCategory, removeCategory, setCategoryActive, isMock } = useMarketplaceData();
  const { orders, loading: ordersLoading, error: ordersError } = useOrders();
  const [section, setSection] = useState('overview');
  const [notice, setNotice] = useState('');
  const [shopQuery, setShopQuery] = useState('');
  const [shopFilter, setShopFilter] = useState('All statuses');
  const [productQuery, setProductQuery] = useState('');
  const [orderQuery, setOrderQuery] = useState('');
  const [orderFilter, setOrderFilter] = useState('All statuses');
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [userQuery, setUserQuery] = useState('');
  const [categoryFormOpen, setCategoryFormOpen] = useState(false);
  const [editingCategoryId, setEditingCategoryId] = useState('');
  const [categoryForm, setCategoryForm] = useState({ name: '', note: '', image: '' });
  const [categoryError, setCategoryError] = useState('');
  const [adminUsers, setAdminUsers] = useState([]);
  const [stats, setStats] = useState(null);
  const [pageError, setPageError] = useState('');
  const [busyUser, setBusyUser] = useState('');
  const [rejectingShop, setRejectingShop] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');

  const refreshAdminData = useCallback((options = {}) => singleFlight(`admin-dashboard:${user?.id ?? 'admin'}:${section}`, async () => {
    const { isCurrent = () => true } = options;
    const generation = getSyncGeneration('admin-dashboard');
    const isFresh = () => isCurrent() && generation === getSyncGeneration('admin-dashboard');
    try {
      const [summary, users] = await Promise.all([
        getAdminDashboard(), section === 'users' ? getAdminUsers() : Promise.resolve(null),
      ]);
      if (!isFresh()) return;
      setStats(summary);
      if (users) setAdminUsers(users.map((item) => ({ id: String(item.id), name: item.fullName, email: item.email, role: String(item.role).toLowerCase(), suspended: item.suspended, phone: item.phone })));
      setPageError('');
    } catch (error) {
      if (isFresh()) setPageError(apiErrorMessage(error, 'We could not load the administration data.'));
    }
  }), [section, user?.id]);
  useEffect(() => { refreshAdminData(); }, [refreshAdminData]);
  useDataSyncTask('admin-dashboard', refreshAdminData, SYNC_INTERVAL, user?.role === 'admin' && !isMock);

  const shopById = useMemo(() => new Map(shops.map((shop) => [shop.id, shop])), [shops]);
  const productById = useMemo(() => new Map(products.map((product) => [product.id, product])), [products]);
  const pendingShops = stats?.pendingShops ?? shops.filter((shop) => shop.status === 'Pending').length;
  const approvedShops = stats?.approvedShops ?? shops.filter((shop) => (shop.status || 'Approved') === 'Approved').length;
  const orderedValue = orders.reduce((sum, order) => sum + (order.status === 'CANCELLED' ? 0 : Number(order.subtotal || 0)), 0);
  const sortedOrders = useMemo(() => [...orders].sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)), [orders]);
  const selectedOrder = orders.find((order) => order.id === selectedOrderId);
  const shopCounts = useMemo(() => products.reduce((counts, product) => ({ ...counts, [product.shopId]: (counts[product.shopId] || 0) + 1 }), {}), [products]);

  const filteredShops = shops.filter((shop) => {
    const query = shopQuery.trim().toLowerCase();
    const matchesText = !query || `${shop.name} ${shop.category} ${shop.area}`.toLowerCase().includes(query);
    return matchesText && (shopFilter === 'All statuses' || (shop.status || 'Approved') === shopFilter);
  });
  const filteredProducts = products.filter((product) => {
    const query = productQuery.trim().toLowerCase();
    const shop = shopById.get(product.shopId);
    return !query || `${product.name} ${product.brand} ${product.variant} ${shop?.name || ''}`.toLowerCase().includes(query);
  });
  const filteredOrders = sortedOrders.filter((order) => {
    const matchesText = !orderQuery.trim() || order.id.toLowerCase().includes(orderQuery.trim().toLowerCase());
    return matchesText && (orderFilter === 'All statuses' || String(order.status || 'PLACED').toUpperCase() === orderFilter);
  });
  const filteredUsers = adminUsers.filter((item) => !userQuery.trim() || `${item.name} ${item.email} ${item.role}`.toLowerCase().includes(userQuery.trim().toLowerCase()));

  async function changeShopStatus(shop, status) {
    try {
      if (status === 'Rejected' && !rejectionReason.trim()) { setRejectingShop(shop.id); setNotice('Enter a reason before rejecting this shop.'); return; }
      await updateShop(shop.id, { status, ...(status === 'Rejected' ? { rejectionReason: rejectionReason.trim() } : {}) });
      setNotice(`${shop.name} is now ${status.toLowerCase()}.`); setRejectingShop(''); setRejectionReason('');
    } catch (error) { setNotice(apiErrorMessage(error, 'We could not update this shop.')); }
  }
  function startNewCategory() {
    setEditingCategoryId('');
    setCategoryForm({ name: '', note: '', image: '', imageFile: null });
    setCategoryError('');
    setCategoryFormOpen(true);
  }
  function startEditCategory(category) {
    setEditingCategoryId(category.id);
    setCategoryForm({ name: category.name, note: category.note || '', image: category.image || '', imageFile: null });
    setCategoryError('');
    setCategoryFormOpen(true);
  }
  async function saveCategory(event) {
    event.preventDefault();
    const name = categoryForm.name.trim();
    if (!name) { setCategoryError('Enter a category name.'); return; }
    if (categories.some((item) => item.id !== editingCategoryId && item.name.trim().toLowerCase() === name.toLowerCase())) {
      setCategoryError('A category with that name already exists.');
      return;
    }
    const changes = { name, note: categoryForm.note.trim(), image: categoryForm.image.trim(), imageFile: categoryForm.imageFile };
    try {
      if (editingCategoryId) { await updateCategory(editingCategoryId, changes); setNotice(`${name} category updated.`); }
      else { await addCategory({ ...(isMock ? { id: `${slug(name) || 'category'}-${Date.now()}` } : {}), ...changes }); setNotice(`${name} category added.`); }
      setCategoryFormOpen(false);
    } catch (error) { setCategoryError(apiErrorMessage(error, 'We could not save the category.')); }
  }
  async function deleteCategory(category) {
    const associated = shops.filter((shop) => shop.categoryId === category.id).length;
    if (associated) { setNotice(`Move ${associated} associated shop${associated === 1 ? '' : 's'} before deleting ${category.name}.`); return; }
    try { if (await removeCategory(category.id)) setNotice(`${category.name} category deactivated.`); }
    catch (error) { setNotice(apiErrorMessage(error, 'We could not update the category.')); }
  }
  async function toggleCategory(category) {
    try { await setCategoryActive(category.id, category.active === false); setNotice(`${category.name} category ${category.active === false ? 'activated' : 'deactivated'}.`); }
    catch (error) { setNotice(apiErrorMessage(error, 'We could not update the category.')); }
  }
  async function toggleSuspension(item) {
    setBusyUser(item.id); setPageError('');
    try { const updated = await setUserSuspended(item.id, !item.suspended); setAdminUsers((current) => current.map((user) => user.id === item.id ? { ...user, suspended: updated.suspended } : user)); requestDataSync('admin-dashboard'); }
    catch (error) { setPageError(apiErrorMessage(error, 'We could not update this account.')); }
    finally { setBusyUser(''); }
  }
  async function toggleShop(shop) {
    try { await updateShop(shop.id, { active: shop.active === false }); setNotice(`${shop.name} visibility updated.`); }
    catch (error) { setNotice(apiErrorMessage(error, 'We could not update this shop.')); }
  }
  async function toggleProduct(product) {
    try { await setProductVisibility(product.id, product.active === false); setNotice(`${product.name} visibility updated.`); }
    catch (error) { setNotice(apiErrorMessage(error, 'We could not update this product.')); }
  }
  function openOrders() { setSection('orders'); setSelectedOrderId(''); }

  return <AdminLayout activeSection={section} onNavigate={(next) => { setSection(next); setNotice(''); }} user={user} onLogout={() => { logout(); navigate('/'); }}>
    <div className="admin-page-heading">
      <div><p className="ui-eyebrow">Velnora operations</p><h1>{section === 'overview' ? 'Admin overview' : section.charAt(0).toUpperCase() + section.slice(1)}</h1><p>Manage marketplace activity and keep local storefronts running smoothly.</p></div>
      <span className="admin-date">Updated {new Date().toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
    </div>
    {notice && <p className="admin-notice" role="status"><Check size={17} />{notice}<button type="button" aria-label="Dismiss message" onClick={() => setNotice('')}><X size={16} /></button></p>}
    {pageError && <p className="admin-notice" role="alert">{pageError}</p>}

    {section === 'overview' && <>
      <div className="admin-stat-grid">
        <StatCard label="Approved shops" value={approvedShops} detail={`${stats?.shops ?? shops.length} shops in directory`} icon={Store} />
        <StatCard label="Pending review" value={pendingShops} detail="Shop applications awaiting review" icon={Eye} emphasis={pendingShops > 0} />
        <StatCard label="Products listed" value={stats?.products ?? products.length} detail="Across all local shops" icon={Package} />
        <StatCard label="Orders placed" value={stats?.orders ?? orders.length} detail={`${money(orderedValue)} active order value`} icon={ArrowDownRight} />
      </div>
      <section className="admin-overview-grid">
        <Card className="admin-overview-card"><div className="admin-section-title"><div><p className="ui-eyebrow">Marketplace</p><h2>Shop approvals</h2></div><Badge tone={pendingShops ? 'brass' : 'neutral'}>{pendingShops} pending</Badge></div>
          {shops.length ? <div className="admin-mini-list">{shops.slice(0, 4).map((shop) => <div className="admin-mini-row" key={shop.id}><div><b>{shop.name}</b><span>{shop.category} · {shop.area}</span></div><Badge tone={statusTone(shop.status || 'Approved')}>{shop.status || 'Approved'}</Badge></div>)}</div> : <EmptyState title="No shops yet" detail="Shop applications will appear here." />}
          <Button variant="secondary" size="sm" onClick={() => setSection('shops')}>Review shops<ArrowRight size={15} /></Button>
        </Card>
        <Card className="admin-overview-card"><div className="admin-section-title"><div><p className="ui-eyebrow">Latest activity</p><h2>Recent orders</h2></div><button type="button" className="admin-text-action" onClick={openOrders}>View all<ArrowRight size={15} /></button></div>
          {sortedOrders.length ? <div className="admin-mini-list">{sortedOrders.slice(0, 4).map((order) => <button type="button" className="admin-mini-row admin-mini-order" key={order.id} onClick={() => { setSection('orders'); setSelectedOrderId(order.id); }}><div><b>Order #{order.id}</b><span>{dateLabel(order.placedAt)}</span></div><span className="admin-order-summary"><Badge tone={statusTone(order.status)}>{orderStatusLabels[order.status] || order.status}</Badge><b>{money(order.subtotal)}</b></span></button>)}</div> : <EmptyState title="No orders yet" detail="Orders placed through checkout will appear here." />}
        </Card>
      </section>
    </>}

    {section === 'shops' && <section className="admin-panel">
      <div className="admin-toolbar"><Input aria-label="Search shops" placeholder="Search shop, category or area" value={shopQuery} onChange={(event) => setShopQuery(event.target.value)} /><label className="admin-filter"><span>Status</span><select value={shopFilter} onChange={(event) => setShopFilter(event.target.value)}><option>All statuses</option><option>Approved</option><option>Pending</option><option>Rejected</option></select></label><span className="admin-result-count">{filteredShops.length} shops</span></div>
      {filteredShops.length ? <div className="admin-shop-grid">{filteredShops.map((shop) => <Card className="admin-shop-card" key={shop.id}><div className="admin-shop-top"><div className="admin-shop-avatar"><Store size={21} /></div><Badge tone={statusTone(shop.status || 'Approved')}>{shop.status || 'Approved'}{shop.active === false ? ' · Suspended' : ''}</Badge></div><h2>{shop.name}</h2><p>{shop.category} · {shop.address || shop.area}</p><div className="admin-shop-facts"><span>{shopCounts[shop.id] || 0} products</span><span>{shop.ownerName || shop.ownerEmail || 'Owner details unavailable'}</span></div><div className="admin-card-actions">{shop.status === 'Pending' && <Button size="sm" onClick={() => changeShopStatus(shop, 'Approved')}><Check size={15} />Approve</Button>}{shop.status === 'Pending' && <Button size="sm" variant="secondary" onClick={() => { setRejectingShop(shop.id); setRejectionReason(''); }}><X size={15} />Reject</Button>}{shop.status !== 'Pending' && <Button size="sm" variant="secondary" onClick={() => toggleShop(shop)}>{shop.active === false ? 'Restore shop' : 'Suspend shop'}</Button>}</div>{rejectingShop === shop.id && <div className="admin-rejection-form"><label>Reason for rejection<textarea value={rejectionReason} maxLength={500} onChange={(event) => setRejectionReason(event.target.value)}/></label><Button size="sm" onClick={() => changeShopStatus(shop, 'Rejected')}>Confirm rejection</Button></div>}</Card>)}</div> : <EmptyState title="No shops match" detail="Try changing your search or status filter." />}
    </section>}

    {section === 'categories' && <section className="admin-panel">
      <div className="admin-section-title admin-category-title"><div><p className="ui-eyebrow">Marketplace structure</p><h2>Categories</h2><p>Shops stay assigned to one category. Categories with shops cannot be deleted.</p></div><Button onClick={startNewCategory}><Plus size={17} />Add category</Button></div>
      {categoryFormOpen && <Card className="admin-category-form-card"><div className="admin-form-heading"><div><h3>{editingCategoryId ? 'Edit category' : 'Create a category'}</h3><p>Changes appear in the marketplace and shop listings.</p></div><button type="button" aria-label="Close category form" onClick={() => setCategoryFormOpen(false)}><X size={18}/></button></div><form className="admin-category-form" onSubmit={saveCategory} noValidate><Input label="Category name" value={categoryForm.name} onChange={(event) => setCategoryForm((current) => ({ ...current, name: event.target.value }))} error={categoryError}/>{isMock ? <><Input label="Short description" value={categoryForm.note} onChange={(event) => setCategoryForm((current) => ({ ...current, note: event.target.value }))}/><Input label="Image URL (optional)" value={categoryForm.image} onChange={(event) => setCategoryForm((current) => ({ ...current, image: event.target.value }))} placeholder="https://…"/></> : <div className="admin-category-image-field"><ImageUpload label="Category image" value={categoryForm.image} file={categoryForm.imageFile} onChange={(imageFile) => setCategoryForm((current) => ({ ...current, imageFile }))}/></div>}{categoryError && <p role="alert">{categoryError}</p>}<Button type="submit"><Check size={16}/>{editingCategoryId ? 'Save category' : 'Create category'}</Button></form></Card>}
      {categories.length ? <div className="admin-category-grid">{categories.map((category) => { const assigned = shops.filter((shop) => shop.categoryId === category.id); return <Card className="admin-category-card" key={category.id}><CategoryImage category={category}/><div className="admin-category-body"><div><h3>{category.name}</h3><p>{category.note}</p></div><span>{assigned.length} shop{assigned.length === 1 ? '' : 's'} · {category.active === false ? 'Inactive' : 'Active'}</span><div className="admin-card-actions"><Button size="sm" variant="secondary" onClick={() => startEditCategory(category)}>Edit</Button><Button size="sm" variant="secondary" onClick={() => toggleCategory(category)}>{category.active === false ? 'Activate' : 'Deactivate'}</Button><Button size="sm" variant="quiet" disabled={assigned.length > 0} title={assigned.length ? 'This category is assigned to a shop' : 'Delete category'} onClick={() => deleteCategory(category)}>Delete</Button></div>{assigned.length > 0 && <small className="admin-association-note">Categories with shops stay in the catalogue.</small>}</div></Card>; })}</div> : <EmptyState title="No categories yet" detail="Create a category to organize shop listings." />}
    </section>}

    {section === 'products' && <section className="admin-panel"><div className="admin-toolbar"><Input aria-label="Search all products" placeholder="Search product, brand or shop" value={productQuery} onChange={(event) => setProductQuery(event.target.value)} /><span className="admin-result-count">{filteredProducts.length} products</span></div>
      {filteredProducts.length ? <div className="admin-product-list">{filteredProducts.map((product) => <Card className="admin-product-row" key={product.id}><img src={product.image} alt={product.name}/><div className="admin-product-copy"><span className="ui-eyebrow">{product.brand}</span><h3>{product.name}</h3><p>{product.variant} · {product.unit}</p></div><div className="admin-product-shop"><small>Shop</small><b>{shopById.get(product.shopId)?.name || 'Shop unavailable'}</b></div><div className="admin-product-price"><small>Price</small><b>{money(product.price)}</b></div><Badge tone={product.active === false ? 'neutral' : product.stock > 0 ? 'sapphire' : 'neutral'}>{product.active === false ? 'Inactive' : product.stock > 0 ? `${product.stock} in stock` : 'Out of stock'}</Badge><Button size="sm" variant="secondary" onClick={() => toggleProduct(product)}>{product.active === false ? 'Restore' : 'Deactivate'}</Button></Card>)}</div> : <EmptyState title="No products match" detail="Try another product, brand or shop name."/>}
    </section>}

    {section === 'orders' && <section className="admin-panel">
      {selectedOrder && <OrderDetail order={selectedOrder} productsById={productById} shopsById={shopById} onClose={() => setSelectedOrderId('')} />}
      <div className="admin-toolbar"><Input aria-label="Search order number" placeholder="Search by order number" value={orderQuery} onChange={(event) => setOrderQuery(event.target.value)}/><label className="admin-filter"><span>Status</span><select value={orderFilter} onChange={(event) => setOrderFilter(event.target.value)}><option>All statuses</option>{Object.entries(orderStatusLabels).map(([status, label]) => <option key={status} value={status}>{label}</option>)}</select></label><span className="admin-result-count">{filteredOrders.length} orders</span></div>
      {ordersError && <p className="admin-notice" role="alert">{ordersError}</p>}{ordersLoading ? <div className="admin-empty">Loading orders…</div> : filteredOrders.length ? <div className="admin-order-list">{filteredOrders.map((order) => <Card className="admin-order-card" key={order.id}><div className="admin-order-card-main"><div><p className="ui-eyebrow">{dateLabel(order.placedAt)}</p><h3>Order #{order.id}</h3><span>{order.shopName} · {(order.lines || []).reduce((sum, line) => sum + Number(line.quantity || 0), 0)} items</span></div><div className="admin-order-value"><Badge tone={statusTone(order.status)}>{orderStatusLabels[order.status] || order.status}</Badge><b>{money(order.subtotal)}</b></div></div><button type="button" className="admin-order-detail-link" onClick={() => setSelectedOrderId(order.id)}>View order details<ArrowRight size={15}/></button></Card>)}</div> : <EmptyState title={orders.length ? 'No matching orders' : 'No orders yet'} detail={orders.length ? 'Try another order number or status.' : 'Orders placed through checkout will appear here.'}/>}
    </section>}

    {section === 'users' && <section className="admin-panel"><div className="admin-toolbar"><Input aria-label="Search users" placeholder="Search by name, email or role" value={userQuery} onChange={(event) => setUserQuery(event.target.value)}/><span className="admin-result-count">{filteredUsers.length} users</span></div>
      {filteredUsers.length ? <div className="admin-user-grid">{filteredUsers.map((item) => <Card className="admin-user-card" key={`${item.email}-${item.role}`}><span className="admin-user-avatar admin-user-avatar--light">{item.name?.charAt(0) || 'V'}</span><div><h3>{item.name}</h3><p>{item.email}</p><small>{item.phone || 'Phone not provided'}</small></div><Badge tone={item.role === 'admin' ? 'brass' : item.role === 'shopkeeper' ? 'sapphire' : 'neutral'}>{item.role}</Badge><Badge tone={item.suspended ? 'neutral' : 'brass'}>{item.suspended ? 'Suspended' : 'Active'}</Badge>{item.role !== 'admin' && <Button size="sm" variant="secondary" disabled={busyUser === item.id} onClick={() => toggleSuspension(item)}>{item.suspended ? 'Restore account' : 'Suspend account'}</Button>}</Card>)}</div> : <EmptyState title="No users match" detail="Try a different name, email or role."/>}
    </section>}
  </AdminLayout>;
}

function StatCard({ label, value, detail, icon: Icon, emphasis = false }) {
  return <Card className={`admin-stat-card${emphasis ? ' is-emphasis' : ''}`}><span className="admin-stat-icon"><Icon size={19} /></span><span className="admin-stat-label">{label}</span><strong>{value}</strong><small>{detail}</small></Card>;
}

function EmptyState({ title, detail }) {
  return <div className="admin-empty"><span><Search size={21} /></span><h3>{title}</h3><p>{detail}</p></div>;
}

function OrderDetail({ order, productsById, shopsById, onClose }) {
  const status = String(order.status || 'PLACED').toUpperCase();
  const lineCount = (order.lines || []).reduce((sum, line) => sum + Number(line.quantity || 0), 0);
  const paymentStatus = order.raw?.paymentStatus || 'Not supported by the current backend';
  return <Card className="admin-order-detail"><div className="admin-order-detail-heading"><div><p className="ui-eyebrow">Order details</p><h2>Order #{order.id}</h2><span>Placed {dateLabel(order.placedAt)}</span></div><div><Badge tone={statusTone(status)}>{orderStatusLabels[status] || status}</Badge><button type="button" aria-label="Close order details" onClick={onClose}><X size={18}/></button></div></div>
    <div className="admin-order-detail-meta"><div><small>Items</small><b>{lineCount}</b></div><div><small>Fulfillment</small><b>{order.fulfillmentType === 'PICKUP' ? 'Customer pickup' : 'Shop delivery'}</b></div><div><small>Payment</small><b>{paymentStatus}</b></div><div><small>Order total</small><b>{money(order.subtotal)}</b></div></div>
    <div className="admin-order-lines"><h3>Shops and products</h3>{(order.lines || []).map((line, index) => { const product = productsById.get(line.productId); const shop = shopsById.get(line.shopId); const unitPrice = Number(line.price ?? product?.price ?? 0); return <div className="admin-order-line" key={`${line.productId}-${index}`}><div><b>{line.productName || product?.name || 'Product unavailable'}</b><span>{line.shopName || shop?.name || 'Shop unavailable'} · {line.brand || product?.brand || ''} {[line.variant || product?.variant, line.unit || product?.unit].filter(Boolean).join(' · ')}</span><small>{line.quantity} × {money(unitPrice)}</small></div><strong>{money(unitPrice * Number(line.quantity || 0))}</strong></div>; })}{!order.lines?.length && <p className="admin-association-note">No item details are saved on this order.</p>}</div>
    <p className="admin-readonly-note">Order status follows the existing customer and shopkeeper fulfilment flow and is read-only here.</p>
  </Card>;
}
