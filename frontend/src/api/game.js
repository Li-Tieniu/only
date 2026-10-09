import http, { adminHttp } from './http'

/**
 * 商品列表（用户端）：支持 keyword / min_price / max_price / sort
 * 返回 { items: [{ itemId, coverImage, gameName, price }] }
 */
export const listProducts = (params) => http.get('/gametrading/product', { params })

/** 商品分页详情（用户端）：返回 { total, page, page_size, items: [DetailsVO] } */
export const pageDetails = (params) => http.get('/gametrading/details', { params })

/**
 * 按 itemId 查单个商品。
 * 后端目前没有"按 ID 查详情"的接口，这里用分页详情接口取前 100 条后在内存里匹配，
 * 属于最小可用方案（商品总数超过 100 时可能查不到，详见 README 的限制说明）。
 */
export async function findProductDetail(itemId) {
  const page = await pageDetails({ page: 1, page_size: 100 })
  const items = page?.items || []
  return items.find((item) => String(item.itemId) === String(itemId)) || null
}

/** 管理端：商品列表 */
export const adminListProducts = (params) => adminHttp.get('/gametrading/getinfoproduct', { params })

/** 管理端：商品分页详情 */
export const adminPageDetails = (params) => adminHttp.get('/gametrading/getinfodetails', { params })

/** 管理端：新增商品 */
export const adminAddGame = (data) => adminHttp.post('/gametrading/addgame', data)

/** 管理端：修改商品 */
export const adminUpdateGame = (data) => adminHttp.post('/gametrading/updategame', data)

/** 管理端：软删除商品（逻辑删除，数据仍保留在库里） */
export const adminSoftDeleteGame = (itemId) => adminHttp.post('/gametrading/softdeletegame', { itemId })
