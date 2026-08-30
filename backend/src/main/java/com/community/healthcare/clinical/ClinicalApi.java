package com.community.healthcare.clinical;

import com.community.healthcare.shared.api.PageResponse;
import com.community.healthcare.residentregistry.application.RegistryApplicationService;
import com.community.healthcare.residentregistry.domain.ProtectedPatientIdentifier;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 管理端居民档案 CRUD；证件原值在保存前转换为哈希和脱敏快照。 */
@RestController
@RequestMapping("/api/patients")
class PatientController {
    private final PatientRepository repository;
    private final RegistryApplicationService registry;
    PatientController(PatientRepository repository, RegistryApplicationService registry) {
        this.repository = repository; this.registry = registry;
    }
    @GetMapping PageResponse<PatientResponse> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        String normalized = keyword.trim();
        var pageable = PortalPageRequests.descending(page, size, "createdAt");
        var identifierPatient = registry.patientIdByIdentifier("NATIONAL_ID", normalized);
        if (identifierPatient.isPresent()) {
            return PageResponse.of(repository.searchByIds(List.of(identifierPatient.get()), "", pageable).map(PatientResponse::from));
        }
        return PageResponse.of(repository.search(normalized, pageable).map(PatientResponse::from));
    }
    @GetMapping("/{id}") PatientResponse get(@PathVariable Long id) { return PatientResponse.from(find(id)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    PatientResponse create(@Valid @RequestBody Patient value, Principal principal) {
        if (value.getIdCard() == null || value.getIdCard().isBlank()) throw new IllegalArgumentException("证件号不能为空");
        ProtectedPatientIdentifier identifier = registry.protectIdentifier("NATIONAL_ID", value.getIdCard());
        value.setIdCard(identifier.maskedValue());
        Patient saved = repository.saveAndFlush(value);
        registry.addProtectedIdentifier(saved.getId(), identifier, principal.getName(), "ADMIN");
        return PatientResponse.from(saved);
    }
    @PutMapping("/{id}") @Transactional PatientResponse update(@PathVariable Long id,
            @Valid @RequestBody Patient value, Principal principal) {
        Patient current = find(id);
        ProtectedPatientIdentifier identifier = null;
        boolean identifierChanged = value.getIdCard() != null && !value.getIdCard().isBlank()
                && !value.getIdCard().startsWith("****") && !value.getIdCard().contains("*");
        if (identifierChanged) {
            identifier = registry.protectIdentifier("NATIONAL_ID", value.getIdCard());
            value.setIdCard(identifier.maskedValue());
        } else {
            value.setIdCard(null);
        }
        current.updateFrom(value);
        if (identifierChanged) {
            registry.addProtectedIdentifier(current.getId(), identifier, principal.getName(), "ADMIN");
        }
        return PatientResponse.from(current);
    }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize("hasRole('ADMIN')") void delete(@PathVariable Long id, Principal principal) { find(id).deactivate(principal.getName()); }
    private Patient find(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("患者不存在")); }
}

/** 管理端医护档案 CRUD。 */
@RestController
@RequestMapping("/api/doctors")
class DoctorController {
    private final DoctorRepository repository;
    DoctorController(DoctorRepository repository) { this.repository = repository; }
    @GetMapping PageResponse<Doctor> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(repository.search(keyword.trim(), PortalPageRequests.descending(page, size, "createdAt")));
    }
    @GetMapping("/{id}") Doctor get(@PathVariable Long id) { return find(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Doctor create(@Valid @RequestBody Doctor value) { return repository.save(value); }
    @PutMapping("/{id}") @Transactional Doctor update(@PathVariable Long id, @Valid @RequestBody Doctor value) { Doctor current = find(id); current.updateFrom(value); return current; }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize("hasRole('ADMIN')") void delete(@PathVariable Long id, Principal principal) { find(id).deactivate(principal.getName()); }
    private Doctor find(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("医生不存在")); }
}

/** 管理端预约维护接口，引用校验和状态变更分别由服务端规则保证。 */
@RestController
@RequestMapping("/api/appointments")
class AppointmentController {
    private final AppointmentRepository repository;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentApplicationService appointmentService;
    private final AppointmentTimePolicy appointmentTimePolicy;
    AppointmentController(AppointmentRepository repository, PatientRepository patients, DoctorRepository doctors,
                          AppointmentApplicationService appointmentService, AppointmentTimePolicy appointmentTimePolicy) {
        this.repository = repository; this.patients = patients; this.doctors = doctors;
        this.appointmentService = appointmentService;
        this.appointmentTimePolicy = appointmentTimePolicy;
    }
    @GetMapping PageResponse<Appointment> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(repository.search(keyword.trim(), PortalPageRequests.descending(page, size, "scheduledAt")));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Appointment create(@Valid @RequestBody AppointmentWriteRequest request) {
        assertReferences(request.patientId(), request.doctorId());
        Appointment value = new Appointment();
        apply(request, value);
        value.setAppointmentNo("AP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        value.setStatus(AppointmentStatus.PENDING); return repository.save(value);
    }
    @PutMapping("/{id}") @Transactional Appointment update(@PathVariable Long id,
            @Valid @RequestBody AppointmentWriteRequest request) {
        assertReferences(request.patientId(), request.doctorId());
        Appointment current = find(id);
        apply(request, current);
        return current;
    }
    @PatchMapping("/{id}/status") AppointmentResponse status(@PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request) {
        return appointmentService.changeStatus(id, request.status());
    }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize("hasRole('ADMIN')") void delete(@PathVariable Long id, Principal principal) { find(id).deactivate(principal.getName()); }
    private void assertReferences(Long patientId, Long doctorId) {
        if (!patients.existsById(patientId)) throw new IllegalArgumentException("患者不存在");
        if (!doctors.existsById(doctorId)) throw new IllegalArgumentException("医生不存在");
    }
    private void apply(AppointmentWriteRequest request, Appointment value) {
        appointmentTimePolicy.requireFuture(request.scheduledAt());
        value.setPatientId(request.patientId());
        value.setDoctorId(request.doctorId());
        value.setScheduledAt(request.scheduledAt());
        value.setReason(request.reason());
        value.setRemark(request.remark());
    }
    private Appointment find(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("预约不存在")); }
}

