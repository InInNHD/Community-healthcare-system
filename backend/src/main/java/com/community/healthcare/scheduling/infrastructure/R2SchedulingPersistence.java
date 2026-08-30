package com.community.healthcare.scheduling.infrastructure;

import com.community.healthcare.encounter.domain.EncounterStatus;
import com.community.healthcare.scheduling.domain.AppointmentStatus;
import com.community.healthcare.scheduling.domain.SlotStatus;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 一次医生排班时段，是多个可预约号源的聚合根。 */
@Entity
@Table(name = "sched_session")
class ScheduleSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long siteId;
    @Column(nullable = false) Long departmentId;
    @Column(nullable = false) Long staffProfileId;
    @Column(nullable = false) LocalDate serviceDate;
    @Column(nullable = false) LocalDateTime startsAt;
    @Column(nullable = false) LocalDateTime endsAt;
    @Column(nullable = false, length = 32) String status = "OPEN";
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;
    @Version long version;
    protected ScheduleSessionEntity() {}
    ScheduleSessionEntity(long siteId, long departmentId, long staffId, LocalDateTime starts, LocalDateTime ends) {
        this.siteId = siteId; this.departmentId = departmentId; this.staffProfileId = staffId;
        this.serviceDate = starts.toLocalDate(); this.startsAt = starts; this.endsAt = ends;
        this.createdAt = LocalDateTime.now(); this.updatedAt = createdAt;
    }
}

/** 排班内的原子号源，通过状态与乐观锁共同防止重复占用。 */
@Entity
@Table(name = "sched_slot")
class ScheduleSlotEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long sessionId;
    @Column(nullable = false) LocalDateTime startsAt;
    @Column(nullable = false) LocalDateTime endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) SlotStatus status = SlotStatus.OPEN;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;
    @Version long version;
    protected ScheduleSlotEntity() {}
    ScheduleSlotEntity(long sessionId, LocalDateTime starts, LocalDateTime ends) {
        this.sessionId = sessionId; this.startsAt = starts; this.endsAt = ends;
        this.createdAt = LocalDateTime.now(); this.updatedAt = createdAt;
    }
    void release() { status = SlotStatus.RELEASED; updatedAt = LocalDateTime.now(); }
    void use() { status = SlotStatus.USED; updatedAt = LocalDateTime.now(); }
}

/** 居民对单个号源的预约记录，封装预约状态机迁移规则。 */
@Entity
@Table(name = "sched_appointment")
class ScheduleAppointmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long slotId;
    @Column(nullable = false) Long patientId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) AppointmentStatus status;
    @Column(length = 500) String reason;
    LocalDateTime cancelledAt;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;
    @Version long version;
    protected ScheduleAppointmentEntity() {}
    ScheduleAppointmentEntity(long slotId, long patientId, String reason) {
        this.slotId = slotId; this.patientId = patientId; this.reason = reason;
        this.status = AppointmentStatus.CONFIRMED; this.createdAt = LocalDateTime.now(); this.updatedAt = createdAt;
    }
    void transition(AppointmentStatus expected, AppointmentStatus target) {
        if (status != expected) throw new R2ConflictException("预约状态已变化，请刷新后重试");
        status = target; updatedAt = LocalDateTime.now();
    }
    void cancel() {
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.PENDING) {
            throw new R2ConflictException("当前预约不可取消");
        }
        status = AppointmentStatus.CANCELLED; cancelledAt = LocalDateTime.now(); updatedAt = cancelledAt;
    }
}

/** 预约签到事实；预约唯一约束保证同一预约只能签到一次。 */
@Entity @Table(name = "sched_check_in")
class CheckInEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, unique = true) Long appointmentId;
    @Column(nullable = false) Long checkedInByStaffId;
    @Column(nullable = false) LocalDateTime checkedInAt;
    protected CheckInEntity() {}
    CheckInEntity(long appointmentId, long staffId) {
        this.appointmentId = appointmentId; this.checkedInByStaffId = staffId; this.checkedInAt = LocalDateTime.now();
    }
}

