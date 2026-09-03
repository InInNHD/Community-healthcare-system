# 社区健康云

社区医疗服务平台 2.0。项目已从 Spring Boot 2.0 / Java 8 / Shiro / Beetl / jQuery 单体页面，重构为前后端分离的现代架构。

## 技术架构

- 后端：Java 17、Spring Boot 4.1、Spring Security 7、OAuth2 Resource Server、JWT、Spring Data JPA、Flyway
- 前端：Vue 3、TypeScript、Vite 8、Vue Router、Element Plus、Axios
- 数据库：开发环境默认使用 H2 内存库；生产环境使用 MySQL 8 和 Flyway 版本化迁移
- 运维：Actuator 提供 `/actuator/health`、`/actuator/info` 和 `/actuator/metrics`

## 已实现模块

- 管理端、医护端、居民端三套独立门户
- 门户类型校验、JWT、ADMIN/DOCTOR/NURSE/PHARMACIST/REGISTRAR/RESIDENT 角色权限与业务档案归属校验
- 正式模式管理员/工作人员 TOTP 双因素认证，MFA 密钥使用 AES-GCM 加密保存
- 驾驶舱统计和近期预约
- 居民档案、医生团队、预约诊疗
- 健康监测、药品库存、慢病档案
- 医护工作台：今日预约处置、居民检索、健康数据登记、慢病分层、库存预警
- 居民门户：个人健康概览、自助预约/取消、指标上报、慢病计划和联系方式维护
- 统一分页、参数校验、错误响应、带删除人/删除时间审计的软删除和乐观锁
- 严格预约状态机、医生预约行级隔离、库存原子增减与慢病风险等级白名单
- JWT issuer/audience/kid 校验、密钥轮换、首次改密和改密后旧令牌即时失效
- MySQL 初始化迁移与本地演示数据

### R1—R5 建设结果

- R1 数据底座：中心/服务站/科室/人员组织模型、居民主索引与证件哈希脱敏、监护关系、授权与撤销、账号生命周期、岗位权限、站点/团队数据范围、MFA 和敏感读取审计。
- R2 门诊闭环：排班号源、原子占号与幂等预约、签到候诊、医生接诊、诊断、病历草稿/签署/更正版本链；已签署病历后才能完成预约。
- R3 药房收费：处方签署、药师审方、调配核对、按效期优先发药、不可变库存流水、账单、支付、退费及模拟医保申请。
- R4 医防融合：家庭医生团队、签约与服务包任务，高血压、2 型糖尿病、COPD、65 岁以上老年人分层和异常处置。
- R5 协同运营：双向转诊状态机、区域平台标准适配器与模拟实现、幂等重试/死信、居民档案开放、非诊断性留言、一次性评价和运营质控快照。
- 前端工作台按 ADMIN、DOCTOR、NURSE、PHARMACIST、REGISTRAR、RESIDENT 六类岗位呈现功能，并构建 `index.html`、`admin.html`、`staff.html`、`resident.html` 四个入口。

上述结果是模块化单体的首轮可运行闭环。公开 Demo 只能使用虚构数据。接入真实居民数据或开放临床高风险功能前，仍须完成本文“正式上线前门禁”中的部署与安全验收。

## 运行要求与模式选择

要求：JDK 17+、Maven 3.9+、Node.js 20.19+ 或 22.12+。项目提供三种运行方式，其中用于公开试用和正式上线的是 `demo`、`prod` 两种模式：

| 对比项 | Demo 公开试用模式 | Prod 正式模式 |
| --- | --- | --- |
| 后端 Profile | `demo` | `prod` |
| 数据库 | H2 内存库，后端停止后数据清空 | MySQL 8，Flyway 管理并持久化数据 |
| 初始化数据 | 自动创建虚构居民、医护、预约和六个共享账号 | 不创建演示数据或固定账号 |
| 登录页 | 显示并预填演示账号及密码 | 用户名、密码均为空，不包含演示密码 |
| 首次改密 | 共享演示账号可直接使用 | 正式账号必须按受控流程创建并执行首次改密策略 |
| JWT | 使用仅供本地试用的演示密钥 | 必须通过环境变量提供独立强密钥和 `kid` |
| 前端命令 | 本地 `npm run dev:demo`；部署包 `npm run build:demo` | 构建 `npm run build`，由 Nginx/IIS 等静态服务器提供 |
| 允许的数据 | 只能使用虚构、可公开数据 | 可承载正式数据，但必须完成数据库备份、TLS、审计和密钥管理 |

