# 后端源码高可读性注释实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `backend/src/main` 的生产源码、配置和 Flyway 迁移增加准确、统一、可长期维护的简体中文注释，同时保持运行行为完全不变。

**Architecture:** 以业务模块和分层包为单位补充 `package-info.java`，再为顶层类型、公共业务方法及非显然规则增加 Javadoc/行内注释。资源文件按配置域和迁移阶段说明意图；每批通过编译与差异审查阻止语义变化。

**Tech Stack:** Java 25、Spring Boot 4.1、Spring Security、Spring Data JPA、Flyway、YAML、MySQL SQL、Maven。

**Spec:** `docs/superpowers/specs/2026-08-23-backend-source-commenting-design.md`

## Global Constraints

- 仅修改 `backend/src/main`；不修改 `backend/src/test`、前端或旧系统。
- 注释使用简体中文，JWT、MFA、CSRF、DTO、JPA、Flyway、幂等键等术语保留英文。
- 现有文件只允许注释变化；新增 Java 文件只允许是 `package-info.java`。
- 不改变方法签名、注解、配置值、SQL 内容或语句顺序。
- 不添加敏感值、作者日期模板、无责任人的 TODO 或复述语法的低价值注释。
- 当前重构后的 `backend/` 尚未整体纳入 Git；执行期间不得提交这些源码，避免把用户的既有重构一并纳入提交。

---

### Task 1: 建立模块与分层包说明

**Files:**
- Create: `backend/src/main/java/com/community/healthcare/package-info.java`
- Create: every missing `package-info.java` in the 37 existing subpackages under `audit/application`, `audit/infrastructure`, `billing/api`, `billing/domain`, `clinical`, `config`, `encounter/domain`, `familydoctor/domain`, `familydoctor/infrastructure`, `identityorg/api`, `identityorg/application`, `identityorg/domain`, `identityorg/infrastructure`, `insurance/domain`, `integration`, `inventory/domain`, `notification`, `pharmacy/api`, `pharmacy/domain`, `pharmacy/infrastructure`, `portal`, `publichealth/domain`, `publichealth/infrastructure`, `quality`, `referral/api`, `referral/application`, `referral/domain`, `residentregistry/api`, `residentregistry/application`, `residentregistry/domain`, `residentregistry/infrastructure`, `scheduling/application`, `scheduling/domain`, `scheduling/infrastructure`, `security`, `shared/api`, and `shared/domain`.

**Interfaces:**
- Consumes: existing Java package boundaries.
- Produces: package-level Javadoc discoverable by IDE and generated documentation; no runtime types.

- [ ] **Step 1: Record the existing package set**

Run:

```powershell
Get-ChildItem backend/src/main/java/com/community/healthcare -Recurse -Filter *.java |
  ForEach-Object DirectoryName | Sort-Object -Unique
```

Expected: 38 packages including the root package.

- [ ] **Step 2: Add package documentation**

Each file follows this semantic pattern, specialized for its module and layer:

```java
/**
 * 提供居民主索引的应用服务与访问策略。
 *
 * <p>本包编排居民登记、身份校验和数据访问范围；持久化细节由 infrastructure 层实现，
 * 门户层不得绕过这里的权限策略直接读取居民数据。</p>
 */
package com.community.healthcare.residentregistry.application;
```

- [ ] **Step 3: Verify package coverage and compile**

Run: `mvn.cmd -pl backend -DskipTests compile`

Expected: `BUILD SUCCESS` and every package containing production Java has a `package-info.java`.

### Task 2: 注释基础设施、安全与组织边界

**Files:**
- Modify: `backend/src/main/java/com/community/healthcare/HealthcareApplication.java`
- Modify: all Java files under `shared`, `audit`, `config`, `security`, `identityorg`, and `portal`.

**Interfaces:**
- Consumes: current Spring Boot、安全、审计、组织和门户配置实现。
- Produces: 类型职责、配置语义、安全边界和公共方法契约的中文说明。

- [ ] **Step 1: Add type-level Javadoc**

Controller、Service、Store、Adapter、配置类、领域策略和 DTO 均说明职责、调用方和边界，例如：

```java
/**
 * 统一配置浏览器会话认证、CSRF 防护、门户访问规则和密码生命周期过滤器。
 *
 * <p>居民端可经公网入口访问，但医护和管理接口仍由角色规则限制；客户端路由只负责体验，
 * 服务端安全链始终是最终授权边界。</p>
 */
```

- [ ] **Step 2: Document security-sensitive methods and decisions**

为 JWT 密钥轮换、MFA 挑战、首次改密、Demo 账号初始化、审计写入、站点层级校验等方法补充参数、返回值、副作用、异常和“为什么”注释，不记录实际凭据。

- [ ] **Step 3: Compile the batch**

Run: `mvn.cmd -pl backend -DskipTests compile`

Expected: `BUILD SUCCESS`.

### Task 3: 注释核心医疗业务

**Files:**
- Modify: all Java files under `clinical`, `scheduling`, `encounter`, and `residentregistry`.

**Interfaces:**
- Consumes: 当前预约、接诊、病历、居民档案和门户服务实现。
- Produces: 状态机、事务、数据归属、幂等和时间规则的可读契约。

