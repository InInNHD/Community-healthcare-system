package com.community.healthcare.clinical;

import com.community.healthcare.shared.api.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;

/**
 * 提供居民本人范围内的门户聚合查询。
 *
 * <p>每次敏感档案读取都会先确认居民记录可用并写入审计；查询只以 JWT 关联的 patientId 为边界。</p>
 */
@Service
@Transactional(readOnly = true)
class ResidentPortalQueryService {
    private static final List<AppointmentStatus> OPEN_APPOINTMENT_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final HealthRecordRepository healthRecords;
    private final ChronicCaseRepository chronicCases;
    private final PortalResponseMapper mapper;
    private final AuditTrail audit;

    ResidentPortalQueryService(PatientRepository patients, DoctorRepository doctors,
                               AppointmentRepository appointments, HealthRecordRepository healthRecords,
                               ChronicCaseRepository chronicCases, PortalResponseMapper mapper, AuditTrail audit) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.healthRecords = healthRecords;
        this.chronicCases = chronicCases;
        this.mapper = mapper;
        this.audit = audit;
    }

    ResidentOverviewResponse overview(Long patientId) {
        Patient profile = ownPatient(patientId, "RESIDENT_OVERVIEW_READ");
        AppointmentResponse nextAppointment = appointments
                .findFirstByPatientIdAndStatusInAndScheduledAtAfterOrderByScheduledAtAsc(
                        patientId, OPEN_APPOINTMENT_STATUSES, LocalDateTime.now())
                .map(mapper::appointment).orElse(null);
        HealthRecordResponse latestHealthRecord = healthRecords
                .findFirstByPatientIdOrderByRecordedAtDesc(patientId)
                .map(mapper::healthRecord).orElse(null);
        return new ResidentOverviewResponse(PatientResponse.from(profile),
                appointments.countByPatientIdAndStatusIn(patientId, OPEN_APPOINTMENT_STATUSES),
                healthRecords.countByPatientId(patientId),
                chronicCases.countByPatientIdAndActiveTrue(patientId),
                nextAppointment, latestHealthRecord);
    }

    PatientResponse profile(Long patientId) {
        return PatientResponse.from(ownPatient(patientId, "RESIDENT_PROFILE_READ"));
    }

    PageResponse<AppointmentResponse> appointments(Long patientId, int page, int size) {
        ownPatient(patientId, "RESIDENT_APPOINTMENT_QUERY");
        Page<Appointment> result = appointments.findByPatientId(patientId,
                PortalPageRequests.descending(page, size, "scheduledAt"));
        return PortalPages.from(result, mapper.appointments(result.getContent()));
    }

    PageResponse<HealthRecordResponse> healthRecords(Long patientId, int page, int size) {
        ownPatient(patientId, "RESIDENT_HEALTH_RECORD_QUERY");
        Page<HealthRecord> result = healthRecords.findByPatientId(patientId,
                PortalPageRequests.descending(page, size, "recordedAt"));
        return PortalPages.from(result, mapper.healthRecords(result.getContent()));
    }

    List<ChronicCaseResponse> chronicPlans(Long patientId) {
        ownPatient(patientId, "RESIDENT_CHRONIC_CASE_QUERY");
        return mapper.chronicCases(chronicCases.findByPatientIdAndActiveTrueOrderByDiagnosisDateDesc(patientId));
    }

    PageResponse<DoctorResponse> doctors(String keyword, int page, int size) {
        Page<Doctor> result = doctors.search(keyword == null ? "" : keyword.trim(),
                PortalPageRequests.descending(page, size, "name"));
        return PortalPages.from(result, result.getContent().stream().map(DoctorResponse::from).toList());
    }

    /** 对成功和拒绝结果均留痕，同时用统一错误隐藏档案存在性。 */
    private Patient ownPatient(Long patientId, String action) {
        var patient = patients.findById(patientId);
        audit.append(new AuditEventCommand("patient:" + patientId, "RESIDENT", action, "PATIENT",
                patientId.toString(), patient.isPresent() ? "SUCCESS" : "DENIED", "SELF_SERVICE", null, null));
        return patient.orElseThrow(() -> new EntityNotFoundException("居民档案不可用"));
    }
}
