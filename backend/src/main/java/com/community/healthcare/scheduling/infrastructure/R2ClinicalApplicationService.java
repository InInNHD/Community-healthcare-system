package com.community.healthcare.scheduling.infrastructure;

import com.community.healthcare.observability.RequestCorrelationFilter;

import com.community.healthcare.scheduling.application.IdempotencyKey;
import com.community.healthcare.scheduling.domain.AppointmentStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * R2 排班、预约、签到和门诊病历闭环的应用服务。
 *
 * <p>服务在单一事务中协调状态迁移、任职范围校验、业务事件和幂等记录。对外只返回
 * 不携带 JPA 实体的不可变视图，避免基础设施模型泄漏到接口层。</p>
 */
@Service
public class R2ClinicalApplicationService {
    /** 预约创建幂等键的业务作用域，防止同一键跨操作复用。 */
    private static final String BOOK_SCOPE = "SCHED_APPOINTMENT_CREATE";
    private final ScheduleSessionRepository sessions;
    private final ScheduleSlotRepository slots;
    private final ScheduleAppointmentRepository appointments;
    private final CheckInRepository checkIns;
    private final QueueEntryRepository queues;
    private final ScheduleEventRepository events;
    private final ClinicalEncounterRepository encounters;
    private final ClinicalNoteRepository notes;
    private final ClinicalDiagnosisRepository diagnoses;
    private final ClinicalDocumentVersionRepository documentVersions;
    private final IdempotencyRecordRepository idempotency;
    private final JdbcTemplate jdbc;

    R2ClinicalApplicationService(ScheduleSessionRepository sessions, ScheduleSlotRepository slots,
                                 ScheduleAppointmentRepository appointments, CheckInRepository checkIns,
                                 QueueEntryRepository queues, ScheduleEventRepository events,
                                 ClinicalEncounterRepository encounters, ClinicalNoteRepository notes,
                                 ClinicalDiagnosisRepository diagnoses,
                                 ClinicalDocumentVersionRepository documentVersions,
                                 IdempotencyRecordRepository idempotency, JdbcTemplate jdbc) {
        this.sessions = sessions; this.slots = slots; this.appointments = appointments; this.checkIns = checkIns;
        this.queues = queues; this.events = events; this.encounters = encounters; this.notes = notes;
        this.diagnoses = diagnoses; this.documentVersions = documentVersions; this.idempotency = idempotency;
        this.jdbc = jdbc;
    }

