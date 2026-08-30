/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type Appointment = {
    createdAt?: string;
    updatedAt?: string;
    version?: number;
    deletedAt?: string;
    deletedBy?: string;
    id?: number;
    appointmentNo?: string;
    patientId: number;
    doctorId: number;
    scheduledAt: string;
    status?: Appointment.status;
    reason: string;
    remark?: string;
    active?: boolean;
};
export namespace Appointment {
    export enum status {
        PENDING = 'PENDING',
        CONFIRMED = 'CONFIRMED',
        COMPLETED = 'COMPLETED',
        CANCELLED = 'CANCELLED',
    }
}

