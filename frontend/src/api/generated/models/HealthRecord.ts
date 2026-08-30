/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type HealthRecord = {
    createdAt?: string;
    updatedAt?: string;
    version?: number;
    deletedAt?: string;
    deletedBy?: string;
    id?: number;
    patientId: number;
    recordedAt: string;
    heartRate?: number;
    systolicPressure?: number;
    diastolicPressure?: number;
    bloodOxygen?: number;
    weight?: number;
    note?: string;
    active?: boolean;
};

