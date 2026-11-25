import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { apiConfigured, apiErrorMessage } from '../services/apiClient.js';
import apiClient from '../services/apiClient.js';
import { getMyOrders, getMyShopOrders, getOrder as getOrderApi, updateOrderStatus as updateOrderStatusApi, content } from '../services/marketplaceApi.js';
import { useAuth } from './AuthContext.jsx';
import { getSyncGeneration, requestDataSync, singleFlight, SYNC_INTERVAL } from '../services/dataSync.js';
import useDataSyncTask from '../hooks/useDataSyncTask.js';

const ORDERS_KEY = 'velnora-orders-v1';
const LEGACY_KEY = 'velnora-last-order-v1';
const OrderContext = createContext(null);
const MOCK_MODE = import.meta.env.VITE_USE_MOCK_DATA === 'true' && !apiConfigured;
const statusLabels = { PLACED: 'Order Placed', ACCEPTED: 'Accepted by Shop', PREPARING: 'Being Prepared', READY: 'Ready', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };

export function mapOrder(order) {
  const status = String(order.status || 'PLACED').toUpperCase();
  return {
    id: String(order.id), placedAt: order.createdAt, shopId: String(order.shopId), shopName: order.shopName,
    shopAddress: order.shopAddress || '', fulfillmentType: order.fulfillmentType,
    address: order.fulfillmentType === 'DELIVERY' ? { name: order.customerName || '', line1: order.deliveryAddress || '', phone: order.customerPhone || '' } : null,
    pickupAddress: order.fulfillmentType === 'PICKUP' ? order.shopAddress || '' : '',
    lines: (order.items || []).map((line) => ({
      productId: String(line.productId), productName: line.productName, shopId: String(order.shopId), shopName: order.shopName,
      quantity: Number(line.quantity), price: Number(line.priceAtPurchase), subtotal: Number(line.subtotal),
    })),
    subtotal: Number(order.totalAmount || 0), savings: 0, status, statusLabel: statusLabels[status] || status, raw: order,
  };
}
function loadMockOrders() {
  try {
    const saved = JSON.parse(localStorage.getItem(ORDERS_KEY) || 'null');
    const list = Array.isArray(saved) ? saved : [];
    const legacy = JSON.parse(localStorage.getItem(LEGACY_KEY) || 'null');
    const migrated = legacy?.id && !list.some((item) => item.id === legacy.id) ? [legacy, ...list] : list;
    if (!Array.isArray(saved) || migrated.length !== list.length) localStorage.setItem(ORDERS_KEY, JSON.stringify(migrated));
    return migrated;
  } catch { return []; }
}
const transitions = { PLACED: 'ACCEPTED', ACCEPTED: 'PREPARING', PREPARING: 'READY', READY: 'COMPLETED' };
async function readEveryPage(fetchPage) {
  const first = await fetchPage(0);
  const rows = [...content(first)];
  const pageCount = Math.max(1, Number(first?.totalPages || 1));
  for (let page = 1; page < pageCount; page += 1) rows.push(...content(await fetchPage(page)));
  return rows;
}

