package com.community.healthcare.clinical;

import com.community.healthcare.security.DemoAccountProvisioner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 为演示环境幂等创建可立即体验的居民、医护、预约、药品和慢病数据。
 *
 * <p>仅在显式开启 Bootstrap 且居民表为空时运行，避免应用重启覆盖试用数据。
 * 初始化顺序晚于基础账号/结构准备，并在 R1 表存在时补齐人员与居民的站点范围。</p>
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
class ClinicalDataInitializer implements CommandLineRunner {
    private final PatientRepository patients; private final DoctorRepository doctors;
    private final AppointmentRepository appointments; private final MedicineRepository medicines;
    private final HealthRecordRepository healthRecords; private final ChronicCaseRepository chronicCases;
    private final DemoAccountProvisioner accounts;
    private final JdbcTemplate jdbc;
    ClinicalDataInitializer(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments,
            MedicineRepository medicines, HealthRecordRepository healthRecords, ChronicCaseRepository chronicCases,
            DemoAccountProvisioner accounts, JdbcTemplate jdbc) {
        this.patients = patients; this.doctors = doctors; this.appointments = appointments;
        this.medicines = medicines; this.healthRecords = healthRecords; this.chronicCases = chronicCases;
        this.accounts = accounts;
        this.jdbc = jdbc;
    }
    /** 创建完整演示闭环；已有任一居民时视为已经初始化。 */
    @Override public void run(String... args) {
        if (patients.count() > 0) return;
        Patient patient = new Patient(); patient.setIdCard("1101**********1234"); patient.setName("张明");
        patient.setGender("男"); patient.setBirthDate(LocalDate.of(1990, 1, 1)); patient.setPhone("13800000001");
        patient.setAddress("幸福社区 8 号楼"); patient.setBalance(new BigDecimal("200.00")); patients.save(patient);
        Patient patient2 = new Patient(); patient2.setIdCard("1101**********2345"); patient2.setName("李芳");
        patient2.setGender("女"); patient2.setBirthDate(LocalDate.of(1985, 5, 6)); patient2.setPhone("13800000002");
        patient2.setAddress("健康社区 2 号楼"); patients.save(patient2);

        Doctor doctor = new Doctor(); doctor.setEmployeeNo("D001"); doctor.setName("王医生"); doctor.setDepartment("全科医学科");
        doctor.setTitle("主任医师"); doctor.setSpecialty("高血压、糖尿病与老年慢病管理"); doctor.setPhone("010-88880001");
        doctor.setScheduleSummary("周一至周五 08:00-17:00"); doctors.save(doctor);
        Doctor nurse = new Doctor(); nurse.setEmployeeNo("N001"); nurse.setName("刘护士"); nurse.setDepartment("社区护理科");
        nurse.setTitle("主管护师"); nurse.setSpecialty("居家护理、生命体征监测与健康宣教"); nurse.setPhone("010-88880002");
        nurse.setScheduleSummary("周一至周五 08:00-17:00"); doctors.save(nurse);
        Doctor pharmacist = new Doctor(); pharmacist.setEmployeeNo("P001"); pharmacist.setName("赵药师"); pharmacist.setDepartment("社区药房");
        pharmacist.setTitle("主管药师"); pharmacist.setSpecialty("处方审核、合理用药与调剂复核"); pharmacist.setPhone("010-88880003");
        pharmacist.setScheduleSummary("周一至周五 08:00-17:00"); doctors.save(pharmacist);
        Doctor registrar = new Doctor(); registrar.setEmployeeNo("R001"); registrar.setName("陈收费员"); registrar.setDepartment("挂号收费处");
        registrar.setTitle("收费员"); registrar.setSpecialty("挂号、收费与医保结算"); registrar.setPhone("010-88880004");
        registrar.setScheduleSummary("周一至周五 08:00-17:00"); doctors.save(registrar);

        Appointment appointment = new Appointment(); appointment.setAppointmentNo("APDEMO000001"); appointment.setPatientId(patient.getId());
        appointment.setDoctorId(doctor.getId()); appointment.setScheduledAt(LocalDateTime.now().plusHours(1));
        appointment.setReason("高血压复诊"); appointments.save(appointment);

        Medicine medicine = new Medicine(); medicine.setName("苯磺酸氨氯地平片"); medicine.setCategory("心血管用药");
        medicine.setPrice(new BigDecimal("28.50")); medicine.setStock(24); medicine.setMinimumStock(30); medicine.setSpecification("5mg×28片"); medicines.save(medicine);

        HealthRecord record = new HealthRecord(); record.setPatientId(patient.getId()); record.setRecordedAt(LocalDateTime.now().minusHours(2));
        record.setHeartRate(76); record.setSystolicPressure(132); record.setDiastolicPressure(86); record.setBloodOxygen(98); record.setWeight(new BigDecimal("68.5"));
        record.setNote("家庭随访采集"); healthRecords.save(record);

        ChronicCase chronic = new ChronicCase(); chronic.setPatientId(patient.getId()); chronic.setDiseaseType("高血压");
        chronic.setRiskLevel("中风险"); chronic.setDiagnosisDate(LocalDate.now().minusYears(3)); chronic.setDoctorId(doctor.getId());
        chronic.setManagementPlan("每周监测血压，低盐饮食，每月随访"); chronicCases.save(chronic);

        accounts.ensureDemoAccounts(patient.getId(), doctor.getId(), nurse.getId(), pharmacist.getId(), registrar.getId());
        ensureH2DemoScopeSchema();
        if (r1ScopeSchemaExists()) seedStaffScope(doctor, nurse, pharmacist, registrar, patient, patient2);
    }

