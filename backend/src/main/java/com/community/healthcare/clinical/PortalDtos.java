package com.community.healthcare.clinical;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 预约状态变更请求。 */
record AppointmentStatusRequest(@NotNull AppointmentStatus status) {}

/** 管理端创建或更新预约的输入。 */
record AppointmentWriteRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        @NotNull LocalDateTime scheduledAt,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 500) String remark) {}

/** 汇总库存调整量；正数入库、负数出库。 */
record StockAdjustmentRequest(@NotNull Integer delta) {}

/** 医护为服务范围内居民登记的健康测量输入。 */
record StaffHealthRecordRequest(
        @NotNull Long patientId,
        LocalDateTime recordedAt,
        @Min(20) @Max(250) Integer heartRate,
        @Min(40) @Max(260) Integer systolicPressure,
        @Min(30) @Max(180) Integer diastolicPressure,
        @Min(50) @Max(100) Integer bloodOxygen,
        @DecimalMin("20.0") @DecimalMax("250.0") BigDecimal weight,
        @Size(max = 500) String note) {}

/** 居民本人登记的自测健康数据输入。 */
record ResidentHealthRecordRequest(
        LocalDateTime recordedAt,
        @Min(20) @Max(250) Integer heartRate,
        @Min(40) @Max(260) Integer systolicPressure,
        @Min(30) @Max(180) Integer diastolicPressure,
        @Min(50) @Max(100) Integer bloodOxygen,
        @DecimalMin("20.0") @DecimalMax("250.0") BigDecimal weight,
        @Size(max = 500) String note) {}

/** 居民预约未来线下服务的输入。 */
record ResidentAppointmentRequest(
        @NotNull Long doctorId,
        @NotNull LocalDateTime scheduledAt,
        @NotBlank @Size(max = 500) String reason) {}

/** 居民可自行维护的基础联系信息。 */
record ResidentProfileRequest(
        @Size(min = 1, max = 64) String name,
        @Size(max = 16) String gender,
        LocalDate birthDate,
        @Size(max = 32) String phone,
        @Size(max = 255) String address) {}

/** 面向门户的居民响应，证件字段始终为脱敏快照。 */
record PatientResponse(
        Long id,
        String name,
        String gender,
        LocalDate birthDate,
        String phone,
        String address,
        String maskedIdCard,
        BigDecimal balance,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {
    static PatientResponse from(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getName(), patient.getGender(),
                patient.getBirthDate(), patient.getPhone(), patient.getAddress(), patient.getIdCard(), patient.getBalance(),
                patient.isActive(), patient.getCreatedAt(), patient.getUpdatedAt(), patient.getVersion());
    }
}

/** 面向门户的医护公开档案响应。 */
record DoctorResponse(
        Long id,
        String employeeNo,
        String name,
        String department,
        String title,
        String specialty,
        String phone,
        String scheduleSummary,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {
    static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getEmployeeNo(), doctor.getName(), doctor.getDepartment(),
                doctor.getTitle(), doctor.getSpecialty(), doctor.getPhone(), doctor.getScheduleSummary(),
                doctor.isActive(), doctor.getCreatedAt(), doctor.getUpdatedAt(), doctor.getVersion());
    }
}

/** 预约响应；姓名和科室是按关联 ID 查询得到的当前展示快照。 */
record AppointmentResponse(
        Long id,
        String appointmentNo,
        Long patientId,
        Long doctorId,
        String patientName,
        String doctorName,
        String department,
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String reason,
        String remark,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {
    static AppointmentResponse from(Appointment appointment, String patientName,
                                    String doctorName, String department) {
        return new AppointmentResponse(appointment.getId(), appointment.getAppointmentNo(),
                appointment.getPatientId(), appointment.getDoctorId(), patientName, doctorName, department,
                appointment.getScheduledAt(), appointment.getStatus(), appointment.getReason(),
                appointment.getRemark(), appointment.isActive(), appointment.getCreatedAt(),
                appointment.getUpdatedAt(), appointment.getVersion());
    }
}

/** 健康测量响应，附带居民姓名展示快照。 */
record HealthRecordResponse(
        Long id,
        Long patientId,
        String patientName,
        LocalDateTime recordedAt,
        Integer heartRate,
        Integer systolicPressure,
        Integer diastolicPressure,
        Integer bloodOxygen,
        BigDecimal weight,
        String note,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {
    static HealthRecordResponse from(HealthRecord record, String patientName) {
        return new HealthRecordResponse(record.getId(), record.getPatientId(), patientName,
                record.getRecordedAt(), record.getHeartRate(), record.getSystolicPressure(),
                record.getDiastolicPressure(), record.getBloodOxygen(), record.getWeight(), record.getNote(),
                record.isActive(), record.getCreatedAt(), record.getUpdatedAt(), record.getVersion());
    }
}

/** 慢病档案响应，附带居民和责任医生展示快照。 */
record ChronicCaseResponse(
        Long id,
        Long patientId,
        String patientName,
        String diseaseType,
        String riskLevel,
        LocalDate diagnosisDate,
        Long doctorId,
        String doctorName,
        String department,
        String managementPlan,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {
    static ChronicCaseResponse from(ChronicCase chronicCase, String patientName,
                                    String doctorName, String department) {
        return new ChronicCaseResponse(chronicCase.getId(), chronicCase.getPatientId(), patientName,
                chronicCase.getDiseaseType(), chronicCase.getRiskLevel(), chronicCase.getDiagnosisDate(),
                chronicCase.getDoctorId(), doctorName, department, chronicCase.getManagementPlan(),
                chronicCase.isActive(), chronicCase.getCreatedAt(), chronicCase.getUpdatedAt(),
                chronicCase.getVersion());
    }
}

/** 药品目录和汇总库存响应。 */
record MedicineResponse(
        Long id,
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        Integer minimumStock,
        String specification,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {
    static MedicineResponse from(Medicine medicine) {
        return new MedicineResponse(medicine.getId(), medicine.getName(), medicine.getCategory(),
                medicine.getPrice(), medicine.getStock(), medicine.getMinimumStock(),
                medicine.getSpecification(), medicine.isActive(), medicine.getCreatedAt(),
                medicine.getUpdatedAt(), medicine.getVersion());
    }
}

/** 医护首页在当前服务范围内的聚合指标。 */
record StaffSummaryResponse(
        DoctorResponse staff,
        DoctorResponse staffProfile,
        long patients,
        long appointmentsToday,
        long pendingAppointments,
        long completedToday,
        long chronicCases,
        long lowStockMedicines) {}

/** 居民首页所需的个人档案、数量指标和最近记录聚合。 */
record ResidentOverviewResponse(
        PatientResponse profile,
        long pendingAppointments,
        long healthRecordCount,
        long chronicPlanCount,
        AppointmentResponse nextAppointment,
        HealthRecordResponse latestHealthRecord) {}