/** 管理端居民健康记录接口。 */
@RestController
@RequestMapping("/api/health-records")
class HealthRecordController {
    private final HealthRecordRepository repository;
    private final PatientRepository patients;
    HealthRecordController(HealthRecordRepository repository, PatientRepository patients) { this.repository = repository; this.patients = patients; }
    @GetMapping PageResponse<HealthRecord> list(@RequestParam(required = false) Long patientId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PortalPageRequests.descending(page, size, "recordedAt");
        return PageResponse.of(patientId == null ? repository.findAll(pageable) : repository.findByPatientId(patientId, pageable));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) HealthRecord create(@Valid @RequestBody HealthRecord value) {
        if (!patients.existsById(value.getPatientId())) throw new IllegalArgumentException("患者不存在");
        if (value.getRecordedAt() == null) value.setRecordedAt(LocalDateTime.now());
        return repository.save(value);
    }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize("hasRole('ADMIN')") void delete(@PathVariable Long id, Principal principal) { find(id).deactivate(principal.getName()); }
    private HealthRecord find(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("健康记录不存在")); }
}

/** 管理端药品目录和汇总库存接口。 */
@RestController
@RequestMapping("/api/medicines")
class MedicineController {
    private final MedicineRepository repository;
    private final MedicineInventoryService inventoryService;
    MedicineController(MedicineRepository repository, MedicineInventoryService inventoryService) {
        this.repository = repository; this.inventoryService = inventoryService;
    }
    @GetMapping PageResponse<Medicine> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(repository.search(keyword.trim(), PortalPageRequests.descending(page, size, "createdAt")));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Medicine create(@Valid @RequestBody Medicine value) { return repository.save(value); }
    @PutMapping("/{id}") @Transactional Medicine update(@PathVariable Long id, @Valid @RequestBody Medicine value) { Medicine current = find(id); current.updateFrom(value); return current; }
    @PatchMapping("/{id}/stock") MedicineResponse adjustStock(@PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.adjust(id, request.delta());
    }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize("hasRole('ADMIN')") void delete(@PathVariable Long id, Principal principal) { find(id).deactivate(principal.getName()); }
    private Medicine find(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("药品不存在")); }
}

/** 管理端慢病档案维护接口。 */
@RestController
@RequestMapping("/api/chronic-cases")
class ChronicCaseController {
    private final ChronicCaseRepository repository;
    private final PatientRepository patients;
    ChronicCaseController(ChronicCaseRepository repository, PatientRepository patients) { this.repository = repository; this.patients = patients; }
    @GetMapping PageResponse<ChronicCase> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(repository.search(keyword.trim(), PortalPageRequests.descending(page, size, "createdAt")));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) ChronicCase create(@Valid @RequestBody ChronicCase value) { assertPatient(value); return repository.save(value); }
    @PutMapping("/{id}") @Transactional ChronicCase update(@PathVariable Long id, @Valid @RequestBody ChronicCase value) { assertPatient(value); ChronicCase current = find(id); current.updateFrom(value); return current; }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize("hasRole('ADMIN')") void delete(@PathVariable Long id, Principal principal) { find(id).deactivate(principal.getName()); }
    private void assertPatient(ChronicCase value) { if (!patients.existsById(value.getPatientId())) throw new IllegalArgumentException("患者不存在"); }
    private ChronicCase find(Long id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("慢病档案不存在")); }
}

/** 为管理工作台提供全中心聚合指标和最近预约。 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController {
    private final PatientRepository patients; private final DoctorRepository doctors; private final AppointmentRepository appointments;
    private final MedicineRepository medicines; private final ChronicCaseRepository chronicCases;
    DashboardController(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments,
                        MedicineRepository medicines, ChronicCaseRepository chronicCases) {
        this.patients = patients; this.doctors = doctors; this.appointments = appointments; this.medicines = medicines; this.chronicCases = chronicCases;
    }
    @GetMapping("/summary") Map<String, Object> summary() {
        LocalDate today = LocalDate.now();
        return Map.of("patients", patients.countByActiveTrue(), "doctors", doctors.countByActiveTrue(),
                "appointmentsToday", appointments.countByScheduledAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                "pendingAppointments", appointments.countByStatus(AppointmentStatus.PENDING),
                "chronicCases", chronicCases.countByActiveTrue(), "lowStockMedicines", medicines.countLowStock());
    }
    @GetMapping("/recent-appointments") List<Appointment> recentAppointments() {
        return appointments.findAll(PageRequest.of(0, 6, Sort.by("scheduledAt").descending())).getContent();
    }
}