/** 签到后生成的候诊队列项。 */
@Entity @Table(name = "sched_queue_entry")
class QueueEntryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, unique = true) Long appointmentId;
    @Column(nullable = false) Long sessionId;
    @Column(nullable = false) int queueNumber;
    @Column(nullable = false, length = 32) String status = "WAITING";
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;
    @Version long version;
    protected QueueEntryEntity() {}
    QueueEntryEntity(long appointmentId, long sessionId, int queueNumber) {
        this.appointmentId = appointmentId; this.sessionId = sessionId; this.queueNumber = queueNumber;
        this.createdAt = LocalDateTime.now(); this.updatedAt = createdAt;
    }
}

/** 记录预约生命周期关键动作的追加式业务事件。 */
@Entity @Table(name = "sched_event")
class ScheduleEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long appointmentId;
    @Column(nullable = false, length = 64) String eventType;
    @Column(nullable = false, length = 128) String actor;
    @Column(nullable = false) LocalDateTime occurredAt;
    @Column(columnDefinition = "TEXT") String detailsJson;
    protected ScheduleEventEntity() {}
    ScheduleEventEntity(long appointmentId, String type, String actor) {
        this.appointmentId = appointmentId; this.eventType = type; this.actor = actor;
        this.occurredAt = LocalDateTime.now(); this.detailsJson = "{}";
    }
}

/** 线下接诊记录，签署后进入不可编辑状态。 */
@Entity @Table(name = "clinical_encounter")
class ClinicalEncounterEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, unique = true) Long appointmentId;
    @Column(nullable = false) Long patientId;
    @Column(nullable = false) Long staffProfileId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) EncounterStatus status = EncounterStatus.DRAFT;
    @Column(nullable = false) LocalDateTime startedAt;
    LocalDateTime signedAt;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;
    @Version long version;
    protected ClinicalEncounterEntity() {}
    ClinicalEncounterEntity(long appointmentId, long patientId, long staffId) {
        this.appointmentId = appointmentId; this.patientId = patientId; this.staffProfileId = staffId;
        this.startedAt = LocalDateTime.now(); this.createdAt = startedAt; this.updatedAt = startedAt;
    }
    void sign() {
        if (status != EncounterStatus.DRAFT) throw new R2ConflictException("接诊记录已签署或不可签署");
        status = EncounterStatus.SIGNED; signedAt = LocalDateTime.now(); updatedAt = signedAt;
    }
    void touch() {
        if (status != EncounterStatus.DRAFT) throw new R2ConflictException("已签署接诊记录不可编辑");
        updatedAt = LocalDateTime.now();
    }
    void requireDraft() {
        if (status != EncounterStatus.DRAFT) throw new R2ConflictException("已签署或已归档的接诊记录不可新增诊断");
    }
}

/** 接诊记录的 SOAP 病历正文草稿。 */
@Entity @Table(name = "clinical_note")
class ClinicalNoteEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long encounterId;
    @Column(nullable = false, length = 32) String noteType = "SOAP";
    @Column(nullable = false, columnDefinition = "TEXT") String body;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;
    @Version long version;
    protected ClinicalNoteEntity() {}
    ClinicalNoteEntity(long encounterId, String body) {
        this.encounterId = encounterId; this.body = body; this.createdAt = LocalDateTime.now(); this.updatedAt = createdAt;
    }
    void change(String body) { this.body = body; this.updatedAt = LocalDateTime.now(); }
}

/** 接诊过程中录入的结构化诊断条目。 */
@Entity @Table(name = "clinical_diagnosis")
class ClinicalDiagnosisEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long encounterId;
    @Column(nullable = false, length = 64) String diagnosisCode;
    @Column(nullable = false, length = 255) String diagnosisName;
    @Column(nullable = false, length = 32) String diagnosisType;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    protected ClinicalDiagnosisEntity() {}
    ClinicalDiagnosisEntity(long encounterId, String code, String name, String type) {
        this.encounterId = encounterId; this.diagnosisCode = code; this.diagnosisName = name;
        this.diagnosisType = type; this.createdAt = LocalDateTime.now();
    }
}