`dev` 仅供开发人员调试：同样使用 H2 和初始化账号，但会要求演示账号首次登录修改密码；前端使用 `npm run dev`，不会预填密码。

## Demo 公开试用模式

### Demo 本地启动

必须让前后端同时使用 Demo 模式，否则登录页预填行为和后端账号策略不一致。

首次运行前安装前端依赖：

```powershell
cd frontend
npm install
cd ..
```

打开两个 PowerShell 终端，在项目根目录分别执行：

```powershell
# 终端 1：启动 Demo 后端，端口 8080
.\run-backend-demo.ps1
```

脚本会自动选择 JDK 17+。也可以不用脚本，手动执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.4'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd -pl backend spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Demo 首次启动会先由 Hibernate 建立 JPA 基础表，再由 Flyway 将演示库标记为 V8 基线并自动执行 V9 及之后的业务迁移；启动日志应显示数据库已迁移到最新版本。无需手工导入 SQL。若迁移失败，应用会停止启动，不会以缺表状态继续提供接口。

第二个终端启动带账号密码预填的 Demo 前端：

```powershell
cd frontend
npm run dev:demo
```

浏览器可访问统一兼容入口 `http://localhost:5173/`，也可直接使用以下相互隔离的入口；独立入口只允许进入对应门户：

- 管理端：`http://localhost:5173/admin.html`
- 医护端：`http://localhost:5173/staff.html`
- 居民端：`http://localhost:5173/resident.html`

统一入口需先选择对应门户再登录：

| 门户 | 账号 | 密码 | 权限范围 |
| --- | --- | --- | --- |
| 管理端 | `admin` | `Admin@123456` | 全局运营、档案和系统管理 |
| 医护端（医生） | `doctor` | `Doctor@123456` | 预约处置、居民档案、健康监测和慢病服务 |
| 医护端（护士） | `nurse` | `Nurse@123456` | 护理记录、健康监测和居民服务 |
| 医护端（药师） | `pharmacist` | `Pharmacist@123456` | 处方审核、配药核对和发药 |
| 医护端（挂号收费） | `registrar` | `Registrar@123456` | 签到分诊、收费与结算 |
| 居民端 | `resident` | `Resident@123456` | 仅限本人资料、预约、健康记录和慢病计划 |

门户与账号角色不匹配时，后端会直接拒绝登录；仅修改前端地址或本地存储无法越过接口权限。

### Demo 部署构建

公开试用服务器需要预填演示密码时，后端 JAR 必须使用 `demo` Profile 启动，前端必须明确使用 `build:demo`：

```powershell
# 项目根目录：构建并启动 Demo 后端
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.4'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd -pl backend clean package
& "$env:JAVA_HOME\bin\java.exe" -jar .\backend\target\healthcare-backend-2.0.0-SNAPSHOT.jar --spring.profiles.active=demo

# 新终端：构建包含演示密码预填的前端
cd frontend
npm ci
npm run build:demo
```

将 `frontend/dist` 部署到静态服务器，并把 `/api` 反向代理到后端 8080 端口。Demo 部署只能使用隔离环境和虚构数据，不得保存真实居民信息，不得连接正式数据库，也不得复用正式 JWT 密钥。

## Prod 正式模式

正式模式不保留任何演示账号预填。创建 MySQL 8 数据库 `community_healthcare`，然后在项目根目录设置环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/community_healthcare?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='healthcare'
$env:DB_PASSWORD='replace-me'
$env:JWT_ACTIVE_KID='prod-2026-08'
$env:JWT_ACTIVE_SECRET='replace-with-a-random-secret-of-at-least-32-bytes'
$env:JWT_ISSUER='community-healthcare'
$env:JWT_AUDIENCE='community-healthcare-web'
$env:JWT_TTL='PT30M'
$env:MFA_ENCRYPTION_KEY='replace-with-an-independent-random-secret-of-at-least-32-bytes'
$env:MFA_BOOTSTRAP_ADMIN_SECRET='replace-with-a-base32-totp-secret-for-the-first-admin'
$env:PORTAL_ORGANIZATION_NAME='某某社区卫生服务中心'
$env:PORTAL_SERVICE_PHONE='010-12345678'
$env:PORTAL_SERVICE_HOURS='工作日 08:00-17:00'
$env:PORTAL_EMERGENCY_PHONE='120'
# 仅当前后端跨域部署时配置，多个来源使用逗号分隔；同源部署无需设置
$env:CORS_ALLOWED_ORIGINS='https://health.example.com'
```

### Prod 后端构建与启动

```powershell
# 构建并运行后端测试
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.4'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd -pl backend clean package

