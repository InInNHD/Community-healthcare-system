package com.community.healthcare.clinical;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 将业务 ID 映射为当前姓名快照。每种资源每页最多执行一次批量查询，避免逐条关联查询。
 * ID 始终是业务关系的唯一依据，姓名和科室只作为响应快照，不参与写入和权限判断。
 */
@Component
class PortalResponseMapper {
    private final PatientRepository patients;
    private final DoctorRepository doctors;

    PortalResponseMapper(PatientRepository patients, DoctorRepository doctors) {
        this.patients = patients;
        this.doctors = doctors;
    }

    List<AppointmentResponse> appointments(List<Appointment> source) {
        if (source.isEmpty()) return List.of();
        Map<Long, PatientNameSnapshot> patientNames = patientNames(
                source.stream().map(Appointment::getPatientId).toList());
        Map<Long, DoctorNameSnapshot> doctorNames = doctorNames(
                source.stream().map(Appointment::getDoctorId).toList());
        return source.stream().map(appointment -> {
            PatientNameSnapshot patient = patientNames.get(appointment.getPatientId());
            DoctorNameSnapshot doctor = doctorNames.get(appointment.getDoctorId());
            return AppointmentResponse.from(appointment, patient == null ? null : patient.getName(),
                    doctor == null ? null : doctor.getName(),
                    doctor == null ? null : doctor.getDepartment());
        }).toList();
    }

    AppointmentResponse appointment(Appointment source) {
        return appointments(List.of(source)).get(0);
    }

    List<HealthRecordResponse> healthRecords(List<HealthRecord> source) {
        if (source.isEmpty()) return List.of();
        Map<Long, PatientNameSnapshot> patientNames = patientNames(
                source.stream().map(HealthRecord::getPatientId).toList());
        return source.stream().map(record -> {
            PatientNameSnapshot patient = patientNames.get(record.getPatientId());
            return HealthRecordResponse.from(record, patient == null ? null : patient.getName());
        }).toList();
    }

    HealthRecordResponse healthRecord(HealthRecord source) {
        return healthRecords(List.of(source)).get(0);
    }

    List<ChronicCaseResponse> chronicCases(List<ChronicCase> source) {
        if (source.isEmpty()) return List.of();
        Map<Long, PatientNameSnapshot> patientNames = patientNames(
                source.stream().map(ChronicCase::getPatientId).toList());
        Map<Long, DoctorNameSnapshot> doctorNames = doctorNames(
                source.stream().map(ChronicCase::getDoctorId).filter(Objects::nonNull).toList());
        return source.stream().map(chronicCase -> {
            PatientNameSnapshot patient = patientNames.get(chronicCase.getPatientId());
            DoctorNameSnapshot doctor = chronicCase.getDoctorId() == null
                    ? null : doctorNames.get(chronicCase.getDoctorId());
            return ChronicCaseResponse.from(chronicCase, patient == null ? null : patient.getName(),
                    doctor == null ? null : doctor.getName(),
                    doctor == null ? null : doctor.getDepartment());
        }).toList();
    }

    private Map<Long, PatientNameSnapshot> patientNames(Collection<Long> ids) {
        Set<Long> uniqueIds = unique(ids);
        if (uniqueIds.isEmpty()) return Map.of();
        return patients.findNameSnapshots(uniqueIds).stream()
                .collect(Collectors.toMap(PatientNameSnapshot::getId, Function.identity()));
    }

    private Map<Long, DoctorNameSnapshot> doctorNames(Collection<Long> ids) {
        Set<Long> uniqueIds = unique(ids);
        if (uniqueIds.isEmpty()) return Map.of();
        return doctors.findNameSnapshots(uniqueIds).stream()
                .collect(Collectors.toMap(DoctorNameSnapshot::getId, Function.identity()));
    }

    private static Set<Long> unique(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptySet();
        return ids.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
