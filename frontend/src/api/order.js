import http from './http'

/** 创建订单：只需传商品 ID，用户身份由后端从 Token 中取 */
export const createOrder = (itemId) => http.post('/order/create', { itemId })

/** 我的订单（分页）：返回 { total, page, items: [OrderVO] } */
export const pageMyOrders = (params) => http.get('/order/my', { params })

/** 订单详情（后端会校验订单归属） */
export const getOrder = (orderNo) => http.get(`/order/${orderNo}`)

/** 模拟支付订单 */
export const payOrder = (orderNo) => http.post(`/order/${orderNo}/pay`)