    /**
     * 为未执行完整旧迁移链的 H2 Demo 库创建范围数据所需最小表。
     * 正式数据库不会进入该兼容分支。
     */
    private void ensureH2DemoScopeSchema() {
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            if (!"H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return;
        } catch (SQLException ex) {
            throw new IllegalStateException("无法检查演示数据库类型", ex);
        }
        jdbc.execute("create table if not exists staff_profile (id bigint generated by default as identity primary key, "
                + "organization_id bigint not null, staff_no varchar(64) not null unique, name varchar(128) not null, "
                + "staff_type varchar(32) not null, account_status varchar(32) not null, active boolean not null, "
                + "created_at timestamp not null, updated_at timestamp not null, version bigint not null)");
        jdbc.execute("create table if not exists staff_site_assignment (id bigint generated by default as identity primary key, "
                + "staff_profile_id bigint not null, site_id bigint not null, department_id bigint null, role_code varchar(32) not null, "
                + "valid_from timestamp not null, valid_to timestamp null, active boolean not null, created_at timestamp not null)");
        jdbc.execute("create table if not exists patient_site_enrollment (id bigint generated by default as identity primary key, "
                + "patient_id bigint not null, site_id bigint not null, enrolled_at timestamp not null, ended_at timestamp null, "
                + "active boolean not null, created_at timestamp not null)");
    }

    private boolean r1ScopeSchemaExists() {
        Integer count = jdbc.queryForObject("select count(*) from information_schema.tables "
                + "where lower(table_name) in ('organization','site','staff_profile','staff_site_assignment','patient_site_enrollment')",
                Integer.class);
        return count != null && count == 5;
    }

