# 线上洗车平台 v6.0 · Windows 部署手册（DEPLOY）

> 适用对象：研发/测试同学在 **Windows 本机** 搭建 V1 内部测试环境。
> 后端技术栈：JDK 8 + Maven 3.6+ + Spring 5 + SpringMVC 5 + MyBatis 3 + MySQL 8 + Tomcat 9（war 部署）。
> 工程根目录：下文统一记为 **`<工程根>`**，即 `一键部署.bat` 所在目录（默认 `D:\xiche\洗车平台SSM工程`）。
> **合规前置阅读**：本系统 V1 为**内部测试版**，模拟支付不产生真实资金收付，**不得对外经营**。详见文末第 10 节。

---

## 目录

1. [环境准备](#1-环境准备)
2. [快速开始：3 步跑起来](#2-快速开始3-步跑起来)
3. [数据库初始化：一键路径与手动路径](#3-数据库初始化一键路径与手动路径)
4. [修改 jdbc.properties](#4-修改-jdbcproperties)
5. [打包、部署与启动](#5-打包部署与启动)
6. [访问地址与测试账号](#6-访问地址与测试账号)
7. [三端（小程序 / App / H5）连接后端](#7-三端小程序--app--h5连接后端)
8. [四个 bat 脚本速查](#8-四个-bat-脚本速查)
9. [常见问题与排查命令](#9-常见问题与排查命令)
10. [合规声明与生产上架要求](#10-合规声明与生产上架要求)

---

## 1. 环境准备

### 1.1 版本矩阵（必须与工程一致）

| 组件 | 推荐版本 | 工程约束 | 下载地址 |
|------|----------|----------|----------|
| JDK | 1.8（8u202 及以上） | 父 `pom.xml` 中 `maven.compiler.source/target = 1.8`，**不要用 JDK 11/17 编译本工程** | https://adoptium.net/temurin/releases/?version=8 |
| Maven | 3.6.3 及以上 | 需支持 `maven-war-plugin 3.4.0` | https://maven.apache.org/download.cgi |
| Tomcat | 9.0.x（64-bit Windows zip） | Servlet 4.0.1；**不要用 Tomcat 10**（包名从 `javax` 改为 `jakarta`，会 ClassNotFound） | https://tomcat.apache.org/download-90.cgi |
| MySQL | 8.0.x | `mysql-connector-j 8.0.33`；驱动类 `com.mysql.cj.jdbc.Driver`；字符集 utf8mb4 | https://dev.mysql.com/downloads/installer/ |
| PowerShell | Windows 自带的 5.1+ | 一键脚本用它自动改写 `jdbc.properties`，无需额外安装 | 系统自带 |

### 1.2 JDK 8 安装要点

1. 运行安装包，记住安装目录，例如 `C:\Program Files\Java\jdk1.8.0_202`。
2. 新建系统变量 `JAVA_HOME` = 上述目录；在 `Path` 中追加 `%JAVA_HOME%\bin`。
3. 验证（打开**新的** cmd 窗口）：

```cmd
java -version
javac -version
echo %JAVA_HOME%
```

应显示 `1.8.x`。若显示 `java is not recognized`，说明 Path 未生效，重开 cmd 或注销重登。

### 1.3 Maven 安装要点

1. 下载 `apache-maven-3.8.8-bin.zip`，解压到无中文、无空格路径（如 `C:\dev\apache-maven-3.8.8`）。
2. 新建系统变量 `MAVEN_HOME` = 解压目录；`Path` 追加 `%MAVEN_HOME%\bin`。
3. 验证：`mvn -v`，应显示 Maven 版本与 `Java version: 1.8.x`。
4. **强烈建议**配置阿里云镜像加速（在 `%MAVEN_HOME%\conf\settings.xml` 的 `<mirrors>` 内加入），否则首次打包可能因下载依赖极慢：

```xml
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 1.4 Tomcat 9 安装要点

1. 下载 **64-bit Windows zip**（不是 `exe` Installer 版，zip 版更便于脚本控制）。
2. 解压到 `C:\Program Files\apache-tomcat-9.0.85`（目录名不含中文）。该目录即 `CATALINA_HOME`。
3. 验证：双击 `%CATALINA_HOME%\bin\startup.bat`，浏览器访问 `http://localhost:8080/` 应看到 Tomcat 欢迎页；随后运行 `shutdown.bat` 关闭。
4. 如 8080 被占用，编辑 `%CATALINA_HOME%\conf\server.xml`，把 `<Connector port="8080" ...>` 的 port 改为 8081 等，并同步修改本文所有访问地址。

### 1.5 MySQL 8 安装要点

1. 用 MySQL Installer 选择 **Server only** 或 Custom 安装 MySQL Server 8.0。
2. 安装过程中会设置 **root 密码**——请牢记，它要填入脚本顶部的 `DB_ROOT_PWD`。
3. 认证插件保持默认 `caching_sha2_password` 即可：工程使用 Connector/J 8.0.33，且 JDBC URL 已带 `allowPublicKeyRetrieval=true`，兼容该插件。
4. 把 MySQL 的 `bin` 目录加入 `Path`：`C:\Program Files\MySQL\MySQL Server 8.0\bin`，验证：

```cmd
mysql --version
```

5. 确认服务已启动：

```cmd
net start MySQL80
```

若提示服务名无效，用 `services.msc` 查找实际服务名（可能是 `MySQL` 或 `MySQL80X`）。

### 1.6 工程目录结构（与脚本对应关系）

```
<工程根>\
├── pom.xml                        父工程：5 个 Maven 模块
├── xiche-common\                  公共常量/工具/统一响应
├── xiche-pojo\                    实体/DTO/VO
├── xiche-dao\                     MyBatis Mapper 接口与 XML
├── xiche-service\                 业务服务（14 个业务域）
├── xiche-web\                     Controller/配置/JSP，最终打 war
│   └── src\main\resources\jdbc.properties   ← 数据库连接配置（第 4 节改这里）
├── sql\
│   ├── 01_schema.sql              建表结构
│   ├── 02_init_data.sql           初始化基础数据（角色/权限/字典）
│   └── 03_test_data.sql           V1 测试数据（含测试账号，禁止用于生产）
├── clients\                       三端源码（小程序 / App / H5）
├── docs\                          模块清单.md、架构说明.md
├── 一键部署.bat                    ★ 主脚本：检测→初始化→打包→部署→启动
├── 一键启动.bat                    仅启动 Tomcat
├── 一键停止.bat                    仅停止 Tomcat
├── 数据库初始化.bat                仅建库 + 导入三个 SQL
├── README.md                      工程总入口
└── DEPLOY.md                      本文档
```

---

## 2. 快速开始：3 步跑起来

> 前提：第 1 节的 JDK/Maven/Tomcat/MySQL 已装好并能启动。

**第 1 步**：用记事本打开 `<工程根>\一键部署.bat`，修改顶部「变量配置区」的 4 处路径与 2 个密码：

```
JAVA_HOME      = 你的 JDK8 目录
MAVEN_HOME     = 你的 Maven 目录
CATALINA_HOME  = 你的 Tomcat 9 解压目录
MYSQL_HOME     = 你的 MySQL 8 安装目录
DB_ROOT_PWD    = 你安装 MySQL 时设置的 root 密码
DB_PWD         = 给应用账号 xiche 设置的密码（默认 Xiche@2024，可改）
```

**第 2 步**：右键 `一键部署.bat` → **以管理员身份运行**。脚本会依次完成 7 个步骤（详见第 8 节），任何一步失败都会**明确报错并暂停**，不会静默跳过。

**第 3 步**：看到 `[7/7] 部署完成！` 后，等待 10~60 秒（首次解压 war + Spring 初始化），浏览器访问 `http://localhost:8080/`，用 `admin / admin123` 登录验证。

> 若只想重新部署代码、不动数据库：后续迭代改完 Java 后重跑 `一键部署.bat` 即可（它默认会重新导入 SQL，幂等脚本不会重复插入；如 SQL 非幂等，请把脚本顶部 `SKIP_DB_INIT` 改为 `1`）。

---

## 3. 数据库初始化：一键路径与手动路径

### 3.1 路径 A（推荐）：一键初始化

直接运行 `数据库初始化.bat`（或以管理员运行 `一键部署.bat`，它内含相同步骤）。脚本做四件事：

1. 找到 `mysql.exe`（优先 `MYSQL_HOME\bin\mysql.exe`，否则从 PATH 探测）；
2. 用 root 连接，执行 `CREATE DATABASE IF NOT EXISTS`、`CREATE USER IF NOT EXISTS`、`GRANT ALL PRIVILEGES`、`FLUSH PRIVILEGES`；
3. 依次导入 `01_schema.sql`、`02_init_data.sql`、`03_test_data.sql`；
4. 任一步失败都打印 `[初始化失败]` 并暂停。

**安全开关**：`DB_RESET=1` 会先 `DROP DATABASE`，**清空全部数据**，仅在需要彻底重建时使用；默认 `0` 保留已有库。

### 3.2 路径 B（手动初始化）：完全用命令行

适合一键脚本因权限/环境问题失败时逐步排障。**在 `<工程根>` 目录打开 cmd**：

**B1. 用 root 登录并建库建用户**

```cmd
mysql -u root -p
```

输入 root 密码后，在 `mysql>` 提示符执行：

```sql
CREATE DATABASE IF NOT EXISTS `xiche` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'xiche'@'%' IDENTIFIED BY 'Xiche@2024';
GRANT ALL PRIVILEGES ON `xiche`.* TO 'xiche'@'%';
FLUSH PRIVILEGES;
EXIT;
```

**B2. 用应用账号导入三个 SQL**

```cmd
mysql -u xiche -pXiche@2024 xiche < sql\01_schema.sql
mysql -u xiche -pXiche@2024 xiche < sql\02_init_data.sql
mysql -u xiche -pXiche@2024 xiche < sql\03_test_data.sql
```

> 说明：`-p` 后紧跟密码（中间无空格）。密码含空格或特殊字符时，改用交互方式 `mysql -u xiche -p xiche < sql\01_schema.sql` 再手工输密码。
> 若 SQL 脚本内部已带 `USE xiche;`，命令中的库名参数 `xiche` 仍保留，二者不冲突。

**B3. 验证导入结果**

```cmd
mysql -u xiche -pXiche@2024 -e "USE xiche; SHOW TABLES; SELECT COUNT(1) FROM sys_user;"
```

应能看到表列表与用户记录数。

---

## 4. 修改 jdbc.properties

一键部署脚本会自动改写该文件；手动部署或自动改写失败时，请编辑：

**`<工程根>\xiche-web\src\main\resources\jdbc.properties`**

关键项设为（其余连接池参数保持原样）：

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://127.0.0.1:3306/xiche?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
jdbc.username=xiche
jdbc.password=Xiche@2024
```

**逐项说明**：

| 参数 | 说明 |
|------|------|
| `127.0.0.1:3306` | 数据库地址与端口；若 MySQL 在另一台机器，改成该机 IP |
| `/xiche` | 库名，必须与第 3 节建库名一致 |
| `useUnicode=true&characterEncoding=utf8` | 防止中文乱码 |
| `serverTimezone=Asia/Shanghai` | MySQL 8 驱动必需，否则报时区错误 |
| `useSSL=false` | 本地测试关闭 SSL |
| `allowPublicKeyRetrieval=true` | 兼容 `caching_sha2_password` 认证插件，缺此项会报 Public Key Retrieval is not allowed |

> Java 后端**不会**在 war 运行时读取源码目录的 properties——它在打包时被复制进 war。所以**改完 jdbc.properties 必须重新执行 `mvn clean package`**（直接跑 `一键部署.bat` 即可）。

---

## 5. 打包、部署与启动

### 5.1 一键方式（推荐）

运行 `一键部署.bat`，它会自动完成 `mvn clean package -DskipTests` 并把 war 复制为 `%CATALINA_HOME%\webapps\ROOT.war`，最后启动 Tomcat。

### 5.2 手动方式（便于理解每一步）

在 `<工程根>` 打开 cmd：

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
call mvn clean package -DskipTests
```

产物：`<工程根>\xiche-web\target\xiche-web-1.0.0.war`

部署为根应用（访问 `http://localhost:8080/`）：

```cmd
if exist "%CATALINA_HOME%\webapps\ROOT" rd /s /q "%CATALINA_HOME%\webapps\ROOT"
copy /y "xiche-web\target\xiche-web-1.0.0.war" "%CATALINA_HOME%\webapps\ROOT.war"
call "%CATALINA_HOME%\bin\startup.bat"
```

若希望带上下文路径访问（`http://localhost:8080/xiche-web/`），把目标文件名改为 `xiche-web.war` 即可，同时把 `一键部署.bat` 顶部的 `DEPLOY_AS_ROOT` 改为 `0`。

### 5.3 启动与停止

| 操作 | 脚本 / 命令 |
|------|-------------|
| 启动 | 双击 `一键启动.bat`（或 `%CATALINA_HOME%\bin\startup.bat`） |
| 前台看日志启动 | `"%CATALINA_HOME%\bin\catalina.bat" run` |
| 停止 | 双击 `一键停止.bat`（或 `%CATALINA_HOME%\bin\shutdown.bat`） |
| 看日志 | `type "%CATALINA_HOME%\logs\catalina.out"` 或用编辑器打开 |

**启动成功的标志**：`catalina.out` 出现 `Server startup in [xxxx] milliseconds`，且不再刷 `ERROR`。

---

## 6. 访问地址与测试账号

| 项 | 值 |
|----|----|
| 后端根地址 | `http://localhost:8080/`（部署为 ROOT 时） |
| 带上下文路径 | `http://localhost:8080/xiche-web/`（`DEPLOY_AS_ROOT=0` 时） |
| API 前缀 | `/api/`（例：`http://localhost:8080/api/...`） |
| 后台管理端 | `http://localhost:8080/`（登录后进入管理控制台） |
| 健康检查 | `http://localhost:8080/api/health`（以后端实际实现为准） |

**测试账号（来自 `sql\03_test_data.sql`，V1 内部测试数据）**：

| 角色 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 平台管理员 | `admin` | `admin123` | 全量后台权限，用于验收 |
| 洗车工 / 门店等 | 见 `03_test_data.sql` | 见脚本 | 以实际初始化数据为准 |

> **严禁**把这些账号和密码带到生产环境。它们是**模拟数据**，密码为明文示例，仅用于本地联调与功能验证。

---

## 7. 三端（小程序 / App / H5）连接后端

### 7.1 通用规则

后端 API 统一前缀为 `/api/`，响应体统一为 `{ "code": 0, "msg": "...", "data": {...} }`（`code` 为 `0` 或 `200` 表示成功）。三端只需把 **baseURL** 指向后端根地址，接口路径继续按各端封装拼接。

> **关键提醒**：手机/小程序**不能**用 `localhost` 访问你的电脑。请先用 `ipconfig` 查本机局域网 IPv4（如 `192.168.1.23`），把下面所有 `<本机IP>` 替换为它，并确保 Windows 防火墙放行 8080 端口。

```cmd
ipconfig | findstr /C:"IPv4"
```

防火墙放行（管理员 cmd）：

```cmd
netsh advfirewall firewall add rule name="xiche-8080" dir=in action=allow protocol=TCP localport=8080
```

### 7.2 小程序（`clients/miniprogram/`）

当前默认 `BASE_URL = '/api/'`（相对路径）。联调时改两处为绝对地址：

1. `clients/miniprogram/utils/constants.js`：

```js
// const BASE_URL = '/api/';
const BASE_URL = 'http://<本机IP>:8080/api/';
```

2. `clients/miniprogram/app.js` 的 `globalData`：

```js
baseURL: 'http://<本机IP>:8080/api/',
```

`utils/api.js` 会优先取 `app.globalData.baseURL`，回退到 `constants.BASE_URL`，所以两处保持一致即可。

**小程序特有约束**：
- 微信开发者工具中勾选「详情 → 本地设置 → 不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」，否则 `http://` 请求会被拦截。
- **真机预览/体验版**同样要求后台配置 request 合法域名，且**必须 HTTPS + 备案域名**。V1 只能在开发环境用局域网 HTTP 联调，**不得**用这套配置对外发布。

### 7.3 App（`clients/app/`）

在 App 工程的全局配置（常见为 `clients/app/common/config.js` 或等价文件，以实际工程为准）中把 API 根地址改为：

```js
export const BASE_URL = 'http://<本机IP>:8080/api/';
```

**App 特有约束**：
- Android 9+ 默认禁止明文 HTTP，需在 `AndroidManifest.xml` 的 `<application>` 上加 `android:usesCleartextTraffic="true"`（仅测试版），或配置 `network_security_config` 放行该 IP。
- iOS 需在 `Info.plist` 配置 `NSAppTransportSecurity` 的 `NSAllowsArbitraryLoads`（仅测试版，上架必须移除并改 HTTPS）。

### 7.4 H5（`clients/h5/`）

在 `clients/h5/js/` 下的 API 配置文件（如 `api.js` / `config.js`，以实际工程为准）中设置：

```js
const BASE_URL = 'http://<本机IP>:8080/api/';
```

若 H5 与后端不同源，需后端在 SpringMVC 配置 CORS 放行该来源（开发期），或开发服务器配置代理。

### 7.5 后台管理端 Web

后台页面位于 `xiche-web/src/main/webapp/WEB-INF/views/admin/`，与后端**同源部署**，API 使用相对路径 `/api/`，无需修改 baseURL。

---

## 8. 四个 bat 脚本速查

| 脚本 | 作用 | 何时用 |
|------|------|--------|
| `一键部署.bat` | 7 步全流程：环境检测 → 建库授权 → 导入 3 个 SQL → 改写 jdbc.properties → `mvn clean package` → 复制 war → 启动 Tomcat | 首次部署 / 后端代码改完要重新出包 |
| `一键启动.bat` | 仅启动 Tomcat | 开发迭代，war 已部署好 |
| `一键停止.bat` | 仅停止 Tomcat | 改代码前先停服 / 释放 8080 端口 |
| `数据库初始化.bat` | 仅建库 + 导入 3 个 SQL | 只想重置数据、不动代码 |

**变量区位置**：每个脚本顶部都有醒目的「变量配置区」，所有路径和密码集中在那里，改完保存即可。**四个脚本的 `CATALINA_HOME` / 数据库密码要保持一致**。

**编码处理**：脚本第二行 `chcp 65001` 切换到 UTF-8 代码页，文件本身请保存为 **UTF-8 编码**（用 VS Code / Notepad++ 编辑，不要用 Windows 记事本另存为 ANSI）。

---

## 9. 常见问题与排查命令

### 9.1 端口 8080 被占用

**现象**：Tomcat 启动失败，`catalina.out` 出现 `Address already in use: bind`。

```cmd
netstat -ano | findstr :8080
taskkill /PID <查到的PID> /F
```

或改用 `一键停止.bat`，或修改 `%CATALINA_HOME%\conf\server.xml` 的 `<Connector port="8080">`。

### 9.2 中文乱码

**现象**：bat 输出乱码、页面/数据库中文变问号。

- bat 窗口乱码：确认脚本第二行 `chcp 65001 >nul`，且 bat 文件保存为 UTF-8；
- 数据库乱码：确认库字符集 `utf8mb4`，JDBC URL 含 `characterEncoding=utf8`；
- 页面乱码：在 `%CATALINA_HOME%\conf\server.xml` 的 Connector 上加 `URIEncoding="UTF-8"`；
- 检查库字符集：

```cmd
mysql -u root -p -e "SHOW CREATE DATABASE xiche\G"
```

### 9.3 mysql 不在 PATH

**现象**：`'mysql' 不是内部或外部命令`。

- 脚本顶部把 `MYSQL_HOME` 设为 `C:\Program Files\MySQL\MySQL Server 8.0`；
- 或把 `C:\Program Files\MySQL\MySQL Server 8.0\bin` 加到系统 `Path`，**重开 cmd** 后验证 `mysql --version`。

### 9.4 war 部署慢 / 一直转圈

**现象**：访问长时间无响应。

- 首次部署 war 需解压 + Spring 容器初始化，通常 10~60 秒，属正常；
- 观察日志：`type "%CATALINA_HOME%\logs\catalina.out"`；
- 若日志刷数据库连接异常，回到第 4 节核对 JDBC URL（尤其 `serverTimezone`、`allowPublicKeyRetrieval`）；
- 若出现 `Deployment of web application archive ... has finished in [xxxx] ms` 即部署成功。

### 9.5 权限不足

**现象**：`copy` 失败 / `Access is denied` / 无法删除 `webapps\ROOT`。

- **以管理员身份运行** bat；
- 确认 Tomcat 已停止（先跑 `一键停止.bat`），否则 `ROOT` 目录被 java.exe 占用：

```cmd
tasklist | findstr java
taskkill /PID <java进程PID> /F
```

### 9.6 Maven 打包失败

**现象**：`BUILD FAILURE` / 依赖下载超时。

- 确认 `java -version` 是 1.8（JDK 11/17 会导致编译目标不兼容）；
- 按 1.3 节配置阿里云镜像；
- 看具体 `ERROR` 行定位缺失依赖，必要时删除 `~/.m2/repository` 中损坏的目录后重试。

### 9.7 数据库连不上 / 账号被拒

| 报错 | 原因 | 处理 |
|------|------|------|
| `Communications link failure` | MySQL 未启动 / 端口错 / 防火墙 | `net start MySQL80`，核对端口 |
| `Access denied for user 'xiche'@'localhost'` | 用户未创建或密码错 | 重跑 `数据库初始化.bat`，核对 `DB_PWD` 与 jdbc.properties 一致 |
| `Public Key Retrieval is not allowed` | 未加该参数 | JDBC URL 补 `allowPublicKeyRetrieval=true` |
| `Unknown database 'xiche'` | 库名不一致 | 重跑初始化，或统一 `DB_NAME` 与 jdbc.properties |

### 9.8 PowerShell 改写 jdbc.properties 失败

**现象**：`[错误] jdbc.properties 自动改写失败`。

- 确认系统有 PowerShell 5.1（Win7 SP1+ 自带）；
- 或按第 4 节**手工编辑**该文件，再把 `mvn clean package` 重跑（直接重跑 `一键部署.bat` 并把 `SKIP_DB_INIT` 设为 `1` 更快）。

### 9.9 小程序请求被拦截

**现象**：开发者工具报 `url not in domain list`。

- 勾选「不校验合法域名」；真机预览必须配置 HTTPS 合法域名（见 7.2）。

---

## 10. 合规声明与生产上架要求

> **务必逐条阅读，V1 不是可经营的生产系统。**

1. **V1 定位**：本系统 V1 为**内部测试版（TEST-ENV-V1）**，用于功能验证与联调演示。
2. **模拟支付**：所有支付均为**模拟支付**，仅产生测试流水与状态变更，**不产生真实资金收付**；前端支付成功文案固定为橙色「模拟支付成功」，严禁伪装成真实支付成功。**不得对外经营、不得向真实用户收费**。
3. **测试数据**：`sql\03_test_data.sql` 与 `admin/admin123` 等账号均为模拟数据，密码为示例明文，禁止用于生产。
4. **生产上线仍需完成**（不在 V1 交付范围内）：
   - **支付与分账**：接入持牌支付机构，分账/结算须由持牌主体完成，回调须幂等；
   - **保险**：如提供保障服务，须与持牌保险机构合作并完成合规公示；
   - **ICP 备案**：对外提供服务的域名须完成 ICP 备案；
   - **小程序认证与类目资质**：微信小程序须完成主体认证，服务类目须与洗车/生活服务资质匹配；涉及支付的类目须提交对应资质；
   - **App 上架**：iOS 须遵守 App Store 审核规则（含虚拟商品 IAP、隐私清单、权限用途文案），Android 须适配厂商通道与隐私合规；
   - **安全基线**：全站 HTTPS、密码哈希存储、接口鉴权、敏感数据加密、日志审计，详见 `docs/架构说明.md`。
5. **禁止行为**：不得将本测试版部署到公网、不得接入真实支付渠道、不得用于真实商业交易；违反上述约束产生的责任由部署方自行承担。

---

**文档版本**：v1.0（对应平台 v6.0 / 工程 V1 测试版）
**相关文档**：`README.md`（总入口） · `docs/模块清单.md` · `docs/架构说明.md`
