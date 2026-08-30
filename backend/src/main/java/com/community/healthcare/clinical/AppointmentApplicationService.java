package com.community.healthcare.clinical;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/**
 * 统一编排既有门户预约状态变更。
 *
 * <p>所有入口共享 {@link AppointmentStatusPolicy}，医护入口还先按医生或居民服务范围定位预约，
 * 未授权目标与不存在目标返回相同错误以避免数据枚举。</p>
 */
@Service
class AppointmentApplicationService {
    private final AppointmentRepository appointments;
    private final AppointmentStatusPolicy statusPolicy;
    private final PortalResponseMapper mapper;

    AppointmentApplicationService(AppointmentRepository appointments,
                                  AppointmentStatusPolicy statusPolicy,
                                  PortalResponseMapper mapper) {
        this.appointments = appointments;
        this.statusPolicy = statusPolicy;
        this.mapper = mapper;
    }

    @Transactional
    AppointmentResponse changeStatus(Long appointmentId, AppointmentStatus target) {
        Appointment appointment = activeAppointment(appointmentId);
        return transition(appointment, target);
    }

    /**
     * 在医护数据范围内变更预约状态。
     *
     * <p>医生按 doctorId 收窄，护士等角色按已计算的居民集合收窄。</p>
     */
    @Transactional
    AppointmentResponse changeStatusForStaff(Long appointmentId, AppointmentStatus target, Long doctorScope,
                                             Set<Long> patientScope) {
        Appointment appointment = doctorScope == null
                ? appointments.findByIdAndPatientIdIn(appointmentId, patientScope)
                    .orElseThrow(() -> new EntityNotFoundException("预约不存在"))
                : appointments.findByIdAndDoctorId(appointmentId, doctorScope)
                    .orElseThrow(() -> new EntityNotFoundException("预约不存在"));
        return transition(appointment, target);
    }

    private AppointmentResponse transition(Appointment appointment, AppointmentStatus target) {
        statusPolicy.check(appointment.getStatus(), target);
        appointment.setStatus(target);
        return mapper.appointment(appointment);
    }

    /** 仅取消属于当前居民且状态允许取消的预约。 */
    @Transactional
    AppointmentResponse cancelOwned(Long appointmentId, Long patientId) {
        Appointment appointment = appointments.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new EntityNotFoundException("预约不存在"));
        statusPolicy.check(appointment.getStatus(), AppointmentStatus.CANCELLED);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return mapper.appointment(appointment);
    }

    private Appointment activeAppointment(Long appointmentId) {
        return appointments.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("预约不存在"));
    }
}
