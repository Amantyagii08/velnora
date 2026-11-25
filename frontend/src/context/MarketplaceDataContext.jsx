import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { categories as baseCategories, products as baseProducts, shops as baseShops } from '../data/marketplace.js';
import { apiConfigured, apiErrorMessage } from '../services/apiClient.js';
import { createProduct as apiCreateProduct, createShop as apiCreateShop, deleteProduct as apiDeleteProduct, getMyShop, getAllMyShopProducts, getPublicCategories, getPublicMarketplace, mapCategory, mapProduct, mapShop, removeProductClearance as apiRemoveProductClearance, setProductClearance as apiSetProductClearance, updateProduct as apiUpdateProduct, updateShop as apiUpdateShop } from '../services/marketplaceApi.js';
import apiClient from '../services/apiClient.js';
import { useAuth } from './AuthContext.jsx';
import { getAdminCategories, getAdminMarketplace, setCategoryActive as setAdminCategoryActive, setProductActive } from '../services/adminApi.js';
import { getSyncGeneration, requestDataSync, singleFlight, STATIC_SYNC_INTERVAL, SYNC_INTERVAL } from '../services/dataSync.js';
import useDataSyncTask from '../hooks/useDataSyncTask.js';

const MarketplaceDataContext = createContext(null);
const KEY = 'velnora-marketplace-demo-data-v1';
const MOCK_MODE = import.meta.env.VITE_USE_MOCK_DATA === 'true' && !apiConfigured;
const emptyData = { products: [], shops: [], categories: [] };
function readDemoData() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY) || '{}');
    const products = baseProducts.map((product) => ({ ...product, ...(saved.products?.[product.id] || {}) }));
    const addedProducts = Array.isArray(saved.addedProducts) ? saved.addedProducts : [];
    const shops = baseShops.map((shop) => ({ ...shop, status: 'Approved', ...(saved.shops?.[shop.id] || {}) }));
    const removed = new Set(saved.removedCategories || []);
    const categories = [...baseCategories.filter((item) => !removed.has(item.id)).map((item) => ({ ...item, ...(saved.categories?.[item.id] || {}) })), ...(saved.addedCategories || [])];
    return { products: [...products, ...addedProducts], shops, categories };
  } catch { return { products: baseProducts, shops: baseShops, categories: baseCategories }; }
}
function packFields(unit = '') {
  const match = String(unit).trim().match(/^([\d.]+)\s*(.*)$/);
  return match ? { packSizeValue: Number(match[1]), packSizeUnit: match[2].trim() || null } : { packSizeValue: null, packSizeUnit: String(unit).trim() || null };
}

