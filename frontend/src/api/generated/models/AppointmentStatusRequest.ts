/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AppointmentStatusRequest = {
    status: AppointmentStatusRequest.status;
};
export namespace AppointmentStatusRequest {
    export enum status {
        PENDING = 'PENDING',
        CONFIRMED = 'CONFIRMED',
        COMPLETED = 'COMPLETED',
        CANCELLED = 'CANCELLED',
    }
}

