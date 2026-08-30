/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DoctorResponse } from './DoctorResponse';
export type StaffSummaryResponse = {
    staff?: DoctorResponse;
    staffProfile?: DoctorResponse;
    patients?: number;
    appointmentsToday?: number;
    pendingAppointments?: number;
    completedToday?: number;
    chronicCases?: number;
    lowStockMedicines?: number;
};

