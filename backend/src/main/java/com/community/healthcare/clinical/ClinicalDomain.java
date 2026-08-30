package com.community.healthcare.clinical;

import com.community.healthcare.shared.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 既有临床模块中的居民基础实体。
 *
 * <p>证件字段只保存脱敏快照，精确匹配使用 residentregistry 模块的受保护标识。</p>
 */
@Entity
@Table(name = "patient", indexes = @Index(name = "idx_patient_name", columnList = "name"))
@SQLRestriction("active = true")
class Patient extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(max = 32) @Column(nullable = false, unique = true, length = 32)
    private String idCard;
    @NotBlank @Size(max = 64) @Column(nullable = false, length = 64)
    private String name;
    @Size(max = 16) private String gender;
    private LocalDate birthDate;
    @Size(max = 32) private String phone;
    @Size(max = 255) private String address;
    @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(nullable = false) private boolean active = true;
    public Long getId() { return id; }
    public String getIdCard() { return idCard; } public void setIdCard(String value) { idCard = value; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getGender() { return gender; } public void setGender(String value) { gender = value; }
    public LocalDate getBirthDate() { return birthDate; } public void setBirthDate(LocalDate value) { birthDate = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public String getAddress() { return address; } public void setAddress(String value) { address = value; }
    public BigDecimal getBalance() { return balance; } public void setBalance(BigDecimal value) { balance = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    void deactivate(String actor) { active = false; markDeleted(actor); }
    void updateFrom(Patient value) { if (value.idCard != null) setIdCard(value.idCard); setName(value.name); setGender(value.gender); setBirthDate(value.birthDate); setPhone(value.phone); setAddress(value.address); setBalance(value.balance); }
}

/** 医护人员基础实体；当前同时兼容医生和护士演示档案。 */
@Entity
@Table(name = "doctor", indexes = @Index(name = "idx_doctor_name", columnList = "name"))
@SQLRestriction("active = true")
class Doctor extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 32) @Column(nullable = false, unique = true, length = 32)
    private String employeeNo;
    @NotBlank @Size(max = 64) @Column(nullable = false, length = 64)
    private String name;
    @NotBlank @Size(max = 64) @Column(nullable = false, length = 64)
    private String department;
    @Size(max = 64) private String title;
    @Size(max = 500) private String specialty;
    @Size(max = 32) private String phone;
    @Size(max = 255) private String scheduleSummary;
    @Column(nullable = false) private boolean active = true;
    public Long getId() { return id; }
    public String getEmployeeNo() { return employeeNo; } public void setEmployeeNo(String value) { employeeNo = value; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getDepartment() { return department; } public void setDepartment(String value) { department = value; }
    public String getTitle() { return title; } public void setTitle(String value) { title = value; }
    public String getSpecialty() { return specialty; } public void setSpecialty(String value) { specialty = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public String getScheduleSummary() { return scheduleSummary; } public void setScheduleSummary(String value) { scheduleSummary = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    void deactivate(String actor) { active = false; markDeleted(actor); }
    void updateFrom(Doctor value) { setEmployeeNo(value.employeeNo); setName(value.name); setDepartment(value.department); setTitle(value.title); setSpecialty(value.specialty); setPhone(value.phone); setScheduleSummary(value.scheduleSummary); }
}

/**
 * 既有门户预约实体。
 *
 * <p>居民和医生以 ID 关联，姓名只在响应映射时批量查询；状态变化必须通过统一策略执行。</p>
 */
@Entity
@Table(name = "appointment", indexes = {
        @Index(name = "idx_appointment_time", columnList = "scheduled_at"),
        @Index(name = "idx_appointment_patient", columnList = "patient_id")})
@SQLRestriction("active = true")
class Appointment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40) private String appointmentNo;
    @NotNull @Column(nullable = false) private Long patientId;
    @NotNull @Column(nullable = false) private Long doctorId;
    @NotNull @Column(nullable = false) private LocalDateTime scheduledAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private AppointmentStatus status = AppointmentStatus.PENDING;
    @NotBlank @Size(max = 500) @Column(nullable = false, length = 500) private String reason;
    @Size(max = 500) private String remark;
    @Column(nullable = false) private boolean active = true;
    public Long getId() { return id; }
    public String getAppointmentNo() { return appointmentNo; } public void setAppointmentNo(String value) { appointmentNo = value; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long value) { patientId = value; }
    public Long getDoctorId() { return doctorId; } public void setDoctorId(Long value) { doctorId = value; }
    public LocalDateTime getScheduledAt() { return scheduledAt; } public void setScheduledAt(LocalDateTime value) { scheduledAt = value; }
    public AppointmentStatus getStatus() { return status; } public void setStatus(AppointmentStatus value) { status = value; }
    public String getReason() { return reason; } public void setReason(String value) { reason = value; }
    public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    void deactivate(String actor) { active = false; markDeleted(actor); }
    void updateFrom(Appointment value) { setPatientId(value.patientId); setDoctorId(value.doctorId); setScheduledAt(value.scheduledAt); setReason(value.reason); setRemark(value.remark); }
}

/** 既有门户使用的简化预约状态。 */
enum AppointmentStatus { PENDING, CONFIRMED, COMPLETED, CANCELLED }

/** 居民一次生命体征或体重测量记录，各数值单位由 API 契约固定。 */
@Entity
@Table(name = "health_record", indexes = @Index(name = "idx_health_patient_time", columnList = "patient_id,recorded_at"))
@SQLRestriction("active = true")
class HealthRecord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @Column(nullable = false) private Long patientId;
    @NotNull @Column(nullable = false) private LocalDateTime recordedAt;
    @Min(20) @Max(250) private Integer heartRate;
    @Min(40) @Max(260) private Integer systolicPressure;
    @Min(30) @Max(180) private Integer diastolicPressure;
    @Min(50) @Max(100) private Integer bloodOxygen;
    @DecimalMin("20.0") @DecimalMax("250.0") private BigDecimal weight;
    @Size(max = 500) private String note;
    @Column(nullable = false) private boolean active = true;
    public Long getId() { return id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long value) { patientId = value; }
    public LocalDateTime getRecordedAt() { return recordedAt; } public void setRecordedAt(LocalDateTime value) { recordedAt = value; }
    public Integer getHeartRate() { return heartRate; } public void setHeartRate(Integer value) { heartRate = value; }
    public Integer getSystolicPressure() { return systolicPressure; } public void setSystolicPressure(Integer value) { systolicPressure = value; }
    public Integer getDiastolicPressure() { return diastolicPressure; } public void setDiastolicPressure(Integer value) { diastolicPressure = value; }
    public Integer getBloodOxygen() { return bloodOxygen; } public void setBloodOxygen(Integer value) { bloodOxygen = value; }
    public BigDecimal getWeight() { return weight; } public void setWeight(BigDecimal value) { weight = value; }
    public String getNote() { return note; } public void setNote(String value) { note = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    void deactivate(String actor) { active = false; markDeleted(actor); }
}

/**
 * 药品目录及汇总库存实体。
 *
 * <p>汇总库存用于既有管理和预警页面；批次级库存由 inventory/pharmacy 模块维护。</p>
 */
@Entity
@Table(name = "medicine", indexes = @Index(name = "idx_medicine_name", columnList = "name"))
@SQLRestriction("active = true")
class Medicine extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 64) @Column(nullable = false, unique = true, length = 64) private String name;
    @Size(max = 64) private String category;
    @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price = BigDecimal.ZERO;
    @PositiveOrZero @Column(nullable = false) private Integer stock = 0;
    @PositiveOrZero @Column(nullable = false) private Integer minimumStock = 0;
    @Size(max = 500) private String specification;
    @Column(nullable = false) private boolean active = true;
    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getCategory() { return category; } public void setCategory(String value) { category = value; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal value) { price = value; }
    public Integer getStock() { return stock; } public void setStock(Integer value) { stock = value; }
    public Integer getMinimumStock() { return minimumStock; } public void setMinimumStock(Integer value) { minimumStock = value; }
    public String getSpecification() { return specification; } public void setSpecification(String value) { specification = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    void deactivate(String actor) { active = false; markDeleted(actor); }
    /** 在整数溢出和负库存保护下调整汇总库存。 */
    void adjustStock(int delta) {
        int adjusted;
        try { adjusted = Math.addExact(stock, delta); }
        catch (ArithmeticException exception) { throw new IllegalArgumentException("库存调整数值超出范围"); }
        if (adjusted < 0) throw new IllegalArgumentException("库存不足，调整后库存不能为负数");
        stock = adjusted;
    }
    void updateFrom(Medicine value) { setName(value.name); setCategory(value.category); setPrice(value.price); setMinimumStock(value.minimumStock); setSpecification(value.specification); }
}

