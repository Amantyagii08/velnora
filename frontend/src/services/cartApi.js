import apiClient from './apiClient.js';

export async function getCart() { return (await apiClient.get('/cart')).data; }
export async function addCartItem(productId, quantity = 1, clearExistingCart = false) {
  return (await apiClient.post('/cart/items', { productId: Number(productId), quantity, clearExistingCart })).data;
}
export async function changeCartItem(itemId, quantity) {
  return (await apiClient.patch(`/cart/items/${itemId}`, { quantity })).data;
}
export async function removeCartItem(itemId) { await apiClient.delete(`/cart/items/${itemId}`); }
export async function clearCart() { await apiClient.delete('/cart'); }
export async function checkoutCart(fulfillmentType, deliveryAddress) {
  return (await apiClient.post('/cart/checkout', { fulfillmentType, deliveryAddress: fulfillmentType === 'DELIVERY' ? deliveryAddress : null })).data;
}