    /** 校验医生任职和时段后创建排班，并按固定分钟数切分号源。 */
    @Transactional
    public SessionView openSession(long staffId, SessionCommand command) {
        if (command.startsAt() == null || command.endsAt() == null || !command.endsAt().isAfter(command.startsAt())) {
            throw new IllegalArgumentException("排班结束时间必须晚于开始时间");
        }
        if (!command.startsAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("只能创建未来排班");
        if (command.slotMinutes() < 5 || command.slotMinutes() > 120) throw new IllegalArgumentException("号源时长必须为 5-120 分钟");
        requireDoctorAssignment(staffId, command.siteId(), command.departmentId(),
                command.startsAt(), command.endsAt());
        ScheduleSessionEntity session = sessions.save(new ScheduleSessionEntity(command.siteId(), command.departmentId(),
                staffId, command.startsAt(), command.endsAt()));
        List<Long> ids = new ArrayList<>();
        LocalDateTime cursor = command.startsAt();
        while (!cursor.plusMinutes(command.slotMinutes()).isAfter(command.endsAt())) {
            ids.add(slots.save(new ScheduleSlotEntity(session.id, cursor,
                    cursor.plusMinutes(command.slotMinutes()))).id);
            cursor = cursor.plusMinutes(command.slotMinutes());
        }
        if (ids.isEmpty()) throw new IllegalArgumentException("排班时间不足以生成号源");
        return new SessionView(session.id, staffId, List.copyOf(ids), session.version);
    }

    /** 查询当前时刻之后仍开放的号源，按开始时间排序。 */
    @Transactional(readOnly = true)
    public List<SlotView> availableSlots() {
        return jdbc.query("select sl.id,sl.session_id,sl.starts_at,sl.ends_at,sl.status,sl.version," +
                        "ss.site_id,ss.department_id,ss.staff_profile_id,sp.name staff_name,d.name department_name " +
                        "from sched_slot sl join sched_session ss on ss.id=sl.session_id " +
                        "join staff_profile sp on sp.id=ss.staff_profile_id left join department d on d.id=ss.department_id " +
                        "where sl.status='OPEN' and sl.starts_at>? order by sl.starts_at",
                (rs, row) -> new SlotView(rs.getLong("id"), rs.getLong("session_id"),
                        rs.getObject("starts_at", LocalDateTime.class), rs.getObject("ends_at", LocalDateTime.class),
                        rs.getString("status"), rs.getLong("version"), rs.getLong("site_id"),
                        rs.getLong("department_id"), rs.getString("department_name"),
                        rs.getLong("staff_profile_id"), rs.getString("staff_name"), 1), LocalDateTime.now());
    }

    /** 查询当前居民的预约，供居民门户与业务中心共享同一事实来源。 */
    @Transactional(readOnly = true)
    public List<ScheduledAppointmentView> residentAppointments(long patientId) {
        return appointmentViews("where a.patient_id=?", patientId);
    }

    /** 查询当前工作人员可办理的预约；医生仅看本人排班，其他岗位按有效站点任职过滤。 */
    @Transactional(readOnly = true)
    public List<ScheduledAppointmentView> staffAppointments(long staffId, boolean ownScheduleOnly) {
        if (ownScheduleOnly) return appointmentViews("where ss.staff_profile_id=?", staffId);
        return appointmentViews("where exists (select 1 from staff_site_assignment sa "
                + "where sa.staff_profile_id=? and sa.site_id=ss.site_id and sa.active=true "
                + "and sa.valid_from<=current_timestamp and (sa.valid_to is null or sa.valid_to>current_timestamp))", staffId);
    }

    /** 返回当前工作人员范围内的候诊队列，前端直接选择记录办理，无需手工输入预约编号。 */
    @Transactional(readOnly = true)
    public List<QueueView> staffQueue(long staffId, boolean ownScheduleOnly) {
        return staffAppointments(staffId, ownScheduleOnly).stream()
                .filter(item -> "CHECKED_IN".equals(item.status()) || "IN_PROGRESS".equals(item.status()))
                .map(item -> new QueueView(item.id(), item.id(), "Q" + item.queueNumber(), item.patientId(),
                        item.patientName(), item.status(), item.scheduledAt(), item.queueNumber(),
                        item.queueCreatedAt() == null ? 0 : Math.max(0,
                                Duration.between(item.queueCreatedAt(), LocalDateTime.now()).toMinutes()),
                        item.encounterId()))
                .toList();
    }

    /** 返回医生本人的接诊记录，供处方等后续业务选择已签署接诊。 */
    @Transactional(readOnly = true)
    public List<EncounterView> staffEncounters(long staffId) {
        return jdbc.queryForList("select id from clinical_encounter where staff_profile_id=? order by started_at desc limit 100", staffId)
                .stream().map(row -> view(encounters.findById(((Number) row.get("id")).longValue()).orElseThrow())).toList();
    }

    private List<ScheduledAppointmentView> appointmentViews(String where, long subjectId) {
        String sql = "select a.id,a.slot_id,a.patient_id,a.status,a.reason,a.version," +
                "s.starts_at,s.ends_at,ss.site_id,ss.department_id,ss.staff_profile_id," +
                "p.name patient_name,sp.name staff_name,d.name department_name," +
                "q.queue_number,q.created_at queue_created_at,e.id encounter_id " +
                "from sched_appointment a join sched_slot s on s.id=a.slot_id " +
                "join sched_session ss on ss.id=s.session_id join patient p on p.id=a.patient_id " +
                "join staff_profile sp on sp.id=ss.staff_profile_id " +
                "left join department d on d.id=ss.department_id " +
                "left join sched_queue_entry q on q.appointment_id=a.id " +
                "left join clinical_encounter e on e.appointment_id=a.id " + where + " order by s.starts_at desc limit 100";
        return jdbc.query(sql, (rs, row) -> new ScheduledAppointmentView(
                rs.getLong("id"), rs.getLong("slot_id"), rs.getLong("patient_id"),
                rs.getString("patient_name"), rs.getLong("staff_profile_id"), rs.getString("staff_name"),
                rs.getLong("site_id"), rs.getLong("department_id"), rs.getString("department_name"),
                rs.getObject("starts_at", LocalDateTime.class), rs.getObject("ends_at", LocalDateTime.class),
                rs.getString("status"), rs.getString("reason"), rs.getLong("version"),
                (Integer) rs.getObject("queue_number"), rs.getObject("queue_created_at", LocalDateTime.class),
                (Long) rs.getObject("encounter_id")), subjectId);
    }

    /**
     * 以居民、操作类型和幂等键为边界预约号源。
     *
     * <p>号源通过条件更新原子抢占；同一键与同一请求可安全重放，若请求内容不同则拒绝，
     * 从而避免网络重试造成重复预约。</p>
     */
    @Transactional
    public BookingResult book(long patientId, String rawKey, BookAppointmentCommand command) {
        IdempotencyKey key = IdempotencyKey.of(rawKey);
        String actor = String.valueOf(patientId);
        String requestHash = sha256(patientId + "|" + command.slotId() + "|" + normalized(command.reason()));
        var replay = idempotency.findByOperationScopeAndActorIdAndIdempotencyKey(BOOK_SCOPE, actor, key.value());
        if (replay.isPresent()) {
            if (!replay.get().requestHash.equals(requestHash)) {
                throw new R2ConflictException("同一幂等键不能用于不同预约请求");
            }
            return new BookingResult(view(appointment(Long.parseLong(replay.get().resourceId))), true);
        }
        if (slots.reserveIfOpen(command.slotId()) != 1) throw new R2ConflictException("号源已被占用或不可预约");
        ScheduleAppointmentEntity created = appointments.saveAndFlush(
                new ScheduleAppointmentEntity(command.slotId(), patientId, normalized(command.reason())));
        events.save(new ScheduleEventEntity(created.id, "APPOINTMENT_CREATED", "patient:" + patientId));
        try {
            idempotency.saveAndFlush(new IdempotencyRecordEntity(BOOK_SCOPE, actor, key.value(), requestHash, created.id));
        } catch (DataIntegrityViolationException ex) {
            throw new R2ConflictException("预约请求正在处理，请稍后使用同一幂等键重试");
        }
        return new BookingResult(view(created), false);
    }

    /** 取消居民本人预约并释放号源，同时记录状态事件。 */
    @Transactional
    public AppointmentView cancel(long patientId, long appointmentId) {
        ScheduleAppointmentEntity appointment = appointments.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new EntityNotFoundException("预约不存在"));
        appointment.cancel();
        slots.findById(appointment.slotId).orElseThrow().release();
        events.save(new ScheduleEventEntity(appointment.id, "APPOINTMENT_CANCELLED", "patient:" + patientId));
        return view(appointments.saveAndFlush(appointment));
    }