/** 居民慢病在管档案，保存风险分层、责任医生和管理计划。 */
@Entity
@Table(name = "chronic_case", indexes = @Index(name = "idx_chronic_patient", columnList = "patient_id"))
@SQLRestriction("active = true")
class ChronicCase extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @Column(nullable = false) private Long patientId;
    @NotBlank @Size(max = 64) @Column(nullable = false, length = 64) private String diseaseType;
    @NotBlank @Pattern(regexp = "^(低风险|中风险|高风险)$", message = "风险等级仅支持低风险、中风险或高风险")
    @Column(nullable = false, length = 32) private String riskLevel;
    @NotNull @Column(nullable = false) private LocalDate diagnosisDate;
    private Long doctorId;
    @Size(max = 500) private String managementPlan;
    @Column(nullable = false) private boolean active = true;
    public Long getId() { return id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long value) { patientId = value; }
    public String getDiseaseType() { return diseaseType; } public void setDiseaseType(String value) { diseaseType = value; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String value) { riskLevel = RiskLevel.requireSupported(value); }
    public LocalDate getDiagnosisDate() { return diagnosisDate; } public void setDiagnosisDate(LocalDate value) { diagnosisDate = value; }
    public Long getDoctorId() { return doctorId; } public void setDoctorId(Long value) { doctorId = value; }
    public String getManagementPlan() { return managementPlan; } public void setManagementPlan(String value) { managementPlan = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    void deactivate(String actor) { active = false; markDeleted(actor); }
    void updateFrom(ChronicCase value) { setPatientId(value.patientId); setDiseaseType(value.diseaseType); setRiskLevel(value.riskLevel); setDiagnosisDate(value.diagnosisDate); setDoctorId(value.doctorId); setManagementPlan(value.managementPlan); }
}

