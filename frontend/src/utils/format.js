/** 价格展示：后端商品价格是字符串（如 "888.00"），订单价格是数字，统一格式化 */
export function formatPrice(value) {
  const num = Number(value)
  return Number.isFinite(num) ? `¥${num.toFixed(2)}` : '-'
}

/** 订单状态文案 */
export function orderStatusText(status) {
  if (status === 'PENDING') return '待支付'
  if (status === 'PAID') return '已支付'
  return status || '-'
}

/** 订单状态标签颜色 */
export function orderStatusType(status) {
  return status === 'PAID' ? 'success' : 'warning'
}
