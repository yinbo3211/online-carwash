# 线上洗车平台 v6.0 · 客户端接口文档

> 适用范围：`clients/miniprogram/`（微信小程序）、`clients/app/`（uni-app → App/H5）、`clients/h5/`（纯静态 H5）
> 后端：SSM 工程 `xiche-*` 模块
> 统一响应体：`{ "code": 0, "msg": "ok", "data": { ... } }`（`code` 为 `0` 或 `200` 视为业务成功）
> 鉴权：除公开接口外，请求头携带 `Authorization: Bearer <token>`
> **金额口径：所有金额字段单位均为「分」，前端展示时除以 100；文档中的价格为示例值。**
> **V1 测试版：支付接口为模拟接口，不产生真实资金收付；成功文案为橙色「模拟支付成功」。**

---

## 一、baseURL 配置（如何指向后端）

三端均为**集中配置**，页面代码中不出现硬编码域名：

| 端 | 配置文件与变量 | 默认 | 改指向后端 |
|---|---|---|---|
| 微信小程序 | `miniprogram/app.js` → `globalData.baseURL`；回退 `miniprogram/utils/constants.js` → `BASE_URL` | `/api/` | 改为 `https:// your-domain.com/api/`（需在小程序后台配置 request 合法域名，HTTPS + 备案） |
| uni-app | `app/App.vue` → `globalData.baseURL`；回退 `app/common/api.js` → `FALLBACK_BASE_URL` | `/api/` | 同上；H5 调试可在 `app/manifest.json` → `h5.devServer.proxy['/api'].target` 配后端地址 |
| 纯静态 H5 | `h5/js/app.js` 顶部 → `API_BASE`（并设 `USE_BACKEND = true`） | `/api/` | 保持 `/api/` 并用同域 Nginx 反代，或将 `API_BASE` 改为后端完整地址 |

- 请求 URL 由封装层拼接：`baseURL` 去掉尾部 `/` + 接口路径去掉头部 `/`。
- 如后端尚未就绪：小程序/uni-app 的 `*.then().catch()` 保留本地示例数据降级；H5 默认 `USE_BACKEND = false`，直接走演示流程。

---

## 二、公共接口

### 2.1 上传证据图
`POST /api/common/upload`
- 入参（`multipart/form-data`）：`file`（图片文件）、`orderId`（订单号）、`stage`（`E1`~`E4`）
- 返回：`data: { "url": "https://.../evidence/xxx.jpg" }`
- 说明：客户端强制相机拍摄（`sourceType: ['camera']`），服务端叠加订单号水印；相册入口灰显，文案「仅支持现场拍摄」。

---

## 三、车主端 `/api/user/`

| 方法 | 路径 | 入参 | 返回 `data` | 说明 |
|---|---|---|---|---|
| GET | `/api/user/home` | — | `{ tiers: [{key,name,priceFen,desc,recommend}], banners: [] }` | 首页示例价可由服务端覆盖 |
| POST | `/api/user/orders` | `{ tier, serviceMode, address, phone, addonKeys[], insurance:{l1Accepted,l1Cancelable,l2Selected}, couponFen, amountFen, requestId }` | `{ orderId, orderNo, amountFen, status }` | 下单；`amountFen` 单位分；`requestId` 幂等键，`l1Cancelable` 恒为 `false` |
| GET | `/api/user/orders/{id}` | 路径参数 `id` | `{ order:{orderNo,tier,statusIndex,amountFen,l2,...}, evidence:[{stage,photos:[]}] }` | 订单进度（8 态 Stepper + 证据） |
| GET | `/api/user/orders` | `page`, `pageSize` | `{ list:[], total }` | 订单列表（强制分页） |
| POST | `/api/user/orders/{id}/simulate-pay` | `{ amountFen, requestId, payMode:'simulate' }` | `{ paid:true, paidAt, amountFen }` | **模拟支付**；不接真实资金通道，成功文案橙色「模拟支付成功」 |
| POST | `/api/user/orders/{id}/cancel` | `{ reason }` | `{ canceled:true }` | 取消订单 |

**订单 8 态 `statusIndex`**：`0`待支付 → `1`待接单 → `2`已接单 → `3`取车中 → `4`洗车中 → `5`配送中 → `6`待验收 → `7`完成。
**服务档位 `tier`**：`basic`(¥83=8300分) / `standard`(¥133=13300分，推荐) / `premium`(¥243=24300分)。
**上门价**：`basic` 9500 分、`standard` 15500 分；`premium` 不支持上门。
**保险**：`l1Accepted` 恒为 `true` 且 `l1Cancelable` 恒为 `false`（L1 强制锁定）；`l2Selected` 可选，1200 分。

