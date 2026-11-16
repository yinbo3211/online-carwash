# 线上洗车平台 v6.0 · SSM 后端工程

> **一句话定位**：支持到店洗车与上门洗车双形态的线上预约平台 V1 内部测试版后端——Java SSM 多模块 + MySQL 8，war 部署到 Tomcat 9，供小程序 / App / H5 / 后台四端联调使用；**支付为模拟支付，不产生真实资金收付，不得对外经营**。

---

## 1. 目录结构

```
.
├── pom.xml                        父工程 xiche-platform（5 个 Maven 模块）
├── xiche-common\                  公共常量 / 工具 / 统一响应 / 全局异常
├── xiche-pojo\                    实体 / DTO / VO
├── xiche-dao\                     MyBatis Mapper 接口 + XML
├── xiche-service\                 14 个业务域服务与实现
├── xiche-web\                     Controller / 配置 / 后台 JSP ★ 打 war 的模块
│   └── src\main\resources\jdbc.properties
├── clients\                       三端源码
│   ├── miniprogram\               微信小程序（utils/api.js、utils/constants.js）
│   ├── app\                       App（用户 / 洗车工 / 门店）
│   └── h5\                        H5
├── sql\
│   ├── 01_schema.sql              建表结构
│   ├── 02_init_data.sql           初始化基础数据
│   └── 03_test_data.sql           V1 测试数据（禁止用于生产）
├── docs\
│   ├── 模块清单.md                工程模块树、14 个业务域、各端目录
│   └── 架构说明.md                SSM 分层、请求链路、模拟支付/缓存/认证
├── 一键部署.bat                   ★ 全流程一键部署主脚本
├── 一键启动.bat                   仅启动 Tomcat
├── 一键停止.bat                   仅停止 Tomcat
├── 数据库初始化.bat               仅建库 + 导入三个 SQL
├── DEPLOY.md                      Windows 部署手册（详尽）
└── README.md                      本文件
```

---

## 2. 快速开始（3 步）

> 环境要求：JDK 8 + Maven 3.6+ + Tomcat 9 + MySQL 8（安装要点见 `DEPLOY.md` 第 1 节）。

**① 改配置**：用记事本打开 `一键部署.bat`，修改顶部「变量配置区」的 `JAVA_HOME`、`MAVEN_HOME`、`CATALINA_HOME`、`MYSQL_HOME`、`DB_ROOT_PWD`（MySQL root 密码）。

**② 一键部署**：右键 `一键部署.bat` → **以管理员身份运行**。脚本自动完成：环境检测 → 建库授权 → 导入 3 个 SQL → 改写 `jdbc.properties` → `mvn clean package -DskipTests` → 复制 war 到 Tomcat → 启动。

**③ 验证**：等待 10~60 秒后访问 `http://localhost:8080/`，用 `admin / admin123` 登录。

日常迭代只改代码时：先 `一键停止.bat` → 改代码 → 重跑 `一键部署.bat`（可把 `SKIP_DB_INIT` 设为 `1` 跳过数据库初始化）；仅重启服务用 `一键启动.bat` / `一键停止.bat`。

---

## 3. 文档索引

| 文档 | 内容 | 适合谁 |
|------|------|--------|
| [`DEPLOY.md`](DEPLOY.md) | 环境准备、两种数据库初始化路径、jdbc.properties 修改、打包部署、三端连后端、常见问题排查、合规声明 | 部署/联调同学（**先看这个**） |
| [`docs/模块清单.md`](docs/模块清单.md) | Maven 模块树、14 个业务域、各端目录说明、依赖规则 | 想快速定位代码的人 |
| [`docs/架构说明.md`](docs/架构说明.md) | SSM 分层、请求处理链路、模拟支付/缓存分级/认证门槛实现、数据模型与风险预案 | 研发与评审 |

---

## 4. 测试账号

| 角色 | 账号 | 密码 | 来源 |
|------|------|------|------|
| 平台管理员 | `admin` | `admin123` | `sql\03_test_data.sql`（V1 测试数据） |

其他账号（洗车工 / 门店 / 车主）以 `sql\03_test_data.sql` 实际数据为准。

---

## 5. 合规声明

1. **V1 为内部测试版（TEST-ENV-V1）**，用于功能验证与联调演示，**不得对外经营、不得向真实用户收费**。
2. **所有支付均为模拟支付**：仅产生测试流水与状态变更，**不产生真实资金收付**；前端成功文案固定为橙色「模拟支付成功」，严禁伪装成真实支付。
3. `03_test_data.sql` 中的账号与 `admin/admin123` 均为**模拟数据**，密码为示例明文，禁止带入生产。
4. **生产上架仍需**：持牌支付与分账、持牌保险合作、ICP 备案、微信小程序认证与类目资质、App 审核合规（隐私清单/权限用途/虚拟商品 IAP）、全站 HTTPS 与接口鉴权等安全基线。详见 `DEPLOY.md` 第 10 节与 `docs/架构说明.md`。
5. 禁止将本测试版直接部署到公网或接入真实支付渠道；违反上述约束产生的责任由部署方自行承担。
