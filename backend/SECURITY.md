# 后端安全配置

## 运行模式

默认配置不会创建任何固定密码账号。仅在显式启用 `dev` 或 `demo` profile 时，才会创建演示数据和以下账号：

- `admin / Admin@123456`
- `doctor / Doctor@123456`
- `nurse / Nurse@123456`
- `resident / Resident@123456`

例如：

```powershell
mvn -pl backend spring-boot:run "-Dspring-boot.run.profiles=demo"
```

共享试用所使用的 `demo` profile 将 `mustChangePassword` 设为 `false`，避免首位试用者改掉
公共预填密码；该模式不得用于生产。`dev` profile 默认为 `true`：在完成改密前，JWT 只能调用
`/api/auth/me` 和 `/api/auth/change-password`，访问业务接口会得到
`403 PASSWORD_CHANGE_REQUIRED`。固定密码初始化在非 `dev/demo/test` profile 下会导致启动失败，
生产环境无条件禁止启用。

## 密码生命周期

密码使用 BCrypt（默认 cost 12）。新密码默认要求 12–128 个字符，并同时包含大写字母、
小写字母、数字和特殊字符，且不能包含空白、不能与当前密码相同。由于 BCrypt 只处理
前 72 字节，服务端会额外拒绝 UTF-8 编码超过 72 字节的密码，避免不同长密码被截断为
等价凭据；客户端可从密码策略接口的 `maxUtf8Bytes` 获取该边界。
客户端可匿名读取 `GET /api/auth/password-policy` 获取当前 min/max 和四类字符要求，
不需要在界面中硬编码策略。

已认证改密接口同时支持 `POST` 和 `PUT`：

```http
POST /api/auth/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "Admin@123456",
  "newPassword": "A-new-strong-password-2026!"
}
```

成功后返回 `reauthenticationRequired: true`。服务端会更新 `passwordChangedAt`、递增单调的
`passwordVersion` 并清除 `mustChangePassword`。JWT 携带签发时的 `pwdVersion`，验证端要求它与
数据库精确相等，因此即使登录与改密发生在同一秒，旧令牌也会立即失效，客户端必须重新登录。

## JWT 生产配置

JWT 配置绑定到强类型的 `app.security.jwt`，启动时会检查：

- issuer、audience 非空且不含空白；
- TTL 在 5 分钟至 24 小时之间；
- 每个 `kid` 唯一；
- 每把 HS256 密钥至少 32 字节；
- 生产环境不得使用演示 `kid` 或默认密钥。

生产启动至少需要：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:JWT_ACTIVE_KID = 'prod-2026-08'
$env:JWT_ACTIVE_SECRET = '<至少 32 字节的随机密钥>'
$env:JWT_ISSUER = 'community-healthcare'
$env:JWT_AUDIENCE = 'community-healthcare-web'
$env:JWT_TTL = 'PT30M'
$env:MFA_ENCRYPTION_KEY = '<独立的至少 32 字节随机密钥>'
$env:MFA_BOOTSTRAP_ADMIN_SECRET = '<首个管理员使用的 Base32 TOTP 种子>'
$env:DB_URL = 'jdbc:mysql://db.example:3306/community_healthcare?serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = '<独立的最小权限数据库账号>'
$env:DB_PASSWORD = '<非空数据库密码>'
```

可使用密码学安全随机数生成密钥，例如生成 48 个随机字节并以 Base64 保存：

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

不要把生产密钥提交到仓库。

## 工作人员 MFA

`prod` profile 强制启用 TOTP 双因素认证，覆盖 `ADMIN`、`DOCTOR`、`NURSE`、
`PHARMACIST` 和 `REGISTRAR`，居民账号不在本阶段强制范围。密码认证通过后，
`POST /api/auth/login` 只返回一个有效期 5 分钟、一次性消费的挑战，不签发 JWT；客户端再将
身份验证器中的 6 位动态验证码提交到 `POST /api/auth/mfa/verify`，验证成功后才获得访问令牌。
`demo` profile 关闭 MFA，以保留公开试用账号的密码预填和直接登录体验。

TOTP 种子使用独立的 `MFA_ENCRYPTION_KEY` 通过 AES-GCM 加密后保存在
`app_user.mfa_secret_ciphertext`，不会通过认证响应返回，也不得写入日志。首次开通种子应通过管理员专用的
`POST /api/v1/admin/accounts/{username}/mfa/provision` 完成；接口仅在创建时返回一次种子和
`otpauth://` URI，并写入强制审计。生产启动要求数据库已有受控创建的 `admin` 账号，并显式配置
`MFA_BOOTSTRAP_ADMIN_SECRET` 完成首个管理员 MFA 初始化；缺少账号或种子都会拒绝启动，避免锁死。
工作人员账号未配置种子时登录会被拒绝，而不会降级为单因素认证。

## JWT 密钥轮换

所有新令牌在 JOSE header 中携带活动密钥的 `kid`。验证端同时接受活动密钥和
`verification-keys` 中的旧密钥，轮换流程如下：

1. 将当前活动密钥作为验证密钥保留；
2. 配置新的 `JWT_ACTIVE_KID` 和 `JWT_ACTIVE_SECRET` 并发布；
3. 等待至少一个“最大 JWT TTL + 时钟偏差”周期；
4. 删除旧验证密钥并再次发布。

旧验证密钥可通过 Spring Boot 的索引环境变量提供：

```powershell
$env:APP_SECURITY_JWT_VERIFICATION_KEYS_0_KID = 'prod-2026-07'
$env:APP_SECURITY_JWT_VERIFICATION_KEYS_0_SECRET = '<上一把密钥>'
```

需要并行保留多把旧密钥时继续使用索引 `1`、`2`。未知或缺失 `kid`、签名错误、
issuer/audience 不匹配以及 `pwdVersion` 与账号当前版本不匹配的令牌都会被拒绝。

## CORS 与数据库凭据

开发配置仅允许 `localhost` 和 `127.0.0.1` 的端口来源。生产环境默认不允许跨域，
同源部署无需额外配置；前后端跨域部署时必须显式提供逗号分隔的来源白名单，例如：

```powershell
$env:CORS_ALLOWED_ORIGINS = 'https://health.example.com,https://staff.example.com'
```

启用凭据时不接受 `*` 来源。`prod` profile 的 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
均无仓库默认值，任何一个缺失都会导致配置解析或数据源初始化失败。
