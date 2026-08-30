# 安全、质量与可维护性整改基线

本文档以当前 Spring Boot + Vue 重构版本为准。`legacy/guns` 仅用于历史业务与迁移核对，不参与构建，也不应继续在旧 Shiro/Beetl 代码上修补。

## 静态审阅问题整改映射

| 原问题 | 当前处理 | 维护约束 |
| --- | --- | --- |
| Spring Boot 2.0、Shiro、JJWT、Beetl 等旧依赖 | 运行时已替换为 Java 17、Spring Boot 4.1、Spring Security 7、OAuth2 Resource Server、Vue 3 和 Vite 8 | 依赖只在 `backend/pom.xml` 与 `frontend/package.json` 维护，旧依赖不得重新加入主构建 |
| MD5 与固定初始密码 | 使用可配置 cost 的 BCrypt；首次密码带 `mustChangePassword`，改密后旧 JWT 立即失效 | 固定演示账号只允许 `dev/demo/test`；`prod` 启用 bootstrap 会启动失败 |
| JWT 密钥只依赖单一环境变量 | 强类型校验 issuer、audience、TTL、`kid` 和至少 32 字节密钥；支持活动签名密钥与多把历史验证密钥轮换 | `prod` 禁止 demo kid/secret，未知 kid、错误 issuer/audience 均拒绝 |
| `${orderByField}` 动态 SQL | 新代码全部使用 Spring Data 参数绑定；分页、大小和排序由服务端白名单确定 | 页码必须非负，每页 1–100，客户端不能传入 SQL 字段或排序表达式 |
| 门户 Controller 过重 | 医护端和居民端拆为 Controller、只读查询服务、事务命令服务、DTO、批量映射器和领域策略 | Controller 只处理协议与身份声明；状态机、库存、归属校验必须放在应用/领域服务 |
| 姓名与 ID 混用 | 预约、健康记录、慢病档案、账号主体均以 ID 和数据库外键关联；姓名只在响应阶段批量映射 | 权限判断和写入不得使用姓名；姓名变更不改变业务归属 |
| `paient/pateint` 拼写固化 | 新领域统一使用 `Patient`、`patientId`、`patient` 表 | 旧拼写仅可出现在迁移说明或 legacy 目录 |
| 软删除不一致 | 六类临床实体统一 `active`、Hibernate `SQLRestriction`、`deletedAt/deletedBy` | DELETE 只执行逻辑删除；普通仓库查询必须自动隐藏已删除记录 |
| N+1 与内存聚合 | 门户分页结果按 ID 集合批量加载显示信息；库存预警及统计在数据库执行 | 禁止在列表映射循环中逐条 `findById`；新增聚合优先仓库投影/数据库分页 |
| 医疗业务测试不足 | 建立门户认证、密码生命周期、医生/居民行级隔离、预约状态机、健康归属、风险白名单、库存原子性、低库存和软删除集成测试 | 修改上述规则必须同时更新测试，不得以放宽断言方式绕过失败 |

## 关键业务不变量

### 预约

- 创建时固定为 `PENDING`，普通 PUT 不得覆盖状态。
- 只允许 `PENDING → CONFIRMED → COMPLETED`。
- `PENDING`、`CONFIRMED` 可以转为 `CANCELLED`。
- `COMPLETED`、`CANCELLED` 为终态，不允许回退或互转。
- 居民只能查看、取消自己的预约；医生只能查询和处置 `doctorId = JWT.staffId` 的预约；护士按社区团队岗位保留全量视图。

### 库存

- 新建药品可设置初始库存。
- 存量库存只能通过 `PATCH /api/medicines/{id}/stock` 和整数 `delta` 调整。
- 普通 PUT 不得覆盖库存。
- 调整操作使用事务和行级悲观锁，结果不得小于 0，整数溢出必须回滚。

### 密码与令牌

- 密码策略由 `GET /api/auth/password-policy` 提供，前端不得复制一套不可配置规则。
- 首次改密 JWT 只允许访问 `/api/auth/me`、`/api/auth/password-policy` 和 `/api/auth/change-password`。
- 改密成功后客户端必须清理旧令牌并重新登录。
- 密钥轮换操作见 [后端安全配置](../backend/SECURITY.md)。

## 发布模式

| 模式 | 后端 Profile | 前端命令 | 固定演示账号 | 首次改密 |
| --- | --- | --- | --- | --- |
| 日常开发 | `dev` | `npm run dev` | 允许 | 强制 |
| 公开试用 | `demo` | `npm run dev:demo` / `npm run build:demo` | 允许并预填 | 不强制，避免公共账号被首位用户改掉 |
| 正式生产 | `prod` | `npm run build` | 禁止 | 由正式账号创建流程决定 |

`demo` 是公开试用模式，不得保存真实居民数据，不得复用生产数据库、JWT 密钥或域名 Cookie。正式生产包默认不包含演示密码预填逻辑。

## 质量门禁

```powershell
# 后端编译与全部自动化测试
mvn -pl backend test

# 前端类型检查与正式构建
cd frontend
npm run typecheck
npm run build

# 试用包构建（包含演示账号预填）
npm run build:demo
```

合并前还应执行 `git diff --check`。任何 Flyway 脚本一旦在共享环境执行，禁止修改原文件，应追加更高版本迁移。
