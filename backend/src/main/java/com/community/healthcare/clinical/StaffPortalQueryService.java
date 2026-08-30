package com.community.healthcare.clinical;

import com.community.healthcare.shared.api.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.community.healthcare.residentregistry.application.RegistryStore;
import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 在工作人员服务范围内聚合预约、居民、健康记录和慢病信息。
 *
 * <p>基础范围来自有效站点派驻与居民站点登记；医生再与本人既有预约居民求交集。
 * 无范围时返回空页并记录拒绝审计，不回退为全量数据。</p>
 */
@Service
@Transactional(readOnly = true)
class StaffPortalQueryService {
    private static final Sort APPOINTMENT_SORT = Sort.by("scheduledAt").descending();
    private static final Sort PATIENT_SORT = Sort.by("createdAt").descending();
    private static final Sort HEALTH_RECORD_SORT = Sort.by("recordedAt").descending();
    private static final Sort CHRONIC_CASE_SORT = Sort.by("createdAt").descending();

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final HealthRecordRepository healthRecords;
    private final ChronicCaseRepository chronicCases;
    private final MedicineRepository medicines;
    private final PortalResponseMapper mapper;
    private final RegistryStore registry;
    private final Clock clock;
    private final AuditTrail audit;

    StaffPortalQueryService(PatientRepository patients, DoctorRepository doctors,
                            AppointmentRepository appointments, HealthRecordRepository healthRecords,
                            ChronicCaseRepository chronicCases, MedicineRepository medicines,
                            PortalResponseMapper mapper, RegistryStore registry, Clock clock, AuditTrail audit) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.healthRecords = healthRecords;
        this.chronicCases = chronicCases;
        this.medicines = medicines;
        this.mapper = mapper;
        this.registry = registry;
        this.clock = clock;
        this.audit = audit;
    }

    StaffSummaryResponse summary(StaffAccessScope scope) {
        Doctor staff = doctors.findById(scope.staffId())
                .orElseThrow(() -> new EntityNotFoundException("医护档案不可用"));
        LocalDate today = LocalDate.now();
        var start = today.atStartOfDay();
        var end = today.plusDays(1).atStartOfDay();
        DoctorResponse profile = DoctorResponse.from(staff);
        Set<Long> scopedIds = scopedPatientIds(scope);
        long todayCount = scopedIds.isEmpty() ? 0 : appointments.countByPatientIdInAndScheduledAtBetween(scopedIds, start, end);
        long pendingCount = scopedIds.isEmpty() ? 0 : appointments.countByPatientIdInAndStatus(scopedIds, AppointmentStatus.PENDING);
        long completedToday = scopedIds.isEmpty() ? 0 : appointments.countByPatientIdInAndScheduledAtBetweenAndStatus(
                scopedIds, start, end, AppointmentStatus.COMPLETED);
        audit(scope, "STAFF_SUMMARY_READ", scopedIds.isEmpty() ? "DENIED" : "SUCCESS");
        return new StaffSummaryResponse(profile, profile, scopedIds.size(),
                todayCount, pendingCount, completedToday,
                scopedIds.isEmpty() ? 0 : chronicCases.countByPatientIdInAndActiveTrue(scopedIds), medicines.countLowStock());
    }

    PageResponse<AppointmentResponse> appointments(StaffAccessScope scope, boolean today,
                                                   String keyword, int page, int size) {
        PageRequest pageable = PortalPageRequests.of(page, size, APPOINTMENT_SORT);
        Page<Appointment> result;
        if (today) {
            LocalDate date = LocalDate.now();
            Set<Long> ids = scopedPatientIds(scope);
            result = scope.appointmentDoctorId() == null
                    ? ids.isEmpty() ? Page.empty(pageable) : appointments.findByPatientIdInAndScheduledAtBetween(
                            ids, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), pageable)
                    : ids.isEmpty() ? Page.empty(pageable) : appointments.findByDoctorIdAndPatientIdInAndScheduledAtBetween(
                            scope.appointmentDoctorId(), ids, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), pageable);
        } else {
            Set<Long> ids = scopedPatientIds(scope);
            result = scope.appointmentDoctorId() == null
                    ? ids.isEmpty() ? Page.empty(pageable) : appointments.searchByPatientIds(ids, normalize(keyword), pageable)
                    : ids.isEmpty() ? Page.empty(pageable) : appointments.searchByDoctorIdAndPatientIds(
                            scope.appointmentDoctorId(), ids, normalize(keyword), pageable);
        }
        return PortalPages.from(result, mapper.appointments(result.getContent()));
    }

    PageResponse<PatientResponse> patients(StaffAccessScope scope, String keyword, int page, int size) {
        Set<Long> ids = scopedPatientIds(scope);
        Page<Patient> result = ids.isEmpty() ? Page.empty(PortalPageRequests.of(page, size, PATIENT_SORT))
                : patients.searchByIds(ids, normalize(keyword), PortalPageRequests.of(page, size, PATIENT_SORT));
        audit(scope, "STAFF_PATIENT_QUERY", ids.isEmpty() ? "DENIED" : "SUCCESS");
        return PortalPages.from(result, result.getContent().stream().map(PatientResponse::from).toList());
    }

    PageResponse<HealthRecordResponse> healthRecords(StaffAccessScope scope, Long patientId, int page, int size) {
        PageRequest pageable = PortalPageRequests.of(page, size, HEALTH_RECORD_SORT);
        Set<Long> ids = scopedPatientIds(scope);
        Page<HealthRecord> result = ids.isEmpty() || (patientId != null && !ids.contains(patientId))
                ? Page.empty(pageable) : patientId == null
                ? healthRecords.findByPatientIdIn(ids, pageable)
                : healthRecords.findByPatientId(patientId, pageable);
        audit(scope, "STAFF_HEALTH_RECORD_QUERY",
                ids.isEmpty() || (patientId != null && !ids.contains(patientId)) ? "DENIED" : "SUCCESS");
        return PortalPages.from(result, mapper.healthRecords(result.getContent()));
    }

    PageResponse<ChronicCaseResponse> chronicCases(StaffAccessScope scope, String keyword, int page, int size) {
        Set<Long> ids = scopedPatientIds(scope);
        Page<ChronicCase> result = ids.isEmpty() ? Page.empty(PortalPageRequests.of(page, size, CHRONIC_CASE_SORT))
                : chronicCases.searchByPatientIds(ids, normalize(keyword), PortalPageRequests.of(page, size, CHRONIC_CASE_SORT));
        audit(scope, "STAFF_CHRONIC_CASE_QUERY", ids.isEmpty() ? "DENIED" : "SUCCESS");
        return PortalPages.from(result, mapper.chronicCases(result.getContent()));
    }

    List<MedicineResponse> medicineAlerts() {
        return medicines.findLowStock().stream().map(MedicineResponse::from).toList();
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    /**
     * 计算当前工作人员可见居民集合。
     *
     * <p>优先解析新的 staffProfileId，兼容旧 doctorId；医生集合还需与本人预约关系求交集。</p>
     */
    Set<Long> scopedPatientIds(StaffAccessScope scope) {
        var profile = registry.resolveStaffProfileId(scope.staffProfileId(), scope.staffId());
        if (profile.isEmpty()) return Set.of();
        Set<Long> sitePatients = registry.scopedPatientIds(profile.get(), clock.instant());
        if (scope.appointmentDoctorId() == null) return sitePatients;
        Set<Long> associated = new LinkedHashSet<>(appointments.findDistinctPatientIdsByDoctorId(scope.appointmentDoctorId()));
        associated.retainAll(sitePatients);
        return associated;
    }

    private void audit(StaffAccessScope scope, String action, String outcome) {
        audit.append(new AuditEventCommand(scope.actor(), scope.role(), action, "PATIENT", "BULK",
                outcome, "CARE_DELIVERY", null, null));
    }
}