/** 系统支持的标准慢病风险层级及中文持久化值。 */
enum RiskLevel {
    LOW("低风险"), MEDIUM("中风险"), HIGH("高风险");
    private final String value;
    RiskLevel(String value) { this.value = value; }
    String value() { return value; }
    static String requireSupported(String value) {
        if (value == null) return null;
        for (RiskLevel level : values()) if (level.value.equals(value)) return value;
        throw new IllegalArgumentException("风险等级仅支持低风险、中风险或高风险");
    }
}

/** 批量响应映射所需的最小居民姓名投影。 */
interface PatientNameSnapshot {
    Long getId();
    String getName();
}

/** 批量响应映射所需的最小医护姓名和科室投影。 */
interface DoctorNameSnapshot {
    Long getId();
    String getName();
    String getDepartment();
}

/** 居民实体仓储，提供关键字、范围和姓名投影查询。 */
interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("select p from Patient p where (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')) or p.phone like concat('%', :keyword, '%'))")
    Page<Patient> search(@Param("keyword") String keyword, Pageable pageable);
    @Query("select p from Patient p where p.id in :ids and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')) or p.phone like concat('%', :keyword, '%'))")
    Page<Patient> searchByIds(@Param("ids") Collection<Long> ids, @Param("keyword") String keyword, Pageable pageable);
    @Query("select p.id as id, p.name as name from Patient p where p.id in :ids")
    List<PatientNameSnapshot> findNameSnapshots(@Param("ids") Collection<Long> ids);
    long countByActiveTrue();
}
/** 医护实体仓储，提供关键字检索和批量姓名投影。 */
interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @Query("select d from Doctor d where (:keyword = '' or lower(d.name) like lower(concat('%', :keyword, '%')) or lower(d.department) like lower(concat('%', :keyword, '%')))")
    Page<Doctor> search(@Param("keyword") String keyword, Pageable pageable);
    @Query("select d.id as id, d.name as name, d.department as department from Doctor d where d.id in :ids")
    List<DoctorNameSnapshot> findNameSnapshots(@Param("ids") Collection<Long> ids);
    long countByActiveTrue();
}
/**
 * 预约仓储。
 *
 * <p>医护门户使用带 doctorId 和 patientId 集合的组合查询，把数据范围下推到数据库分页。</p>
 */
interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("select a from Appointment a where :keyword = '' or lower(a.appointmentNo) like lower(concat('%', :keyword, '%')) or lower(a.reason) like lower(concat('%', :keyword, '%'))")
    Page<Appointment> search(@Param("keyword") String keyword, Pageable pageable);
    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
    Page<Appointment> findByScheduledAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Appointment> findByDoctorIdAndScheduledAtBetween(Long doctorId, LocalDateTime start,
                                                         LocalDateTime end, Pageable pageable);
    Page<Appointment> findByDoctorIdAndPatientIdInAndScheduledAtBetween(Long doctorId,
            Collection<Long> patientIds, LocalDateTime start, LocalDateTime end, Pageable pageable);
    @Query("select a from Appointment a where a.doctorId = :doctorId and " +
            "(:keyword = '' or lower(a.appointmentNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(a.reason) like lower(concat('%', :keyword, '%')))")
    Page<Appointment> searchByDoctorId(@Param("doctorId") Long doctorId,
                                       @Param("keyword") String keyword, Pageable pageable);
    @Query("select a from Appointment a where a.doctorId = :doctorId and a.patientId in :patientIds and " +
            "(:keyword = '' or lower(a.appointmentNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(a.reason) like lower(concat('%', :keyword, '%')))")
    Page<Appointment> searchByDoctorIdAndPatientIds(@Param("doctorId") Long doctorId,
            @Param("patientIds") Collection<Long> patientIds, @Param("keyword") String keyword, Pageable pageable);
    @Query("select a from Appointment a where a.patientId in :patientIds and " +
            "(:keyword = '' or lower(a.appointmentNo) like lower(concat('%', :keyword, '%')) or lower(a.reason) like lower(concat('%', :keyword, '%')))")
    Page<Appointment> searchByPatientIds(@Param("patientIds") Collection<Long> patientIds,
                                         @Param("keyword") String keyword, Pageable pageable);
    Page<Appointment> findByPatientIdInAndScheduledAtBetween(Collection<Long> patientIds,
                                                              LocalDateTime start, LocalDateTime end, Pageable pageable);
    @Query("select distinct a.patientId from Appointment a where a.doctorId = :doctorId")
    List<Long> findDistinctPatientIdsByDoctorId(@Param("doctorId") Long doctorId);
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);
    Optional<Appointment> findByIdAndPatientIdIn(Long id, Collection<Long> patientIds);
    Optional<Appointment> findFirstByPatientIdAndStatusInAndScheduledAtAfterOrderByScheduledAtAsc(
            Long patientId, Collection<AppointmentStatus> statuses, LocalDateTime scheduledAt);
    long countByScheduledAtBetween(LocalDateTime start, LocalDateTime end);
    long countByScheduledAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, AppointmentStatus status);
    long countByDoctorIdAndScheduledAtBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    long countByDoctorIdAndScheduledAtBetweenAndStatus(Long doctorId, LocalDateTime start,
                                                      LocalDateTime end, AppointmentStatus status);
    long countByStatus(AppointmentStatus status);
    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    long countByPatientIdInAndScheduledAtBetween(Collection<Long> patientIds, LocalDateTime start, LocalDateTime end);
    long countByPatientIdInAndScheduledAtBetweenAndStatus(Collection<Long> patientIds, LocalDateTime start, LocalDateTime end, AppointmentStatus status);
    long countByPatientIdInAndStatus(Collection<Long> patientIds, AppointmentStatus status);
    long countByPatientIdAndStatusIn(Long patientId, Collection<AppointmentStatus> statuses);
}
/** 健康记录仓储，支持居民本人和医护范围分页。 */
interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    Page<HealthRecord> findByPatientId(Long patientId, Pageable pageable);
    Page<HealthRecord> findByPatientIdIn(Collection<Long> patientIds, Pageable pageable);
    Optional<HealthRecord> findFirstByPatientIdOrderByRecordedAtDesc(Long patientId);
    long countByPatientId(Long patientId);
}
/** 药品仓储，库存调整查询使用写锁避免并发丢失更新。 */
interface MedicineRepository extends JpaRepository<Medicine, Long> {
    @Query("select m from Medicine m where (:keyword = '' or lower(m.name) like lower(concat('%', :keyword, '%')) or lower(m.category) like lower(concat('%', :keyword, '%')))")
    Page<Medicine> search(@Param("keyword") String keyword, Pageable pageable);
    @Query("select count(m) from Medicine m where m.stock <= m.minimumStock") long countLowStock();
    @Query("select m from Medicine m where m.stock <= m.minimumStock order by m.stock asc")
    List<Medicine> findLowStock();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medicine m where m.id = :id")
    Optional<Medicine> findByIdForStockUpdate(@Param("id") Long id);
}
/** 慢病档案仓储，支持按居民范围和风险关键字查询。 */
interface ChronicCaseRepository extends JpaRepository<ChronicCase, Long> {
    @Query("select c from ChronicCase c where (:keyword = '' or lower(c.diseaseType) like lower(concat('%', :keyword, '%')) or lower(c.riskLevel) like lower(concat('%', :keyword, '%')))")
    Page<ChronicCase> search(@Param("keyword") String keyword, Pageable pageable);
    @Query("select c from ChronicCase c where c.patientId in :patientIds and (:keyword = '' or lower(c.diseaseType) like lower(concat('%', :keyword, '%')) or lower(c.riskLevel) like lower(concat('%', :keyword, '%')))")
    Page<ChronicCase> searchByPatientIds(@Param("patientIds") Collection<Long> patientIds,
                                         @Param("keyword") String keyword, Pageable pageable);
    List<ChronicCase> findByPatientIdAndActiveTrueOrderByDiagnosisDateDesc(Long patientId);
    long countByActiveTrue();
    long countByPatientIdAndActiveTrue(Long patientId);
    long countByPatientIdInAndActiveTrue(Collection<Long> patientIds);
}
