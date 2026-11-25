import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { products as baseProducts } from '../data/marketplace.js';
import { useMarketplaceData } from './MarketplaceDataContext.jsx';
import { useAuth } from './AuthContext.jsx';
import { apiConfigured, apiErrorMessage } from '../services/apiClient.js';
import { addCartItem, changeCartItem, checkoutCart, clearCart as clearRemoteCart, getCart, removeCartItem } from '../services/cartApi.js';
import { getSyncGeneration, requestDataSync, singleFlight, SYNC_INTERVAL } from '../services/dataSync.js';
import useDataSyncTask from '../hooks/useDataSyncTask.js';

const CartContext = createContext(null);
const STORAGE_KEY = 'velnora-cart-v1';
const MOCK_MODE = import.meta.env.VITE_USE_MOCK_DATA === 'true' && !apiConfigured;

function readCart(products = baseProducts) {
  if (!MOCK_MODE) return [];
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    if (!Array.isArray(saved)) return [];
    const merged = new Map();
    saved.forEach(({ productId, quantity }) => {
      const product = products.find((item) => String(item.id) === String(productId));
      if (!product || product.stock < 1 || !Number.isFinite(quantity)) return;
      const key = `${product.id}:${product.variant}:${product.unit}`;
      merged.set(key, Math.min(product.stock, (merged.get(key) || 0) + Math.max(0, Math.floor(quantity))));
    });
    return [...merged.entries()].filter(([, quantity]) => quantity > 0).map(([key, quantity]) => ({ key, productId: key.slice(0, key.indexOf(':')), quantity }));
  } catch { return []; }
}

function normalizeOrder(order) {
  return {
    id: String(order.id), placedAt: order.createdAt, shopId: String(order.shopId), shopName: order.shopName,
    shopAddress: order.shopAddress || '',
    fulfillmentType: order.fulfillmentType,
    address: order.fulfillmentType === 'DELIVERY' ? { name: order.customerName, line1: order.deliveryAddress || '', phone: order.customerPhone || '' } : null,
    pickupAddress: order.fulfillmentType === 'PICKUP' ? order.shopAddress || '' : '', lines: (order.items || []).map((line) => ({
      productId: String(line.productId), productName: line.productName, shopId: String(order.shopId), shopName: order.shopName,
      quantity: line.quantity, price: Number(line.priceAtPurchase), subtotal: Number(line.subtotal),
    })),
    subtotal: Number(order.totalAmount || 0), savings: 0, status: order.status, raw: order,
  };
}