    /** 校验护士站点任职后完成签到并分配当前排班的下一个队列号。 */
    @Transactional
    public AppointmentView checkIn(long nurseStaffId, long appointmentId) {
        ScheduleAppointmentEntity appointment = appointment(appointmentId);
        ScheduleSlotEntity slot = slots.findById(appointment.slotId)
                .orElseThrow(() -> new EntityNotFoundException("号源不存在"));
        ScheduleSessionEntity session = sessions.findById(slot.sessionId)
                .orElseThrow(() -> new EntityNotFoundException("排班不存在"));
        requireNurseAssignment(nurseStaffId, session.siteId);
        appointment.transition(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);
        int queueNumber = queues.maximumQueueNumber(slot.sessionId) + 1;
        checkIns.save(new CheckInEntity(appointment.id, nurseStaffId));
        queues.save(new QueueEntryEntity(appointment.id, slot.sessionId, queueNumber));
        events.save(new ScheduleEventEntity(appointment.id, "CHECKED_IN", "staff:" + nurseStaffId));
        return view(appointments.saveAndFlush(appointment));
    }

    /** 仅允许该排班医生开始接诊，并将预约和号源推进到就诊中状态。 */
    @Transactional
    public EncounterView startEncounter(long doctorStaffId, long appointmentId) {
        ScheduleAppointmentEntity appointment = appointment(appointmentId);
        ScheduleSlotEntity slot = slots.findById(appointment.slotId)
                .orElseThrow(() -> new EntityNotFoundException("号源不存在"));
        ScheduleSessionEntity session = sessions.findById(slot.sessionId)
                .orElseThrow(() -> new EntityNotFoundException("排班不存在"));
        if (!session.staffProfileId.equals(doctorStaffId)) throw new EntityNotFoundException("预约不存在");
        appointment.transition(AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS);
        slot.use();
        ClinicalEncounterEntity encounter = encounters.saveAndFlush(
                new ClinicalEncounterEntity(appointment.id, appointment.patientId, doctorStaffId));
        events.save(new ScheduleEventEntity(appointment.id, "ENCOUNTER_STARTED", "staff:" + doctorStaffId));
        return view(encounter);
    }

