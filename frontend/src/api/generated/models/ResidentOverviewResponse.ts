/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AppointmentResponse } from './AppointmentResponse';
import type { HealthRecordResponse } from './HealthRecordResponse';
import type { PatientResponse } from './PatientResponse';
export type ResidentOverviewResponse = {
    profile?: PatientResponse;
    pendingAppointments?: number;
    healthRecordCount?: number;
    chronicPlanCount?: number;
    nextAppointment?: AppointmentResponse;
    latestHealthRecord?: HealthRecordResponse;
};