---

## 四、洗车工端 `/api/worker/`

| 方法 | 路径 | 入参 | 返回 `data` | 说明 |
|---|---|---|---|---|
| GET | `/api/worker/certs` | — | `{ list:[{key:'C1'\|'C2'\|'C3'\|'D'\|'O', name, passed}] }` | 资质；C3 需考试 ≥80 分 |
| GET | `/api/worker/hall` | `page`, `pageSize` | `{ tasks:[{orderNo,tier,serviceName,modeText,address,feeFen,countdown,requiredCert}] }` | 抢单大厅；资质不足由前端置灰锁定 |
| POST | `/api/worker/orders/{id}/grab` | `{ requestId }` | `{ grabbed:true, lockUntil }` | 抢单，90 秒确认锁 |
| GET | `/api/worker/orders/{id}` | 路径参数 | `{ order, evidence, steps }` | 任务详情（E1~E4） |
| POST | `/api/worker/evidence` | `{ orderNo, stage:'E1'~'E4', photos:[] }` | `{ saved:true }` | 提交证据；完成上一步才解锁下一步 |
| GET | `/api/worker/exams` | — | `{ questions:[{id,q,options[],answer}] }` | C3 在线考试题库，80 分及格 |
| POST | `/api/worker/exams/submit` | `{ cert:'C3', score, answers[] }` | `{ passed, certUnlocked }` | 判分以服务端为准 |
| GET | `/api/worker/income` | `page`, `pageSize` | `{ bills:[{id,title,no,date,amountFen,feeFen}], totalFen }` | 收入钱包；服务费 35% / 洗车工分成 40%（示例） |

---

## 五、门店端 `/api/store/`

| 方法 | 路径 | 入参 | 返回 `data` | 说明 |
|---|---|---|---|---|
| GET | `/api/store/bays` | — | `{ bays:[{id,name,status:'free'\|'full'\|'paused'}] }` | 工位三态看板 |
| PUT | `/api/store/bays/{id}` | `{ status }` | `{ updated:true }` | 切换工位状态 |
| POST | `/api/store/orders/{id}/accept` | `{ bayId, requestId }` | `{ accepted:true, bayStatus:'full' }` | 接单；无 `free` 工位时前端拦截，服务端亦须校验 |
| GET | `/api/store/reviews` | `page`, `pageSize` | `{ list:[], total }` | 评价列表 |
| POST | `/api/store/reviews/{id}/reply` | `{ scores:[{dim,score,reason}], comment }` | `{ saved:true }` | 五维评价；≤2 星必须填 `reason`（服务端校验） |

**五维 `dim`**：`attitude`服务态度 / `quality`清洁质量 / `punctual`准时交付 / `protect`车辆保护 / `communicate`沟通响应。
**门店结算**：仅显示工位费 M 部分，不下发用户支付与平台服务费明细。

---

## 六、后台端 `/api/admin/`

契约中已预留 `/api/admin/` 前缀；本轮三端客户端（车主/洗车工/门店）**未调用**后台接口，由后续后台管理端实现。

---

## 七、统一错误码约定

| code | 含义 | 客户端行为 |
|---|---|---|
| 0 / 200 | 成功 | 解包 `data` |
| 400 | 参数错误 | toast 显示 `msg` |
| 401 | 未登录/登录过期 | 清除本地 token |
| 403 | 权限/认证不足 | 展示置灰锁定 + 去补认证入口 |
| 409 | 冲突（无空位/已被抢） | 弹窗提示，保持当前状态 |
| 500 | 服务端错误 | toast 提示，保留本地示例数据降级 |

---

## 八、联调检查清单

1. 三端 `baseURL` 均指向同一后端，接口返回必须是 `{code,msg,data}`（`msg` 非 `message`，客户端兼容两者）。
2. 金额字段一律分；前端 `fen2Yuan()` 展示为元并标注「示例价」。
3. 小程序 request/upload 域名加入白名单（HTTPS + 备案）；H5 用 Nginx 反代 `/api/` 或开启 `manifest.json` devServer proxy。
4. 支付成功页必须为**橙色**「模拟支付成功」，全程顶部常驻琥珀斜纹横幅；禁止出现绿色「支付成功」。
5. L1 强制锁定不可取消、C3 未通过订单置灰锁定、工位无空位拦截接单——前后端双重校验。
6. 下单、抢单、模拟支付、接单均携带幂等 `requestId`，服务端需去重。
