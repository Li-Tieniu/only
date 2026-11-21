/** 金额只在此格式化展示；商品与订单返回 JSON 数字，也兼容旧数字字符串，不参与金额计算。 */
export function formatPrice(value) {
  const num = Number(value)
  return Number.isFinite(num) ? `¥${num.toFixed(2)}` : '-'
}

/** 订单状态文案 */
export function orderStatusText(status) {
  if (status === 'PENDING') return '待支付'
  if (status === 'PAID') return '已支付'
  if (status === 'CANCELLED') return '已取消'
  return status || '-'
}

/** 订单状态标签颜色 */
export function orderStatusType(status) {
  if (status === 'CANCELLED') return 'info'
  return status === 'PAID' ? 'success' : 'warning'
}
