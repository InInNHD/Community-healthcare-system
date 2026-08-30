package com.community.healthcare.clinical;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 编排居民本人可执行的档案、预约和健康数据写操作。
 *
 * <p>patientId 始终来自 JWT 而非请求正文；居民端只能预约未来线下服务，
 * 不提供在线诊断或处方创建能力。</p>
 */
@Service
class ResidentPortalCommandService {
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final HealthRecordRepository healthRecords;
    private final PortalResponseMapper mapper;
    private final AppointmentTimePolicy appointmentTimePolicy;

    ResidentPortalCommandService(PatientRepository patients, DoctorRepository doctors,
                                 AppointmentRepository appointments, HealthRecordRepository healthRecords,
                                 PortalResponseMapper mapper, AppointmentTimePolicy appointmentTimePolicy) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.healthRecords = healthRecords;
        this.mapper = mapper;
        this.appointmentTimePolicy = appointmentTimePolicy;
    }

    @Transactional
    PatientResponse updateProfile(Long patientId, ResidentProfileRequest request) {
        Patient patient = ownPatient(patientId);
        if (request.name() != null) patient.setName(request.name().trim());
        if (request.gender() != null) patient.setGender(request.gender());
        if (request.birthDate() != null) patient.setBirthDate(request.birthDate());
        if (request.phone() != null) patient.setPhone(request.phone());
        if (request.address() != null) patient.setAddress(request.address());
        return PatientResponse.from(patient);
    }

    /** 创建待确认预约，并用随机业务号避免客户端控制标识。 */
    @Transactional
    AppointmentResponse createAppointment(Long patientId, ResidentAppointmentRequest request) {
        appointmentTimePolicy.requireFuture(request.scheduledAt());
        ownPatient(patientId);
        Doctor doctor = doctors.findById(request.doctorId())
                .orElseThrow(() -> new EntityNotFoundException("医生不存在"));
        Appointment appointment = new Appointment();
        appointment.setAppointmentNo("AP" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctor.getId());
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setReason(request.reason().trim());
        appointment.setStatus(AppointmentStatus.PENDING);
        return mapper.appointment(appointments.save(appointment));
    }

    /** 登记居民自测健康数据；未提供时间时使用服务端当前时间。 */
    @Transactional
    HealthRecordResponse createHealthRecord(Long patientId, ResidentHealthRecordRequest request) {
        ownPatient(patientId);
        HealthRecord record = new HealthRecord();
        record.setPatientId(patientId);
        record.setRecordedAt(request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt());
        record.setHeartRate(request.heartRate());
        record.setSystolicPressure(request.systolicPressure());
        record.setDiastolicPressure(request.diastolicPressure());
        record.setBloodOxygen(request.bloodOxygen());
        record.setWeight(request.weight());
        record.setNote(request.note());
        return mapper.healthRecord(healthRecords.save(record));
    }

    private Patient ownPatient(Long patientId) {
        return patients.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("居民档案不可用"));
    }
}
