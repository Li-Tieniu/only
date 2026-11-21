import http, { adminHttp } from './http'

/**
 * 商品列表（用户端）：支持 keyword / min_price / max_price / sort
 * 返回 { items: [{ itemId, coverImage, gameName, price }] }
 */
export const listProducts = (params) => http.get('/gametrading/product', { params })

/** 商品分页详情（用户端）：返回 { total, page, page_size, items: [DetailsVO] } */
export const pageDetails = (params) => http.get('/gametrading/details', { params })

/** 按 itemId 查询单个商品详情，返回 DetailsVO。 */
export async function findProductDetail(itemId) {
  return http.get(`/gametrading/details/${itemId}`)
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