    /** 保存尚未签署的 SOAP 草稿；版本不一致时要求客户端刷新。 */
    @Transactional
    public EncounterView saveDraft(long doctorStaffId, long encounterId, DraftCommand command) {
        ClinicalEncounterEntity encounter = ownedEncounter(doctorStaffId, encounterId);
        requireVersion(encounter.version, command.version());
        String body = requireText(command.body(), "病历正文不能为空");
        ClinicalNoteEntity note = notes.findByEncounterIdAndNoteType(encounterId, "SOAP")
                .orElseGet(() -> new ClinicalNoteEntity(encounterId, body));
        note.change(body);
        notes.save(note);
        encounter.touch();
        return view(encounters.saveAndFlush(encounter));
    }

    /** 为医生本人尚未签署的接诊记录添加结构化诊断。 */
    @Transactional
    public DiagnosisView addDiagnosis(long doctorStaffId, long encounterId, DiagnosisCommand command) {
        ClinicalEncounterEntity encounter = ownedEncounter(doctorStaffId, encounterId);
        encounter.requireDraft();
        ClinicalDiagnosisEntity diagnosis = diagnoses.save(new ClinicalDiagnosisEntity(encounterId,
                requireText(command.code(), "诊断编码不能为空"), requireText(command.name(), "诊断名称不能为空"),
                requireText(command.type(), "诊断类型不能为空")));
        return new DiagnosisView(diagnosis.id, encounterId, diagnosis.diagnosisCode,
                diagnosis.diagnosisName, diagnosis.diagnosisType);
    }

