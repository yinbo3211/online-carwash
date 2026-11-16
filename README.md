# 线上洗车 O2O 平台（Online Carwash Platform）

一个面向 **车主 / 洗车工 / 合作门店 / 后台治理** 四端的线上洗车 O2O 平台完整交付工程。覆盖从需求定义、产品 PRD、技术架构、全栈源码、前端原型到部署运维的全套资产。

> ⚠️ **重要声明**：本仓库为 **V1 内部测试 / 演示项目**。支付为 **模拟支付**（`模拟支付成功·测试数据，不产生真实资金收付`），不接入真实资金通道，禁止用于对外经营。正式上线前需闭环持牌分账、保险合作、ICP/小程序资质、灵活用工法律税务等合规事项。

## 目录结构

| 目录 | 内容 | 说明 |
|------|------|------|
| `backend/` | SSM 全栈工程源码 | Maven 五模块，14 业务域，34 张表，含一键部署脚本 |
| `frontend/` | 前端界面原型 | 纯前端自包含，四端可切换，23 个页面视图 |
| `docs/` | 产品设计文档 | 方案 v2.0~v6.0（PDF + HTML）及 PRD v1.0 |
| `releases/` | 部署交付包 | `洗车平台SSM工程-v6.0.zip` 完整源码打包 |

## 一、业务形态

- **四端**：车主端（下单/支付/评价）、洗车工端（抢单/取车/洗车/还车）、门店端（工位确认/结算/评价/拉黑）、后台端（治理/结算/权限/培训认证）。
- **两种履约**：`STORE` 取送到店、`ONSITE` 纯上门（上门定价比到店高，覆盖门店工位与人力成本）。
- **结算口径**：STORE 平台抽成 40%、ONSITE 35%；抽成基数仅劳务收入 L；金额全程 `BigDecimal` / `DECIMAL(10,2)`。
- **保险**：L1 基础保障每单强制不可取消，L2 增值保障自愿加购。
- **派单资格**：六项合取 `certCovered && scoreOk && capacityOk && !blacklisted && !banned && policyValid`。
- **培训认证**：C1 外观 / C2 标准 / C3 精细 + D 取送车 / O 上门；80 分及格解锁接单资格，24h 补考，认证有有效期。

## 二、技术架构

- **后端**：Java + Spring + SpringMVC + MyBatis（SSM），Maven 多模块。
  - 父坐标 `com.xiche:xiche-platform:1.0.0`
  - 五模块：`xiche-common` / `xiche-pojo` / `xiche-dao` / `xiche-service` / `xiche-web`
  - 业务域包前缀 `com.xiche.<域>.*`，含 user / worker / store / order / catalog / payment / insurance / settlement / coupon / commission / auth / learning / governance / cache 共 14 域。
- **缓存**：CDN → Caffeine → Redis Cluster → MySQL 四级；缓存分 A 可缓存 / B 短 TTL 强一致 / C 禁止缓存（资金、订单、产能、保单、券卡余量），策略 `fail-closed`。
- **权限**：RBAC 功能 / 数据 / 字段三维权限，职责分离，WORM 审计日志。
- **数据库**：MySQL，库名 `xiche`，34 张表，脚本见 `backend/sql/`。
- **前端原型**：HTML/CSS/JS 纯静态，无构建依赖，浏览器直接打开 `frontend/index.html`。

## 三、快速开始

### 后端（Windows 一键部署）

1. 安装 JDK 8 + MySQL 5.7+ + Tomcat 9。
2. 双击 `backend/数据库初始化.bat` 初始化库表（`sql/01_schema.sql` / `02_init_data.sql` / `03_test_data.sql`）。
3. 修改 `backend/xiche-web/src/main/resources/jdbc.properties` 中的数据库连接。
4. 双击 `backend/一键部署.bat` 打包部署；或用 `一键启动.bat` / `一键停止.bat` 管理。
5. 详细说明见 `backend/DEPLOY.md` 与 `backend/README.md`。

### 前端原型

直接用浏览器打开 `frontend/index.html`，顶部可切换车主 / 洗车工 / 门店 / 后台四端视图。

## 四、上架阻塞项（需外部主体闭环）

| 类别 | 事项 |
|------|------|
| 资金 | 持牌分账通道（滴滴代驾式结算需合规分账资质） |
| 保险 | L1/L2 保障需保险机构合作 |
| 资质 | ICP 备案、小程序类目资质、支付商户号 |
| 用工 | 灵活用工法律关系与税务合规 |
| 培训 | 培训/考试认证体系的免费法务意见 |

---

*本项目为软件系统方案与工程交付，不构成任何金融投资建议或法律意见。*