/** 签署时生成的不可变临床文档版本及其完整性哈希。 */
@Entity @Table(name = "clinical_document_version")
class ClinicalDocumentVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long encounterId;
    @Column(nullable = false, length = 32) String documentType;
    @Column(nullable = false) int versionNo;
    @Column(nullable = false, columnDefinition = "TEXT") String content;
    @Column(nullable = false, length = 64) String contentHash;
    @Column(nullable = false) Long createdByStaffId;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    protected ClinicalDocumentVersionEntity() {}
    ClinicalDocumentVersionEntity(long encounterId, String content, String hash, long staffId) {
        this.encounterId = encounterId; this.documentType = "ENCOUNTER_NOTE"; this.versionNo = 1;
        this.content = content; this.contentHash = hash; this.createdByStaffId = staffId; this.createdAt = LocalDateTime.now();
    }
}

/** 保存请求指纹和结果资源的幂等记录，具有有限有效期。 */
@Entity @Table(name = "idempotency_record")
class IdempotencyRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 64) String operationScope;
    @Column(nullable = false, length = 128) String actorId;
    @Column(nullable = false, length = 128) String idempotencyKey;
    @Column(nullable = false, length = 64) String requestHash;
    @Column(nullable = false, length = 128) String resourceId;
    @Column(nullable = false, columnDefinition = "TEXT") String responseJson;
    @Column(nullable = false, updatable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime expiresAt;
    protected IdempotencyRecordEntity() {}
    IdempotencyRecordEntity(String scope, String actor, String key, String hash, long resourceId) {
        this.operationScope = scope; this.actorId = actor; this.idempotencyKey = key; this.requestHash = hash;
        this.resourceId = String.valueOf(resourceId); this.responseJson = "{\"resourceId\":" + resourceId + "}";
        this.createdAt = LocalDateTime.now(); this.expiresAt = createdAt.plusDays(1);
    }
}

/** 排班时段仓储。 */
interface ScheduleSessionRepository extends JpaRepository<ScheduleSessionEntity, Long> {}
/** 号源仓储，提供数据库级条件抢占能力。 */
interface ScheduleSlotRepository extends JpaRepository<ScheduleSlotEntity, Long> {
    List<ScheduleSlotEntity> findByStatusAndStartsAtAfterOrderByStartsAtAsc(SlotStatus status, LocalDateTime startsAt);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ScheduleSlotEntity s set s.status = com.community.healthcare.scheduling.domain.SlotStatus.RESERVED, s.updatedAt = CURRENT_TIMESTAMP where s.id = :id and s.status = com.community.healthcare.scheduling.domain.SlotStatus.OPEN")
    int reserveIfOpen(@Param("id") long id);
}
/** 预约仓储，支持按居民所有权查询。 */
interface ScheduleAppointmentRepository extends JpaRepository<ScheduleAppointmentEntity, Long> {
    Optional<ScheduleAppointmentEntity> findByIdAndPatientId(long id, long patientId);
}
/** 签到记录仓储。 */
interface CheckInRepository extends JpaRepository<CheckInEntity, Long> {}
/** 候诊队列仓储。 */
interface QueueEntryRepository extends JpaRepository<QueueEntryEntity, Long> {
    @Query("select coalesce(max(q.queueNumber), 0) from QueueEntryEntity q where q.sessionId = :sessionId")
    int maximumQueueNumber(@Param("sessionId") long sessionId);
}
/** 排班业务事件仓储。 */
interface ScheduleEventRepository extends JpaRepository<ScheduleEventEntity, Long> {}
/** 接诊记录仓储。 */
interface ClinicalEncounterRepository extends JpaRepository<ClinicalEncounterEntity, Long> {}
/** 病历正文仓储。 */
interface ClinicalNoteRepository extends JpaRepository<ClinicalNoteEntity, Long> {
    Optional<ClinicalNoteEntity> findByEncounterIdAndNoteType(long encounterId, String noteType);
}
/** 结构化诊断仓储。 */
interface ClinicalDiagnosisRepository extends JpaRepository<ClinicalDiagnosisEntity, Long> {
    List<ClinicalDiagnosisEntity> findByEncounterIdOrderByIdAsc(long encounterId);
}
/** 不可变临床文档版本仓储。 */
interface ClinicalDocumentVersionRepository extends JpaRepository<ClinicalDocumentVersionEntity, Long> {}
/** 预约命令幂等记录仓储。 */
interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
    Optional<IdempotencyRecordEntity> findByOperationScopeAndActorIdAndIdempotencyKey(String scope, String actor, String key);
}
