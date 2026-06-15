import http, { SVC } from './http'

// 商品 / 票种（product-service 8081）
export const productApi = {
  listProducts: () => http.get(`${SVC.product}/admin/v1/products`),
  createProduct: (body) => http.post(`${SVC.product}/admin/v1/products`, body),
  updateStatus: (id, status) => http.patch(`${SVC.product}/admin/v1/products/${id}/status`, { status }),
  listSkus: (productId) => http.get(`${SVC.product}/admin/v1/skus`, { params: { product_id: productId } }),
  createSku: (body) => http.post(`${SVC.product}/admin/v1/skus`, body),
}

// 价格日历（pricing-service 8082）
export const pricingApi = {
  upsert: (body) => http.put(`${SVC.pricing}/admin/v1/prices`, body),
  list: (skuId, saleDate) => http.get(`${SVC.pricing}/admin/v1/prices`, { params: { sku_id: skuId, sale_date: saleDate || undefined } }),
}

// 库存桶（inventory-service 8084）
export const inventoryApi = {
  upsertBucket: (body) => http.post(`${SVC.inventory}/admin/v1/buckets`, body),
  getBucket: (skuId, saleDate, timeSlotId) =>
    http.get(`${SVC.inventory}/admin/v1/buckets`, { params: { sku_id: skuId, sale_date: saleDate, time_slot_id: timeSlotId } }),
}

// 订单（order-service 8087）
export const orderApi = {
  list: (status) => http.get(`${SVC.order}/admin/v1/orders`, { params: { status: status || undefined } }),
  detail: (orderId) => http.get(`${SVC.order}/api/v1/orders/${orderId}`),
}

// 商户分账（payment-service 8085）
export const paymentApi = {
  upsertCommission: (merchantId, rate) =>
    http.put(`${SVC.payment}/admin/v1/merchant-commissions`, { merchant_id: merchantId, commission_rate: rate }),
}

// 核销密钥（ticket-verification-service 8086）
export const ticketApi = {
  rotateKey: (scenicId) => http.post(`${SVC.ticket}/admin/v1/verify-keys/rotate`, null, { params: { scenic_id: scenicId } }),
}

// 跨业态对账（reconciliation-service 8093）
export const reconApi = {
  daily: (date, merchantId) =>
    http.get(`${SVC.reconciliation}/api/v1/reconciliation/daily`, { params: { date, merchant_id: merchantId || undefined } }),
}