    /**
     * 签署完整病历，完成预约并保存内容哈希可校验的不可变文档快照。
     *
     * <p>签署要求正文和至少一条诊断均已存在，同时写入临床审计事件。</p>
     */
    @Transactional
    public EncounterView sign(long doctorStaffId, String actor, long encounterId, SignCommand command) {
        ClinicalEncounterEntity encounter = ownedEncounter(doctorStaffId, encounterId);
        requireVersion(encounter.version, command.version());
        ClinicalNoteEntity note = notes.findByEncounterIdAndNoteType(encounterId, "SOAP")
                .orElseThrow(() -> new R2ConflictException("签署前必须保存病历正文"));
        List<ClinicalDiagnosisEntity> encounterDiagnoses = diagnoses.findByEncounterIdOrderByIdAsc(encounterId);
        if (encounterDiagnoses.isEmpty()) throw new R2ConflictException("签署前必须添加诊断");
        encounter.sign();
        ScheduleAppointmentEntity appointment = appointment(encounter.appointmentId);
        appointment.transition(AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED);
        String immutableDocument = immutableDocument(note.body, encounterDiagnoses);
        documentVersions.save(new ClinicalDocumentVersionEntity(encounter.id, immutableDocument,
                sha256(immutableDocument), doctorStaffId));
        jdbc.update("insert into audit_event(occurred_at,actor,actor_role,action,resource_type,resource_id,outcome,purpose,details_json,correlation_id) values(current_timestamp,?,?,?,?,?,'SUCCESS','CLINICAL_CARE','{}',?)",
                actor, "DOCTOR", "ENCOUNTER_SIGNED", "CLINICAL_ENCOUNTER", String.valueOf(encounter.id),
                RequestCorrelationFilter.current());
        encounters.saveAndFlush(encounter);
        appointments.saveAndFlush(appointment);
        return view(encounter);
    }

