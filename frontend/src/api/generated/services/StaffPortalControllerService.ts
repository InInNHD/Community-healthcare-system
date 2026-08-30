/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AppointmentResponse } from '../models/AppointmentResponse';
import type { AppointmentStatusRequest } from '../models/AppointmentStatusRequest';
import type { HealthRecordResponse } from '../models/HealthRecordResponse';
import type { MedicineResponse } from '../models/MedicineResponse';
import type { PageResponseAppointmentResponse } from '../models/PageResponseAppointmentResponse';
import type { PageResponseChronicCaseResponse } from '../models/PageResponseChronicCaseResponse';
import type { PageResponseHealthRecordResponse } from '../models/PageResponseHealthRecordResponse';
import type { PageResponsePatientResponse } from '../models/PageResponsePatientResponse';
import type { StaffHealthRecordRequest } from '../models/StaffHealthRecordRequest';
import type { StaffSummaryResponse } from '../models/StaffSummaryResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class StaffPortalControllerService {
    /**
     * @returns PageResponseHealthRecordResponse OK
     * @throws ApiError
     */
    public static healthRecords({
        patientId,
        page,
        size = 10,
    }: {
        patientId?: number,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseHealthRecordResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/health-records',
            query: {
                'patientId': patientId,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns HealthRecordResponse Created
     * @throws ApiError
     */
    public static createHealthRecord({
        requestBody,
    }: {
        requestBody: StaffHealthRecordRequest,
    }): CancelablePromise<HealthRecordResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/staff/health-records',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns AppointmentResponse OK
     * @throws ApiError
     */
    public static updateStatus({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: AppointmentStatusRequest,
    }): CancelablePromise<AppointmentResponse> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/staff/appointments/{id}/status',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns StaffSummaryResponse OK
     * @throws ApiError
     */
    public static summary(): CancelablePromise<StaffSummaryResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/summary',
        });
    }
    /**
     * @returns PageResponsePatientResponse OK
     * @throws ApiError
     */
    public static patients({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponsePatientResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/patients',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns MedicineResponse OK
     * @throws ApiError
     */
    public static medicineAlerts(): CancelablePromise<Array<MedicineResponse>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/medicines',
        });
    }
    /**
     * @returns MedicineResponse OK
     * @throws ApiError
     */
    public static medicineAlerts1(): CancelablePromise<Array<MedicineResponse>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/medicine-alerts',
        });
    }
    /**
     * @returns PageResponseChronicCaseResponse OK
     * @throws ApiError
     */
    public static chronicCases({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseChronicCaseResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/chronic-cases',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns PageResponseAppointmentResponse OK
     * @throws ApiError
     */
    public static appointments1({
        today = false,
        keyword = '',
        page,
        size = 10,
    }: {
        today?: boolean,
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseAppointmentResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/staff/appointments',
            query: {
                'today': today,
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
}