export function MarketplaceDataProvider({ children }) {
  const { user } = useAuth();
  const [data, setData] = useState(() => MOCK_MODE ? readDemoData() : emptyData);
  const dataRef = useRef(data);
  dataRef.current = data;
  const [loading, setLoading] = useState(apiConfigured);
  const [error, setError] = useState(apiConfigured ? '' : MOCK_MODE ? '' : 'Marketplace service is not configured. Set VITE_API_BASE_URL to connect to Velnora.');
  const hasLoaded = useRef(false);
  const identityRef = useRef(user ? `${user.id}:${user.role}` : null);

  const refresh = useCallback((options = {}) => singleFlight(`marketplace:${user?.id ?? 'public'}:${user?.role ?? 'guest'}`, async () => {
    const { includeCategories = true, isCurrent = () => true } = options;
    const generation = getSyncGeneration('marketplace');
    const categoryGeneration = getSyncGeneration('categories');
    const isFresh = () => isCurrent() && generation === getSyncGeneration('marketplace')
      && (!includeCategories || categoryGeneration === getSyncGeneration('categories'));
    if (!apiConfigured || MOCK_MODE) return;
    if (!hasLoaded.current) setLoading(true);
    try {
      const next = user?.role === 'admin' ? await getAdminMarketplace({ includeCategories }) : await getPublicMarketplace({ includeCategories });
      if (!isFresh()) return;
      const currentData = dataRef.current;
      const previousProducts = new Map(currentData.products.map((product) => [product.id, product]));
      const inventoryChanged = currentData.products.length !== next.products.length || next.products.some((product) => {
        const previous = previousProducts.get(product.id);
        return !previous || previous.stock !== product.stock || previous.price !== product.price
          || previous.available !== product.available || previous.active !== product.active;
      });
      setData((current) => {
        const categories = next.categories ?? current.categories;
        const categoryById = new Map(categories.map((category) => [category.id, category.name]));
        return { categories, shops: next.shops.map((shop) => ({ ...shop, category: categoryById.get(shop.categoryId) || shop.category || '' })), products: next.products };
      });
      hasLoaded.current = true;
      setError('');
      if (inventoryChanged) requestDataSync('cart');
    } catch (cause) {
      if (isFresh()) setError(apiErrorMessage(cause, 'We could not load the marketplace. Please try again.'));
    } finally {
      if (isFresh()) setLoading(false);
    }
  }), [user]);

  const refreshCategories = useCallback(async ({ isCurrent = () => true } = {}) => {
    if (!apiConfigured || MOCK_MODE) return;
    const generation = getSyncGeneration('categories');
    try {
      const categories = user?.role === 'admin' ? await getAdminCategories() : await getPublicCategories();
      if (isCurrent() && generation === getSyncGeneration('categories')) {
        const categoryById = new Map(categories.map((category) => [category.id, category.name]));
        setData((current) => ({ ...current, categories, shops: current.shops.map((shop) => ({ ...shop, category: categoryById.get(shop.categoryId) || shop.category || '' })) }));
      }
    } catch { /* Keep the last usable category list when a refresh fails. */ }
  }, [user]);

  useEffect(() => {
    const identity = user ? `${user.id}:${user.role}` : null;
    if (identityRef.current !== identity) {
      identityRef.current = identity;
      hasLoaded.current = false;
      setData(emptyData);
      setError('');
    }
    if (apiConfigured && !MOCK_MODE) refresh();
  }, [refresh, user?.id, user?.role]);
  useDataSyncTask('marketplace', (context) => refresh({ ...context, includeCategories: false }), SYNC_INTERVAL, apiConfigured && !MOCK_MODE);
  useDataSyncTask('categories', refreshCategories, STATIC_SYNC_INTERVAL, apiConfigured && !MOCK_MODE);

  useEffect(() => {
    if (!MOCK_MODE) return;
    try {
      const old = JSON.parse(localStorage.getItem(KEY) || '{}');
      localStorage.setItem(KEY, JSON.stringify({
        ...old,
        products: Object.fromEntries(data.products.filter((p) => baseProducts.some((base) => base.id === p.id)).map((p) => [p.id, p])),
        addedProducts: data.products.filter((p) => !baseProducts.some((base) => base.id === p.id)),
        shops: Object.fromEntries(data.shops.map((shop) => [shop.id, shop])),
        categories: Object.fromEntries(data.categories.filter((c) => baseCategories.some((base) => base.id === c.id)).map((c) => [c.id, c])),
        addedCategories: data.categories.filter((c) => !baseCategories.some((base) => base.id === c.id)),
        removedCategories: baseCategories.filter((c) => !data.categories.some((current) => current.id === c.id)).map((c) => c.id),
      }));
    } catch { /* Keep demo data available for this session. */ }
  }, [data]);

  const updateProduct = useCallback(async (id, changes) => {
    const product = data.products.find((item) => item.id === id);
    if (!product) throw new Error('Product is no longer available. Refresh and try again.');
    if (MOCK_MODE) { setData((current) => ({ ...current, products: current.products.map((item) => item.id === id ? { ...item, ...changes } : item) })); return true; }
    if (!user || user.role !== 'shopkeeper') throw new Error('Only the shopkeeper can update this product.');
    if (Object.keys(changes).length === 1 && Object.hasOwn(changes, 'stock')) {
      await apiClient.put(`/shops/${product.shopId}/products/${product.id}/stock`, { quantity: Number(changes.stock) });
      await requestDataSync('marketplace', 'seller-shop', 'admin-dashboard'); return true;
    }
    const basePrice = Number(changes.wasPrice || changes.price || product.wasPrice || product.price);
    const result = await apiUpdateProduct(product.shopId, product.id, {
      name: changes.name ?? product.name, description: changes.description ?? product.description ?? '',
      brand: changes.brand ?? product.brand ?? '', variant: changes.variant ?? product.variant ?? '',
      ...packFields(changes.unit ?? product.unit), price: basePrice,
    }, changes.imageFile);
    if (changes.wasPrice && Number(changes.price) < Number(changes.wasPrice)) {
      await apiClient.post(`/shops/${product.shopId}/products/${product.id}/discount`, { discountAmount: basePrice - Number(changes.price) });
    } else if (!changes.wasPrice && product.wasPrice) await apiClient.delete(`/shops/${product.shopId}/products/${product.id}/discount`);
    setData((current) => ({ ...current, products: current.products.map((item) => item.id === id ? { ...item, ...result, ...changes } : item) }));
    requestDataSync('marketplace', 'seller-shop', 'admin-dashboard');
    return true;
  }, [data.products, user, refresh]);

  const addProduct = useCallback(async (product) => {
    if (MOCK_MODE) { setData((current) => ({ ...current, products: [...current.products, product] })); return product; }
    if (!user || user.role !== 'shopkeeper') throw new Error('Only the shopkeeper can add products.');
    const created = await apiCreateProduct(product.shopId, {
      name: product.name, description: product.description || '', brand: product.brand, variant: product.variant,
      ...packFields(product.unit), price: Number(product.wasPrice || product.price), stockQuantity: Number(product.stock || 0),
    }, product.imageFile);
    if (product.wasPrice && Number(product.price) < Number(product.wasPrice)) await apiClient.post(`/shops/${product.shopId}/products/${created.id}/discount`, { discountAmount: Number(product.wasPrice) - Number(product.price) });
    setData((current) => ({ ...current, products: [...current.products, created] }));
    requestDataSync('marketplace', 'seller-shop', 'admin-dashboard');
    return created;
  }, [user]);

  const setProductClearance = useCallback(async (id, discountAmount, expiresAt) => {
    const product = data.products.find((item) => String(item.id) === String(id));
    if (!product) throw new Error('Product is no longer available. Refresh and try again.');
    if (MOCK_MODE) {
      const originalPrice = product.clearance ? Number(product.price) + Number(product.clearanceDiscountAmount || 0) : Number(product.price);
      const updated = { ...product, price: originalPrice - Number(discountAmount), wasPrice: originalPrice,
        clearance: true, clearanceDiscountAmount: Number(discountAmount), clearanceExpiresAt: expiresAt || null };
      setData((current) => ({ ...current, products: current.products.map((item) => String(item.id) === String(id) ? updated : item) }));
      return updated;
    }
    if (user?.role !== 'shopkeeper') throw new Error('Only the shopkeeper can manage this clearance deal.');
    const updated = await apiSetProductClearance(product.shopId, product.id, Number(discountAmount), expiresAt || undefined);
    setData((current) => ({ ...current, products: current.products.map((item) => String(item.id) === String(id) ? { ...item, ...updated } : item) }));
    requestDataSync('marketplace', 'seller-shop', 'admin-dashboard');
    return updated;
  }, [data.products, user]);

  const removeProductClearance = useCallback(async (id) => {
    const product = data.products.find((item) => String(item.id) === String(id));
    if (!product) throw new Error('Product is no longer available. Refresh and try again.');
    if (MOCK_MODE) {
      const updated = { ...product, price: Number(product.wasPrice || product.price), wasPrice: null,
        clearance: false, clearanceDiscountAmount: 0, clearanceExpiresAt: null };
      setData((current) => ({ ...current, products: current.products.map((item) => String(item.id) === String(id) ? updated : item) }));
      return updated;
    }
    if (user?.role !== 'shopkeeper') throw new Error('Only the shopkeeper can manage this clearance deal.');
    const updated = await apiRemoveProductClearance(product.shopId, product.id);
    setData((current) => ({ ...current, products: current.products.map((item) => String(item.id) === String(id) ? { ...item, ...updated } : item) }));
    requestDataSync('marketplace', 'seller-shop', 'admin-dashboard');
    return updated;
  }, [data.products, user]);

  const updateShop = useCallback(async (id, changes) => {
    if (MOCK_MODE) { setData((current) => ({ ...current, shops: current.shops.map((shop) => shop.id === id ? { ...shop, ...changes } : shop) })); return true; }
    if (Object.hasOwn(changes, 'status')) {
      const status = changes.status === 'Approved' ? 'APPROVED' : changes.status === 'Rejected' ? 'REJECTED' : 'PENDING';
      const updated = (await apiClient.patch(`/admin/shops/${id}/approval`, null, { params: { status, ...(changes.rejectionReason ? { rejectionReason: changes.rejectionReason } : {}) } })).data;
      setData((current) => ({ ...current, shops: current.shops.map((shop) => shop.id === id ? mapShop(updated) : shop) }));
      requestDataSync('marketplace', 'categories', 'admin-dashboard', 'seller-shop'); return true;
    }
    if (Object.hasOwn(changes, 'active') && user?.role === 'admin') {
      const updated = (await apiClient.patch(`/admin/shops/${id}/suspension`, null, { params: { suspended: !changes.active } })).data;
      setData((current) => ({ ...current, shops: current.shops.map((shop) => shop.id === id ? { ...shop, active: updated.active } : shop) }));
      requestDataSync('marketplace', 'admin-dashboard');
      return true;
    }
    if (!user || user.role !== 'shopkeeper') throw new Error('Only the shopkeeper can edit this shop.');
    const shop = data.shops.find((item) => item.id === id);
    if (!shop) throw new Error('Shop is no longer available. Refresh and try again.');
    const updated = await apiUpdateShop(id, {
      name: changes.name ?? shop.name, description: changes.description ?? shop.description ?? '',
      address: changes.area ?? changes.address ?? shop.address ?? '', categoryId: Number(shop.categoryId),
      contactPhone: shop.contactPhone || '', contactEmail: shop.contactEmail || '',
    }, changes.imageFile);
    const shopRecord = mapShop({ ...updated.shop, approvalStatus: updated.approvalStatus, rejectionReason: updated.rejectionReason });
    setData((current) => ({ ...current, shops: current.shops.map((item) => item.id === id ? { ...item, ...shopRecord, ...changes, area: changes.area ?? shopRecord.address } : item) }));
    requestDataSync('marketplace', 'seller-shop', 'admin-dashboard'); return true;
  }, [data.shops, user]);

  const deleteProduct = useCallback(async (id) => {
    const product = data.products.find((item) => String(item.id) === String(id));
    if (!product) return false;
    if (!MOCK_MODE) {
      if (!user || user.role !== 'shopkeeper') throw new Error('Only the shopkeeper can remove this product.');
      await apiDeleteProduct(product.shopId, product.id);
    }
    setData((current) => ({ ...current, products: current.products.filter((item) => String(item.id) !== String(id)) }));
    requestDataSync('marketplace', 'seller-shop', 'admin-dashboard');
    return true;
  }, [data.products, user]);

  const setProductVisibility = useCallback(async (id, active) => {
    if (MOCK_MODE) { setData((current) => ({ ...current, products: current.products.map((item) => String(item.id) === String(id) ? { ...item, active } : item) })); return true; }
    if (user?.role !== 'admin') throw new Error('Only administrators can change product visibility.');
    const updated = mapProduct(await setProductActive(id, active));
    setData((current) => ({ ...current, products: current.products.map((item) => String(item.id) === String(id) ? { ...item, ...updated } : item) }));
    requestDataSync('marketplace', 'admin-dashboard');
    return true;
  }, [user]);

  const getSellerShop = useCallback((options = {}) => singleFlight(`seller-shop:${user?.id ?? 'seller'}`, async () => {
    const { isCurrent = () => true } = options;
    if (MOCK_MODE) return dataRef.current.shops.find((item) => String(item.id) === String(user?.shopId)) || null;
    const generation = getSyncGeneration('seller-shop');
    const isFresh = () => isCurrent() && generation === getSyncGeneration('seller-shop');
    const response = await getMyShop();
    if (!isFresh()) return null;
    const shop = mapShop({ ...response.shop, approvalStatus: response.approvalStatus, rejectionReason: response.rejectionReason });
    setData((current) => ({ ...current, shops: [...current.shops.filter((item) => item.id !== shop.id), shop] }));
    if (response.approvalStatus === 'APPROVED') {
      const rows = (await getAllMyShopProducts(shop.id)).map(mapProduct);
      if (isFresh()) setData((current) => ({ ...current, products: [...current.products.filter((item) => item.shopId !== shop.id), ...rows] }));
    }
    return shop;
  }), [user]);

  const createShopApplication = useCallback(async ({ shop, image }) => {
    if (MOCK_MODE) throw new Error('Shop applications are unavailable in demo mode.');
    const response = await apiCreateShop(shop, image);
    const created = mapShop({ ...response.shop, approvalStatus: response.approvalStatus, rejectionReason: response.rejectionReason });
    setData((current) => ({ ...current, shops: [...current.shops.filter((item) => item.id !== created.id), created] }));
    requestDataSync('marketplace', 'admin-dashboard', 'seller-shop');
    return created;
  }, []);

  const addCategory = useCallback(async (category) => {
    if (MOCK_MODE) { setData((current) => ({ ...current, categories: [...current.categories, category] })); return category; }
    const body = new FormData(); body.append('name', category.name);
    if (category.imageFile) body.append('image', category.imageFile);
    const created = mapCategory((await apiClient.post('/admin/categories', body)).data);
    setData((current) => ({ ...current, categories: [...current.categories, created] }));
    requestDataSync('categories', 'marketplace', 'admin-dashboard'); return created;
  }, []);
  const updateCategory = useCallback(async (id, changes) => {
    if (MOCK_MODE) { setData((current) => ({ ...current, categories: current.categories.map((category) => category.id === id ? { ...category, ...changes } : category) })); return true; }
    const body = new FormData(); body.append('name', changes.name);
    if (changes.imageFile) body.append('image', changes.imageFile);
    const updated = mapCategory((await apiClient.put(`/admin/categories/${id}`, body)).data);
    setData((current) => ({ ...current, categories: current.categories.map((category) => category.id === id ? updated : category) }));
    requestDataSync('categories', 'marketplace', 'admin-dashboard'); return true;
  }, []);
  const removeCategory = useCallback(async (id) => {
    if (data.shops.some((shop) => shop.categoryId === id)) return false;
    if (MOCK_MODE) { setData((current) => ({ ...current, categories: current.categories.filter((category) => category.id !== id) })); return true; }
    await apiClient.delete(`/admin/categories/${id}`);
    setData((current) => ({ ...current, categories: current.categories.filter((category) => category.id !== id) }));
    requestDataSync('categories', 'marketplace', 'admin-dashboard'); return true;
  }, [data.shops]);

  const setCategoryActive = useCallback(async (id, active) => {
    if (MOCK_MODE) { setData((current) => ({ ...current, categories: current.categories.map((category) => category.id === id ? { ...category, active } : category) })); return true; }
    if (user?.role !== 'admin') throw new Error('Only administrators can change category availability.');
    const updated = mapCategory(await setAdminCategoryActive(id, active));
    setData((current) => ({ ...current, categories: current.categories.map((category) => category.id === id ? updated : category) }));
    requestDataSync('categories', 'marketplace', 'admin-dashboard');
    return true;
  }, [user]);

  const value = useMemo(() => ({ ...data, loading, error, isMock: MOCK_MODE, refresh: () => refresh(), getSellerShop, createShopApplication, updateProduct, addProduct, deleteProduct, setProductVisibility, setProductClearance, removeProductClearance, updateShop, addCategory, updateCategory, removeCategory, setCategoryActive }), [data, loading, error, refresh, getSellerShop, createShopApplication, updateProduct, addProduct, deleteProduct, setProductVisibility, setProductClearance, removeProductClearance, updateShop, addCategory, updateCategory, removeCategory, setCategoryActive]);
  return <MarketplaceDataContext.Provider value={value}>{children}</MarketplaceDataContext.Provider>;
}
export function useMarketplaceData() {
  const value = useContext(MarketplaceDataContext);
  if (!value) throw new Error('useMarketplaceData must be used inside MarketplaceDataProvider');
  return value;
}
