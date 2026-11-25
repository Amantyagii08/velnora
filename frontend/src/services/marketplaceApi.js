import apiClient, { resolveMediaUrl } from './apiClient.js';

const content = (page) => Array.isArray(page) ? page : page?.content || [];
const mapCategory = (category) => ({ ...category, id: String(category.id), image: resolveMediaUrl(category.imagePath) });
const mapShop = (shop) => ({
  ...shop,
  id: String(shop.id),
  categoryId: String(shop.categoryId),
  category: shop.categoryName || '',
  area: shop.address || '',
  image: resolveMediaUrl(shop.imagePath),
  status: ({ APPROVED: 'Approved', PENDING: 'Pending', REJECTED: 'Rejected' })[shop.approvalStatus || shop.status] || shop.approvalStatus || shop.status || 'Approved',
});
const mapProduct = (product) => {
  const currentPrice = Number(product.currentPrice ?? product.price ?? 0);
  const originalPrice = Number(product.price ?? currentPrice);
  return {
    ...product,
    id: String(product.id),
    shopId: String(product.shopId),
    price: currentPrice,
    wasPrice: currentPrice < originalPrice ? originalPrice : null,
    unit: [product.packSizeValue, product.packSizeUnit].filter((part) => part !== null && part !== undefined && part !== '').join(' ') || '',
    stock: Number(product.stockQuantity || 0),
    image: resolveMediaUrl(product.imagePath),
    clearance: product.dealType === 'CLEARANCE',
    clearanceDiscountAmount: product.dealType === 'CLEARANCE' ? Number(product.discountAmount || 0) : 0,
    clearanceExpiresAt: product.dealExpiresAt || null,
    available: Boolean(product.available),
  };
};

async function allPages(path) {
  const first = await apiClient.get(path, { params: { page: 0, size: 100 } });
  if (Array.isArray(first.data)) return first.data;
  const pages = Math.max(1, Number(first.data?.totalPages || 1));
  if (pages <= 1) return content(first.data);
  const remaining = await Promise.all(Array.from({ length: pages - 1 }, (_, i) =>
    apiClient.get(path, { params: { page: i + 1, size: 100 } }).then((response) => content(response.data))));
  return [...content(first.data), ...remaining.flat()];
}

export async function getPublicMarketplace({ includeCategories = true } = {}) {
  const [categoryRows, shopRows, productRows] = await Promise.all([
    includeCategories ? allPages('/categories') : Promise.resolve(null),
    allPages('/shops/search/page'), allPages('/products/search/page'),
  ]);
  const categories = categoryRows?.map(mapCategory) ?? null;
  const shops = shopRows.map(mapShop);
  const approvedShopIds = new Set(shops.map((shop) => shop.id));
  const products = productRows.map(mapProduct).filter((product) => approvedShopIds.has(product.shopId));
  return { categories, shops, products };
}

export async function getPublicCategories() {
  return (await allPages('/categories')).map(mapCategory);
}

export async function getShop(shopId) { return mapShop((await apiClient.get(`/shops/${shopId}`)).data); }
export async function getShopProducts(shopId, params = {}) {
  return (await apiClient.get(`/shops/${shopId}/products`, { params })).data;
}
export async function searchProducts(keyword, params = {}) {
  return (await apiClient.get('/products/search/page', { params: { ...params, keyword } })).data;
}
export async function searchShopProducts(shopId, keyword, params = {}) {
  return (await apiClient.get(`/shops/${shopId}/products/search`, { params: { ...params, keyword } })).data;
}
export async function compareProducts(params) { return (await apiClient.get('/products/compare', { params })).data; }
export async function getClearanceDeals(params = {}) { return (await apiClient.get('/clearance-deals', { params })).data; }

export async function getMyShop() { return (await apiClient.get('/shops/my-shop')).data; }
export async function getMyShopProducts(shopId, params = {}) {
  const response = await apiClient.get(`/shops/${shopId}/products`, { params });
  return response.data;
}
export async function getAllMyShopProducts(shopId) {
  const first = await getMyShopProducts(shopId, { page: 0, size: 100 });
  const rows = [...content(first)];
  for (let current = 1; current < Math.max(1, Number(first?.totalPages || 1)); current += 1) {
    rows.push(...content(await getMyShopProducts(shopId, { page: current, size: 100 })));
  }
  return rows;
}
export async function createShop(shop, image) {
  const body = new FormData();
  body.append('shop', new Blob([JSON.stringify(shop)], { type: 'application/json' }));
  if (image) body.append('image', image);
  return (await apiClient.post('/shops', body)).data;
}
export async function updateShop(shopId, shop, image) {
  const body = new FormData();
  body.append('shop', new Blob([JSON.stringify(shop)], { type: 'application/json' }));
  if (image) body.append('image', image);
  return (await apiClient.put(`/shops/${shopId}`, body)).data;
}

export async function createProduct(shopId, product, image) {
  const body = new FormData();
  body.append('products', new Blob([JSON.stringify(product)], { type: 'application/json' }));
  if (image) body.append('image', image);
  return mapProduct((await apiClient.post(`/shops/${shopId}/products`, body)).data);
}
export async function updateProduct(shopId, productId, product, image) {
  const body = new FormData();
  body.append('product', new Blob([JSON.stringify(product)], { type: 'application/json' }));
  if (image) body.append('image', image);
  return mapProduct((await apiClient.put(`/shops/${shopId}/products/${productId}`, body)).data);
}
export async function deleteProduct(shopId, productId) { await apiClient.delete(`/shops/${shopId}/products/${productId}`); }
export async function setStock(shopId, productId, quantity, reason) {
  return mapProduct((await apiClient.put(`/shops/${shopId}/products/${productId}/stock`, { quantity, reason })).data);
}
export async function addStock(shopId, productId, quantity) {
  return mapProduct((await apiClient.post(`/shops/${shopId}/products/${productId}/stock`, { quantity })).data);
}
export async function setAvailability(shopId, productId, available) {
  return mapProduct((await apiClient.patch(`/shops/${shopId}/products/${productId}/availability`, null, { params: { available } })).data);
}
export async function setProductClearance(shopId, productId, discountAmount, expiresAt) {
  return mapProduct((await apiClient.post(`/shops/${shopId}/products/${productId}/clearance`, null, {
    params: { discountAmount, ...(expiresAt ? { expiresAt } : {}) },
  })).data);
}
export async function removeProductClearance(shopId, productId) {
  return mapProduct((await apiClient.delete(`/shops/${shopId}/products/${productId}/clearance`)).data);
}
export async function getMyOrders(params = {}) { return (await apiClient.get('/orders/my-orders', { params })).data; }
export async function getOrder(orderId) { return (await apiClient.get(`/orders/${orderId}`)).data; }
export async function getMyShopOrders(params = {}) { return (await apiClient.get('/orders/my-shop-orders', { params })).data; }
export async function updateOrderStatus(orderId, status) {
  return (await apiClient.patch(`/orders/${orderId}/status`, null, { params: { status } })).data;
}

export { content, mapCategory, mapProduct, mapShop };
