# 旧系统迁移说明

## 结构变化

| 旧系统 | 新系统 | 处理方式 |
| --- | --- | --- |
| `patient_info.paient_idcard` | `patient.id_card` | 修复拼写并增加数值型内部主键 |
| `patient_info.paient_money` | `patient.balance` | 字符串改为 `DECIMAL(12,2)` |
| `doctor_info` | `doctor` | 保留科室、职称、专长和出诊摘要，增加唯一工号 |
| `order` | `appointment` | 规避 SQL 关键字，增加预约号、医生关联与状态机 |
| `patient_health` | `health_record` | 血压拆分为收缩压/舒张压，补充采集时间和体重 |
| `medicine_info` | `medicine` | 金额改为小数，保留库存与安全库存 |
| `chronic_disease` | `chronic_case` | 使用患者/医生内部 ID 建立外键 |
| `sys_user` + Shiro Session | `app_user` + JWT | 密码使用 BCrypt，接口改为无状态认证 |

## 推荐迁移顺序

1. 备份旧库，并在只读副本上执行清洗；不得直接修改生产旧库。
2. 先迁移患者和医生，生成“旧业务键 → 新 ID”映射表。
3. 再迁移预约、健康记录、慢病和药品，利用映射表补齐外键。
4. 对金额、日期、证件号和软删除标志做数据质量校验。
5. 比对各业务表总量、有效记录量以及按日统计结果。
6. 进行双写或短时停机切换，验证后再将旧系统设为只读。

旧公共卫生模块（老年体检、孕产妇、传染病、预防接种）仍保存在 `legacy/guns`。新版已预留相同的分层方式，建议在核心档案数据迁移稳定后，按独立领域模块逐项迁移。
