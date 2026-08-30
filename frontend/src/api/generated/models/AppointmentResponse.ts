/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AppointmentResponse = {
    id?: number;
    appointmentNo?: string;
    patientId?: number;
    doctorId?: number;
    patientName?: string;
    doctorName?: string;
    department?: string;
    scheduledAt?: string;
    status?: AppointmentResponse.status;
    reason?: string;
    remark?: string;
    active?: boolean;
    createdAt?: string;
    updatedAt?: string;
    version?: number;
};
export namespace AppointmentResponse {
    export enum status {
        PENDING = 'PENDING',
        CONFIRMED = 'CONFIRMED',
        COMPLETED = 'COMPLETED',
        CANCELLED = 'CANCELLED',
    }
}