    /** 将演示医护与居民绑定到同一服务站，使医护门户数据隔离规则可被真实演示。 */
    private void seedStaffScope(Doctor doctor, Doctor nurse, Doctor pharmacist, Doctor registrar,
                                Patient... enrolledPatients) {
        Instant now = Instant.now();
        jdbc.update("insert into organization(code,name,active,created_at,updated_at,version) values('DEMO-ORG','演示社区中心',true,?,?,0)", now, now);
        Long organizationId = jdbc.queryForObject("select id from organization where code='DEMO-ORG'", Long.class);
        jdbc.update("insert into site(organization_id,code,name,site_type,active,created_at,updated_at,version) values(?,'DEMO-SITE','演示服务站','CENTER',true,?,?,0)", organizationId, now, now);
        Long siteId = jdbc.queryForObject("select id from site where code='DEMO-SITE'", Long.class);
        jdbc.update("insert into department(organization_id,site_id,code,name,active,created_at,updated_at,version) values(?,?, 'DEMO-GP','全科医学科',true,?,?,0)",
                organizationId, siteId, now, now);
        Long departmentId = jdbc.queryForObject("select id from department where code='DEMO-GP'", Long.class);
        Long doctorProfileId = seedStaffProfile(organizationId, siteId, departmentId, doctor, "DOCTOR", now);
        Long nurseProfileId = seedStaffProfile(organizationId, siteId, null, nurse, "NURSE", now);
        Long pharmacistProfileId = seedStaffProfile(organizationId, siteId, null, pharmacist, "PHARMACIST", now);
        Long registrarProfileId = seedStaffProfile(organizationId, siteId, null, registrar, "REGISTRAR", now);
        for (Patient patient : enrolledPatients) {
            jdbc.update("insert into patient_site_enrollment(patient_id,site_id,enrolled_at,active,created_at) values(?,?,?,true,?)",
                    patient.getId(), siteId, now, now);
        }
        jdbc.update("update app_user set staff_profile_id=? where username='doctor'", doctorProfileId);
        jdbc.update("update app_user set staff_profile_id=? where username='nurse'", nurseProfileId);
        jdbc.update("update app_user set staff_profile_id=? where username='pharmacist'", pharmacistProfileId);
        jdbc.update("update app_user set staff_profile_id=? where username='registrar'", registrarProfileId);
        seedSchedulingDemo(siteId, departmentId, doctorProfileId, enrolledPatients[0].getId());
    }

    private Long seedStaffProfile(Long organizationId, Long siteId, Long departmentId,
                                  Doctor staff, String role, Instant now) {
        jdbc.update("insert into staff_profile(organization_id,staff_no,name,staff_type,account_status,active,created_at,updated_at,version) values(?,?,?,?, 'ACTIVE',true,?,?,0)",
                organizationId, staff.getEmployeeNo(), staff.getName(), role, now, now);
        Long profileId = jdbc.queryForObject("select id from staff_profile where staff_no=?", Long.class, staff.getEmployeeNo());
        jdbc.update("insert into staff_site_assignment(staff_profile_id,site_id,department_id,role_code,valid_from,active,created_at) values(?,?,?,?,?,true,?)",
                profileId, siteId, departmentId, role, now, now);
        return profileId;
    }

    /** 为统一预约模型建立一条可立即办理的演示预约。 */
    private void seedSchedulingDemo(Long siteId, Long departmentId, Long doctorProfileId, Long patientId) {
        LocalDateTime startsAt = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
        LocalDateTime endsAt = startsAt.plusMinutes(30);
        jdbc.update("insert into sched_session(site_id,department_id,staff_profile_id,service_date,starts_at,ends_at,status,created_at,updated_at,version) values(?,?,?,?,?,?,'OPEN',current_timestamp,current_timestamp,0)",
                siteId, departmentId, doctorProfileId, startsAt.toLocalDate(), startsAt, endsAt);
        Long sessionId = jdbc.queryForObject("select max(id) from sched_session where staff_profile_id=?", Long.class, doctorProfileId);
        jdbc.update("insert into sched_slot(session_id,starts_at,ends_at,status,created_at,updated_at,version) values(?,?,?,'RESERVED',current_timestamp,current_timestamp,0)",
                sessionId, startsAt, endsAt);
        Long slotId = jdbc.queryForObject("select max(id) from sched_slot where session_id=?", Long.class, sessionId);
        jdbc.update("insert into sched_appointment(slot_id,patient_id,status,reason,created_at,updated_at,version) values(?,?,'CONFIRMED','高血压复诊',current_timestamp,current_timestamp,0)",
                slotId, patientId);
    }
}