export function OrderProvider({ children }) {
  const { user, loading: authLoading } = useAuth();
  const [orders, setOrders] = useState(() => MOCK_MODE ? loadMockOrders() : []);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const hasLoaded = useRef(false);

  const refreshOrders = useCallback((options = {}) => singleFlight(`orders:${user?.id ?? 'anonymous'}:${user?.role ?? 'guest'}`, async () => {
    const { isCurrent = () => true } = options;
    const generation = getSyncGeneration('orders');
    const isFresh = () => isCurrent() && generation === getSyncGeneration('orders');
    if (MOCK_MODE) { setOrders(loadMockOrders()); setError(''); return; }
    if (!apiConfigured || authLoading || !user) { setOrders([]); return; }
    if (!hasLoaded.current) setLoading(true);
    try {
      let response;
      if (user.role === 'customer') response = await readEveryPage((page) => getMyOrders({ page, size: 100 }));
      else if (user.role === 'shopkeeper') response = await readEveryPage((page) => getMyShopOrders({ page, size: 100 }));
      else if (user.role === 'admin') response = await readEveryPage((page) => apiClient.get('/admin/orders', { params: { page, size: 100 } }).then((r) => r.data));
      else { setOrders([]); return; }
      if (!isFresh()) return;
      setOrders((Array.isArray(response) ? response : content(response)).map(mapOrder));
      hasLoaded.current = true;
      setError('');
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('velnora:orders-refreshed'));
    } catch (cause) { if (isFresh()) setError(apiErrorMessage(cause, 'We could not load orders. Please try again.')); }
    finally { if (isFresh()) setLoading(false); }
  }), [authLoading, user]);

  useEffect(() => { refreshOrders(); }, [refreshOrders]);
  useDataSyncTask('orders', refreshOrders, SYNC_INTERVAL, apiConfigured && !MOCK_MODE && !authLoading && Boolean(user));
  useEffect(() => {
    if (!MOCK_MODE) return;
    try { localStorage.setItem(ORDERS_KEY, JSON.stringify(orders)); } catch { /* Demo order data stays available for this session. */ }
  }, [orders]);

  const addOrder = useCallback((order) => {
    if (!MOCK_MODE) throw new Error('Orders can only be created by the backend checkout service.');
    setOrders((current) => current.some((item) => item.id === order.id) ? current : [order, ...current]);
    return order;
  }, []);

  const loadOrder = useCallback(async (id) => {
    const existing = orders.find((item) => String(item.id) === String(id));
    if (existing || MOCK_MODE) return existing || null;
    if (!apiConfigured || !user) return null;
    try {
      let response;
      if (user.role === 'customer') response = await getOrderApi(id);
      else if (user.role === 'shopkeeper') response = await apiClient.get(`/orders/my-shop-orders/${id}`).then((r) => r.data);
      else response = await apiClient.get(`/admin/orders/${id}`).then((r) => r.data);
      const order = mapOrder(response);
      setOrders((current) => [order, ...current.filter((item) => item.id !== order.id)]);
      return order;
    } catch (cause) { setError(apiErrorMessage(cause, 'Order details could not be loaded.')); return null; }
  }, [orders, user]);

  const updateStatus = useCallback(async (id, status) => {
    const current = orders.find((item) => String(item.id) === String(id));
    if (!current) return { ok: false, error: 'Order not found.' };
    const currentStatus = String(current.status || 'PLACED').toUpperCase();
    const nextStatus = String(status).toUpperCase();
    const canCancel = nextStatus === 'CANCELLED' && (user?.role === 'admin' || user?.role === 'customer' && currentStatus === 'PLACED' || user?.role === 'shopkeeper' && ['PLACED', 'ACCEPTED'].includes(currentStatus));
    if (nextStatus !== transitions[currentStatus] && !canCancel) return { ok: false, error: 'That order update is not allowed at this stage.' };
    if (MOCK_MODE) {
      setOrders((items) => items.map((item) => String(item.id) === String(id) ? { ...item, status: nextStatus, statusLabel: statusLabels[nextStatus] || nextStatus } : item));
      return { ok: true };
    }
    try {
      const updated = mapOrder(await updateOrderStatusApi(id, nextStatus));
      setOrders((items) => items.map((item) => item.id === updated.id ? updated : item));
      requestDataSync('orders', 'admin-dashboard', 'seller-shop');
      return { ok: true, order: updated };
    } catch (cause) { const message = apiErrorMessage(cause, 'We could not update this order.'); setError(message); return { ok: false, error: message }; }
  }, [orders, user]);

  const value = useMemo(() => ({ orders, loading, error, addOrder, updateStatus, refreshOrders, loadOrder, statusLabels }), [orders, loading, error, addOrder, updateStatus, refreshOrders, loadOrder]);
  return <OrderContext.Provider value={value}>{children}</OrderContext.Provider>;
}

export function useOrders() {
  const context = useContext(OrderContext);
  if (!context) throw new Error('useOrders must be used within OrderProvider');
  return context;
}
export { ORDERS_KEY };