export function CartProvider({ children }) {
  const { products, shops } = useMarketplaceData();
  const { user, loading: authLoading } = useAuth();
  const [lines, setLines] = useState(() => readCart(products));
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [shopSwitch, setShopSwitch] = useState(null);
  const [needsOrderCheck, setNeedsOrderCheck] = useState(false);
  const orderAttempt = useRef(false);
  const hasLoaded = useRef(false);

  const productById = useMemo(() => new Map(products.map((product) => [String(product.id), product])), [products]);
  const shopById = useMemo(() => new Map(shops.map((shop) => [String(shop.id), shop])), [shops]);

  const applyServerLines = useCallback((records) => {
    const rows = Array.isArray(records) ? records : records?.items || [];
    setLines(rows.map((row) => {
      const productId = String(row.productId);
      const product = productById.get(productId) || {
        id: productId, shopId: String(row.shopId), name: row.productName, brand: row.brand || '', variant: row.variant || '',
        unit: '', image: row.imagePath || '', price: Number(row.unitPrice || 0), stock: Number(row.stockQuantity || 0), wasPrice: null,
      };
      const key = `${product.id}:${product.variant || ''}:${product.unit || ''}`;
      return { key, cartItemId: row.id, productId, quantity: Number(row.quantity), product: { ...product, shopId: String(row.shopId ?? product.shopId), stock: Number(row.stockQuantity ?? product.stock) } };
    }));
  }, [productById]);

  const refresh = useCallback((options = {}) => singleFlight(`cart:${user?.id ?? 'anonymous'}`, async () => {
    const { isCurrent = () => true } = options;
    const generation = getSyncGeneration('cart');
    const isFresh = () => isCurrent() && generation === getSyncGeneration('cart');
    if (!apiConfigured || MOCK_MODE || authLoading || user?.role !== 'customer') return;
    if (!hasLoaded.current) setLoading(true);
    try {
      const records = await getCart();
      if (!isFresh()) return;
      applyServerLines(records); hasLoaded.current = true; setError('');
    } catch (cause) {
      if (isFresh()) setError(apiErrorMessage(cause, 'We could not load your bag. Please try again.'));
    } finally { if (isFresh()) setLoading(false); }
  }), [applyServerLines, authLoading, user]);

  useEffect(() => {
    if (MOCK_MODE) return;
    if (user?.role !== 'customer') { setLines([]); return; }
    refresh();
  }, [refresh, user?.id, user?.role]);
  useDataSyncTask('cart', refresh, SYNC_INTERVAL, apiConfigured && !MOCK_MODE && !authLoading && user?.role === 'customer');
  useEffect(() => {
    const verifyOrderState = () => setNeedsOrderCheck(false);
    window.addEventListener('velnora:orders-refreshed', verifyOrderState);
    return () => window.removeEventListener('velnora:orders-refreshed', verifyOrderState);
  }, []);
  useEffect(() => {
    if (!MOCK_MODE) return;
    setLines((current) => current.map((line) => {
      const product = productById.get(String(line.productId));
      return product ? { ...line, quantity: Math.min(line.quantity, product.stock) } : line;
    }).filter((line) => line.quantity > 0));
  }, [productById]);
  useEffect(() => {
    if (!MOCK_MODE) return;
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(lines.map(({ productId, quantity }) => ({ productId, quantity })))); } catch { /* Storage may be disabled. */ }
  }, [lines]);

  const commitAdd = useCallback(async (product, replace = false) => {
    if (!product || product.stock < 1) { setError('This product is currently out of stock.'); return false; }
    const existing = lines.find((line) => String(line.productId) === String(product.id));
    const quantity = existing ? existing.quantity + 1 : 1;
    if (quantity > product.stock) { setError(`Only ${product.stock} ${product.unit || 'items'} are available.`); return false; }
    setBusy(true); setError('');
    try {
      const response = await addCartItem(product.id, quantity, replace);
      applyServerLines(response);
      requestDataSync('cart');
      return true;
    } catch (cause) {
      const message = apiErrorMessage(cause, 'We could not add this item to your bag.');
      if (message.includes('CART_SHOP_MISMATCH')) { setShopSwitch({ product, currentShopId: lines[0]?.product?.shopId }); return false; }
      setError(message); return false;
    } finally { setBusy(false); }
  }, [applyServerLines, lines]);

  const add = useCallback(async (product) => {
    setError('');
    if (!MOCK_MODE && !apiConfigured) { setError('The cart service is not configured. Please contact the site administrator.'); return false; }
    if ((!user || user.role !== 'customer') && !MOCK_MODE) { setError('Sign in with a customer account to add products to your bag.'); return false; }
    const currentShopId = lines[0]?.product?.shopId ?? productById.get(String(lines[0]?.productId))?.shopId;
    if (lines.length && String(currentShopId) !== String(product.shopId)) {
      setShopSwitch({ product, currentShopId }); return false;
    }
    if (MOCK_MODE) {
      let added = false;
      setLines((current) => {
        const key = `${product.id}:${product.variant}:${product.unit}`;
        const found = current.find((line) => line.key === key);
        if (found?.quantity >= product.stock) { setError(`Only ${product.stock} ${product.unit || 'items'} are available.`); return current; }
        added = true;
        return found ? current.map((line) => line.key === key ? { ...line, quantity: line.quantity + 1 } : line) : [...current, { key, productId: product.id, quantity: 1 }];
      });
      return added;
    }
    return commitAdd(product, false);
  }, [commitAdd, lines, productById, user]);

  const confirmShopSwitch = useCallback(async () => {
    if (!shopSwitch) return false;
    const { product } = shopSwitch; setShopSwitch(null);
    if (MOCK_MODE) { setLines([]); return add(product); }
    return commitAdd(product, true);
  }, [add, commitAdd, shopSwitch]);
  const cancelShopSwitch = useCallback(() => setShopSwitch(null), []);
  const clearError = useCallback(() => setError(''), []);

  const setQuantity = useCallback(async (key, requested) => {
    const line = lines.find((item) => item.key === key);
    if (!line) return;
    const product = productById.get(String(line.productId)) || line.product;
    const quantity = Math.max(0, Math.min(Number(product?.stock ?? line.product?.stock ?? 0), Math.floor(Number(requested) || 0)));
    setError('');
    if (MOCK_MODE) { setLines((current) => quantity === 0 ? current.filter((item) => item.key !== key) : current.map((item) => item.key === key ? { ...item, quantity } : item)); return; }
    if (!apiConfigured || !user || !line.cartItemId) { setError('Sign in to update your bag.'); return; }
    setBusy(true);
    try {
      applyServerLines(quantity === 0 ? (await removeCartItem(line.cartItemId), await getCart()) : await changeCartItem(line.cartItemId, quantity));
      requestDataSync('cart');
    }
    catch (cause) { setError(apiErrorMessage(cause, 'We could not update this quantity.')); }
    finally { setBusy(false); }
  }, [applyServerLines, lines, productById, user]);

  const clear = useCallback(async () => {
    setError('');
    if (MOCK_MODE) { setLines([]); return true; }
    if (!apiConfigured || !user) { setLines([]); return true; }
    setBusy(true);
    try { await clearRemoteCart(); setLines([]); requestDataSync('cart'); return true; }
    catch (cause) { setError(apiErrorMessage(cause, 'We could not clear your bag.')); return false; }
    finally { setBusy(false); }
  }, [user]);

  const placeOrder = useCallback(async (fulfillmentType, deliveryAddress) => {
    if (!lines.length) return { ok: false, error: 'Your bag is empty.' };
    if (MOCK_MODE) return { ok: false, error: 'Demo mode does not create real orders.' };
    if (!apiConfigured || !user || user.role !== 'customer') return { ok: false, error: 'Sign in as a customer to place an order.' };
    if (!['DELIVERY', 'PICKUP'].includes(fulfillmentType)) return { ok: false, error: 'Choose shop delivery or customer pickup.' };
    if (fulfillmentType === 'DELIVERY' && !deliveryAddress?.trim()) return { ok: false, error: 'Enter a delivery address to continue.' };
    if (needsOrderCheck) return { ok: false, uncertain: true, error: 'Check My Orders to confirm the previous attempt before placing another order.' };
    if (orderAttempt.current) return { ok: false, error: 'Your order is already being submitted. Please wait.' };
    orderAttempt.current = true;
    setBusy(true); setError('');
    try {
      const order = normalizeOrder(await checkoutCart(fulfillmentType, deliveryAddress));
      setLines([]);
      await refresh();
      requestDataSync('orders', 'cart', 'marketplace', 'admin-dashboard');
      return { ok: true, order };
    } catch (cause) {
      const uncertain = !cause.response || cause.response.status >= 500;
      const message = uncertain ? 'We could not confirm whether your order was placed. Check My Orders before trying again.' : apiErrorMessage(cause, 'We could not place the order. Your bag is still saved.');
      if (uncertain) setNeedsOrderCheck(true);
      setError(message); return { ok: false, error: message, uncertain };
    } finally { orderAttempt.current = false; setBusy(false); }
  }, [lines, needsOrderCheck, refresh, user]);

  const items = useMemo(() => lines.map((line) => {
    const product = productById.get(String(line.productId)) || line.product;
    return { ...line, product };
  }).filter((line) => line.product), [lines, productById]);
  const count = items.reduce((sum, line) => sum + line.quantity, 0);
  const subtotal = items.reduce((sum, line) => sum + Number(line.product.currentPrice ?? line.product.price ?? 0) * line.quantity, 0);
  const originalTotal = items.reduce((sum, line) => sum + Number(line.product.wasPrice || line.product.price || 0) * line.quantity, 0);
  const value = useMemo(() => ({ items, count, subtotal, originalTotal, loading, busy, error, clearError, shopSwitch, needsOrderCheck, add, setQuantity, clear, refresh, placeOrder, confirmShopSwitch, cancelShopSwitch }), [items, count, subtotal, originalTotal, loading, busy, error, clearError, shopSwitch, needsOrderCheck, add, setQuantity, clear, refresh, placeOrder, confirmShopSwitch, cancelShopSwitch]);
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);
  if (!value) throw new Error('useCart must be used within CartProvider');
  return value;
}
export { normalizeOrder };