# 启动前检查当前终端中的数据库、JWT、JDK 和 JAR 配置
.\run-backend-prod.ps1 -CheckOnly

# 检查通过后启动正式后端，端口默认 8080
.\run-backend-prod.ps1
```

环境变量只在设置它们的 PowerShell 进程中有效，因此必须在设置 `DB_URL` 等变量的同一个终端中执行 Prod 启动脚本。若启动日志出现 `'url' must start with "jdbc"`，说明当前终端的 `DB_URL` 未设置或格式错误；正确格式必须以 `jdbc:mysql://` 开头。可以运行以下命令检查，但不要在日志或截图中输出数据库密码和 JWT 密钥：

```powershell
$env:DB_URL
.\run-backend-prod.ps1 -CheckOnly
```

### Prod 前端构建与启动

正式前端必须使用标准构建，不能使用 `build:demo`：

```powershell
cd frontend
npm ci
npm run build
```

生产环境不运行 Vite 开发服务器。应将 `frontend/dist` 部署到 Nginx、IIS 或其他静态服务器，并配置：

- 所有前端路由回退到 `index.html`。
- `/api` 反向代理到 Spring Boot 后端。
- 对外只开放 HTTPS，后端 8080 端口不直接暴露到公网。

仅在本机验收正式构建产物时，可以启动预览服务器；项目已为本地预览配置 `/api` 到 `http://localhost:8080` 的代理：

```powershell
# 启动 Prod 后端前，可将正式跨域白名单临时改为本机预览地址
$env:CORS_ALLOWED_ORIGINS='http://localhost:5173'

cd frontend
npm run preview
```

浏览器访问 `http://localhost:5173`。此时登录页不会显示或预填演示账号，且后端必须已按 `prod` Profile 启动。

正式构建会同时生成 `index.html`、`admin.html`、`staff.html`、`resident.html`。部署时应优先对外提供三个独立入口，`index.html` 仅保留兼容用途；前端入口隔离用于降低误入风险，最终权限仍由后端角色和数据归属校验强制执行。

正式环境不会自动创建任何固定密码账号，首批账号必须通过受控运维流程预置，并要求首次改密。管理员、医生、护士、药师和挂号员登录时必须完成 TOTP 动态验证码挑战；MFA 密钥应由受控账号开通流程写入加密字段，不会由登录接口返回或写入日志。生产表结构由 `backend/src/main/resources/db/migration` 中的 Flyway 脚本管理：V2 增加门户主体关联，V3/V5 增加密码生命周期与令牌版本，V4 统一临床软删除及审计字段，V6 增加机构、站点、工作人员、居民标识、监护关系、访问授权、审计和 MFA 字段。完整密钥轮换与配置要求见 [后端安全配置](backend/SECURITY.md)。

## 构建与测试

```powershell
mvn.cmd test
cd frontend
npm run build
# 公开试用包（包含演示密码预填）
npm run build:demo
```

前端产物位于 `frontend/dist`；`build` 与 `build:demo` 会写入同一目录，后执行的命令会覆盖前一个产物，部署前必须确认使用了正确模式。正式包已通过路由懒加载与依赖分块控制单块体积。本地开发和预览服务器会将 `/api` 与 `/actuator` 代理到 `http://localhost:8080`。整改映射、业务不变量与质量门禁见 [安全、质量与可维护性基线](docs/security-quality-review.md)。

## 正式上线前门禁

- 在可用 Docker 或 CI 环境执行 MySQL 8 Testcontainers 迁移、约束、索引和并发测试；本机无 Docker 时该测试会自动跳过，不能视作已通过。
- 当前访问令牌已使用生产 `HttpOnly + Secure + SameSite=Strict` Cookie，Cookie 会话写操作启用 CSRF 防护；正式承载真实敏感数据前仍需接入 Redis 可撤销刷新会话、设备会话和全端退出。
- 补齐 Playwright 三门户 E2E、axe 无障碍、OpenAPI 契约、ArchUnit 边界、依赖/基础安全扫描和关键列表性能测试。
- 完成正式 MySQL 备份恢复、Flyway 升级/回滚、密钥轮换、审计归档、网关公网/内网隔离及应急演练。
- 用当地正式接口规范与测试环境替换医保、区域平台、LIS/PACS 等模拟适配器，并通过幂等、对账和故障恢复验收。

## 旧系统与迁移

旧版源码、SQL 和原 Maven 配置完整保留在 `legacy/guns`，仅作为业务核对和数据迁移参照，不参与新版构建。字段映射与后续迁移建议见 [迁移说明](docs/migration.md)。