    /** 按医生所有权查找接诊记录；不存在或越权统一表现为未找到，避免泄露数据。 */
    private ClinicalEncounterEntity ownedEncounter(long staffId, long encounterId) {
        ClinicalEncounterEntity encounter = encounters.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("接诊记录不存在"));
        if (!encounter.staffProfileId.equals(staffId)) throw new EntityNotFoundException("接诊记录不存在");
        return encounter;
    }

    private ScheduleAppointmentEntity appointment(long id) {
        return appointments.findById(id).orElseThrow(() -> new EntityNotFoundException("预约不存在"));
    }

    /** 执行业务层乐观锁前置校验，为客户端提供明确冲突提示。 */
    private void requireVersion(long actual, long expected) {
        if (actual != expected) throw new R2ConflictException("数据版本已变化，请刷新后重试");
    }

    /** 校验医生在整个排班时段内拥有有效的站点、科室和角色任职。 */
    private void requireDoctorAssignment(long staffId, long siteId, long departmentId,
                                         LocalDateTime startsAt, LocalDateTime endsAt) {
        Integer count = jdbc.queryForObject("select count(*) from staff_site_assignment a "
                        + "join staff_profile sp on sp.id=a.staff_profile_id "
                        + "join site s on s.id=a.site_id "
                        + "join department d on d.id=a.department_id "
                        + "where a.staff_profile_id=? and a.site_id=? and (a.department_id=? or a.department_id is null) "
                        + "and a.role_code='DOCTOR' and a.active=true and sp.active=true "
                        + "and sp.account_status='ACTIVE' and s.active=true and d.active=true "
                        + "and sp.organization_id=s.organization_id and d.site_id=s.id "
                        + "and a.valid_from<=? and (a.valid_to is null or a.valid_to>=?)",
                Integer.class, staffId, siteId, departmentId, startsAt, endsAt);
        if (count == null || count == 0) {
            throw new AccessDeniedException("医生未在该站点科室的排班时段内任职");
        }
    }

    /** 校验护士当前在预约所属站点具有有效任职。 */
    private void requireNurseAssignment(long staffId, long siteId) {
        LocalDateTime now = LocalDateTime.now();
        Integer count = jdbc.queryForObject("select count(*) from staff_site_assignment a "
                        + "join staff_profile sp on sp.id=a.staff_profile_id "
                        + "join site s on s.id=a.site_id "
                        + "where a.staff_profile_id=? and a.site_id=? and a.role_code='NURSE' "
                        + "and a.active=true and sp.active=true and sp.account_status='ACTIVE' "
                        + "and s.active=true and sp.organization_id=s.organization_id "
                        + "and a.valid_from<=? and (a.valid_to is null or a.valid_to>?)",
                Integer.class, staffId, siteId, now, now);
        if (count == null || count == 0) {
            throw new AccessDeniedException("护士未在该预约站点的有效任职范围内");
        }
    }

    /** 以带长度前缀的确定性格式组装病历快照，避免字段拼接歧义。 */
    private static String immutableDocument(String noteBody, List<ClinicalDiagnosisEntity> diagnoses) {
        StringBuilder document = new StringBuilder();
        appendField(document, "SOAP", noteBody);
        for (ClinicalDiagnosisEntity diagnosis : diagnoses) {
            appendField(document, "DIAGNOSIS_CODE", diagnosis.diagnosisCode);
            appendField(document, "DIAGNOSIS_NAME", diagnosis.diagnosisName);
            appendField(document, "DIAGNOSIS_TYPE", diagnosis.diagnosisType);
        }
        return document.toString();
    }

    private static void appendField(StringBuilder target, String name, String value) {
        target.append(name).append(':').append(value.length()).append(':').append(value).append('\n');
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private static String normalized(String value) { return value == null ? "" : value.trim(); }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }

    private static AppointmentView view(ScheduleAppointmentEntity a) {
        return new AppointmentView(a.id, a.slotId, a.patientId, a.status.name(), a.reason, a.version);
    }
    private static EncounterView view(ClinicalEncounterEntity e) {
        return new EncounterView(e.id, e.appointmentId, e.patientId, e.staffProfileId,
                e.status.name(), e.version, e.startedAt, e.signedAt);
    }

    /** 创建排班的应用层命令。 */
    public record SessionCommand(long siteId, long departmentId, LocalDateTime startsAt,
                                 LocalDateTime endsAt, int slotMinutes) {}
    /** 已创建排班及其号源标识视图。 */
    public record SessionView(long id, long staffProfileId, List<Long> slotIds, long version) {}
    /** 居民可见号源视图。 */
    public record SlotView(long id, long sessionId, LocalDateTime startsAt, LocalDateTime endsAt,
                           String status, long version, long siteId, long departmentId,
                           String departmentName, long staffProfileId, String staffName, int remaining) {}
    /** 预约号源命令。 */
    public record BookAppointmentCommand(long slotId, String reason) {}
    /** 预约状态视图。 */
    public record AppointmentView(long id, long slotId, long patientId, String status, String reason, long version) {}
    /** 门户展示使用的预约视图，包含排班、居民、医护和候诊关联信息。 */
    public record ScheduledAppointmentView(long id, long slotId, long patientId, String patientName,
                                           long staffProfileId, String staffName, long siteId,
                                           long departmentId, String departmentName,
                                           LocalDateTime scheduledAt, LocalDateTime endsAt,
                                           String status, String reason, long version,
                                           Integer queueNumber, LocalDateTime queueCreatedAt,
                                           Long encounterId) {}
    /** 候诊队列视图。 */
    public record QueueView(long id, long appointmentId, String ticketNo, long patientId,
                            String residentName, String status, LocalDateTime scheduledAt,
                            Integer queueNumber, long waitingMinutes, Long encounterId) {}
    /** 预约结果，{@code replay} 表示响应来自幂等重放。 */
    public record BookingResult(AppointmentView appointment, boolean replay) {}
    /** 保存病历草稿命令。 */
    public record DraftCommand(String body, long version) {}
    /** 新增诊断命令。 */
    public record DiagnosisCommand(String code, String name, String type) {}
    /** 诊断视图。 */
    public record DiagnosisView(long id, long encounterId, String code, String name, String type) {}
    /** 病历签署命令。 */
    public record SignCommand(long version) {}
    /** 接诊记录状态视图。 */
    public record EncounterView(long id, long appointmentId, long patientId, long staffProfileId,
                                String status, long version, LocalDateTime startedAt, LocalDateTime signedAt) {}
}
