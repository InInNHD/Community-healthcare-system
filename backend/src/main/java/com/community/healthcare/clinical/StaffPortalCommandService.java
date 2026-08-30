package com.community.healthcare.clinical;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 编排医护门户中的居民健康数据写操作。
 *
 * <p>写入前重新计算当前工作人员的居民范围，不能因为前端已展示居民就跳过服务端授权。</p>
 */
@Service
class StaffPortalCommandService {
    private final PatientRepository patients;
    private final HealthRecordRepository healthRecords;
    private final PortalResponseMapper mapper;
    private final StaffPortalQueryService queries;

    StaffPortalCommandService(PatientRepository patients, HealthRecordRepository healthRecords,
                              PortalResponseMapper mapper, StaffPortalQueryService queries) {
        this.patients = patients;
        this.healthRecords = healthRecords;
        this.mapper = mapper;
        this.queries = queries;
    }

    /** 为当前服务范围内的居民登记测量记录。 */
    @Transactional
    HealthRecordResponse createHealthRecord(StaffAccessScope scope, StaffHealthRecordRequest request) {
        if (!queries.scopedPatientIds(scope).contains(request.patientId())) {
            throw new EntityNotFoundException("居民不存在");
        }
        Patient patient = patients.findById(request.patientId())
                .orElseThrow(() -> new EntityNotFoundException("居民不存在"));
        HealthRecord record = new HealthRecord();
        record.setPatientId(patient.getId());
        record.setRecordedAt(request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt());
        record.setHeartRate(request.heartRate());
        record.setSystolicPressure(request.systolicPressure());
        record.setDiastolicPressure(request.diastolicPressure());
        record.setBloodOxygen(request.bloodOxygen());
        record.setWeight(request.weight());
        record.setNote(request.note());
        return mapper.healthRecord(healthRecords.save(record));
    }
}