- [ ] **Step 1: Document domain invariants**

为预约、排班时段、接诊记录、监护关系、受保护证件号等领域类型解释合法状态和不变量，例如：

```java
/**
 * 确认预约并占用排班时段。
 *
 * @throws IllegalStateException 当前状态不允许确认，或关联时段已不可用时抛出
 */
```

- [ ] **Step 2: Document application and portal flows**

解释应用服务事务边界、Controller 角色范围、居民/医护数据隔离、响应 DTO 映射和 Demo 数据初始化幂等性。

- [ ] **Step 3: Add focused inline rationale**

仅在状态转换、幂等键命中、时间窗口判断、软删除过滤或兼容字段映射处解释原因；不逐句翻译查询和赋值。

- [ ] **Step 4: Compile the batch**

Run: `mvn.cmd -pl backend -DskipTests compile`

Expected: `BUILD SUCCESS`.

### Task 4: 注释药事、公卫与平台扩展业务

**Files:**
- Modify: all Java files under `pharmacy`, `inventory`, `billing`, `insurance`, `familydoctor`, `publichealth`, `referral`, `integration`, `notification`, and `quality`.

**Interfaces:**
- Consumes: R3–R5 处方、库存、收费、签约、公卫、转诊和外部平台适配实现。
- Produces: 业务闭环、状态约束、原子性、适配器契约和模拟实现边界的中文说明。

- [ ] **Step 1: Document domain and transaction rules**

说明处方状态、库存批次选择、支付退款、医保状态、签约任务、重点人群、公卫规则和转诊状态的合法变化及失败条件。

- [ ] **Step 2: Document adapter and controller boundaries**

明确模拟区域平台适配器可替换范围、幂等回执语义、居民通知边界、质量指标用途和各 Controller 的角色限制。

- [ ] **Step 3: Compile the batch**

Run: `mvn.cmd -pl backend -DskipTests compile`

Expected: `BUILD SUCCESS`.

### Task 5: 注释运行配置与数据库迁移

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Modify: `backend/src/main/resources/db/migration/V2__add_portal_accounts.sql`
- Modify: `backend/src/main/resources/db/migration/V3__add_password_lifecycle.sql`
- Modify: `backend/src/main/resources/db/migration/V4__unify_clinical_soft_delete.sql`
- Modify: `backend/src/main/resources/db/migration/V5__add_password_version.sql`
- Modify: `backend/src/main/resources/db/migration/V6__r1_identity_org_registry_audit.sql`
- Modify: `backend/src/main/resources/db/migration/V7__mask_legacy_patient_identifier.sql`
- Modify: `backend/src/main/resources/db/migration/V8__r2_scheduling_encounter.sql`
- Modify: `backend/src/main/resources/db/migration/V9__r3_pharmacy_inventory_billing.sql`
- Modify: `backend/src/main/resources/db/migration/V10__r4_family_doctor_public_health.sql`
- Modify: `backend/src/main/resources/db/migration/V11__r5_referral_integration_notification_quality.sql`

**Interfaces:**
- Consumes: 当前配置键、环境变量、Flyway 版本链和 SQL 语句。
- Produces: 可独立阅读的环境配置说明与迁移意图；不改变运行值和数据库结构。

- [ ] **Step 1: Annotate configuration domains**

用 `#` 说明数据源、JPA/Flyway、Demo/正式模式、安全策略和密钥配置的用途、覆盖方式与生产约束。

- [ ] **Step 2: Annotate every migration**

每个脚本增加迁移目标、前置假设、不可逆影响和业务分段；不得改动任何 SQL token 或语句顺序。

- [ ] **Step 3: Validate resources through tests**

Run: `mvn.cmd -pl backend test`

Expected: 全部可执行测试通过；依赖本机 Docker 的测试可按其既有条件跳过。

### Task 6: 全量质量复核与交付验证

**Files:**
- Review: all files under `backend/src/main` changed by Tasks 1–5.

**Interfaces:**
- Consumes: 完成后的注释集和设计规范。
- Produces: 无行为差异、无低价值注释、可编译可测试的最终后端源码。

- [ ] **Step 1: Audit coverage**

检查所有生产包具有 `package-info.java`，所有承担业务职责的顶层类型具有中文类型说明，复杂分支具有原因说明。

- [ ] **Step 2: Audit comment quality**

搜索作者日期模板、敏感值、无责任 TODO、语法翻译式注释和与实现冲突的承诺，并逐项清理。

- [ ] **Step 3: Audit semantic preservation**

检查差异，确认已有 Java/YAML/SQL 文件仅增加或改写注释；任何非注释差异均撤回或单独说明。

- [ ] **Step 4: Run final test and package gates**

Run: `mvn.cmd -pl backend test`

Expected: 所有可执行测试通过。

Run: `mvn.cmd -pl backend package -DskipTests`

Expected: `BUILD SUCCESS`，生成可运行后端 JAR。

- [ ] **Step 5: Report evidence**

汇总新增/修改文件数、模块覆盖情况、测试数量、跳过项、打包结果和任何未解决警告；不提交 `backend/` 源码。
