import apiClient from './apiClient.js';
import { mapCategory, mapProduct, mapShop } from './marketplaceApi.js';

async function page(path, params = {}) {
  const first = (await apiClient.get(path, { params: { page: 0, size: 100, ...params } })).data;
  const rows = [...(first.content || [])];
  const totalPages = Math.max(1, Number(first.totalPages || 1));
  for (let current = 1; current < totalPages; current += 1) {
    const next = (await apiClient.get(path, { params: { page: current, size: 100, ...params } })).data;
    rows.push(...(next.content || []));
  }
  return rows;
}
export async function getAdminDashboard() { return (await apiClient.get('/admin/dashboard')).data; }
export async function getAdminMarketplace({ includeCategories = true } = {}) {
  const [shops, categories, products] = await Promise.all([
    page('/admin/shops'),
    includeCategories ? page('/admin/categories') : Promise.resolve(null),
    page('/admin/products'),
  ]);
  return {
    shops: shops.map((row) => mapShop({ ...row, categoryName: '' })),
    categories: categories?.map(mapCategory) ?? null,
    products: products.map(mapProduct),
  };
}
export async function getAdminCategories() { return (await page('/admin/categories')).map(mapCategory); }
export async function getAdminUsers(params = {}) { return page('/admin/users', params); }
export async function setUserSuspended(userId, suspended) { return (await apiClient.patch(`/admin/users/${userId}/suspension`, null, { params: { suspended } })).data; }
export async function setShopSuspended(shopId, suspended) { return (await apiClient.patch(`/admin/shops/${shopId}/suspension`, null, { params: { suspended } })).data; }
export async function setProductActive(productId, active) { return (await apiClient.patch(`/admin/products/${productId}/active`, null, { params: { active } })).data; }
export async function setCategoryActive(categoryId, active) { return (await apiClient.patch(`/admin/categories/${categoryId}/active`, null, { params: { active } })).data; }
