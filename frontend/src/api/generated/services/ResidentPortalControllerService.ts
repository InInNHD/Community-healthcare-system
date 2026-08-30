/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AppointmentResponse } from '../models/AppointmentResponse';
import type { ChronicCaseResponse } from '../models/ChronicCaseResponse';
import type { HealthRecordResponse } from '../models/HealthRecordResponse';
import type { PageResponseAppointmentResponse } from '../models/PageResponseAppointmentResponse';
import type { PageResponseDoctorResponse } from '../models/PageResponseDoctorResponse';
import type { PageResponseHealthRecordResponse } from '../models/PageResponseHealthRecordResponse';
import type { PatientResponse } from '../models/PatientResponse';
import type { ResidentAppointmentRequest } from '../models/ResidentAppointmentRequest';
import type { ResidentHealthRecordRequest } from '../models/ResidentHealthRecordRequest';
import type { ResidentOverviewResponse } from '../models/ResidentOverviewResponse';
import type { ResidentProfileRequest } from '../models/ResidentProfileRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class ResidentPortalControllerService {
    /**
     * @returns PatientResponse OK
     * @throws ApiError
     */
    public static profile(): CancelablePromise<PatientResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/profile',
        });
    }
    /**
     * @returns PatientResponse OK
     * @throws ApiError
     */
    public static updateProfile({
        requestBody,
    }: {
        requestBody: ResidentProfileRequest,
    }): CancelablePromise<PatientResponse> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/resident/profile',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns PageResponseHealthRecordResponse OK
     * @throws ApiError
     */
    public static healthRecords1({
        page,
        size = 10,
    }: {
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseHealthRecordResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/health-records',
            query: {
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns HealthRecordResponse Created
     * @throws ApiError
     */
    public static createHealthRecord1({
        requestBody,
    }: {
        requestBody: ResidentHealthRecordRequest,
    }): CancelablePromise<HealthRecordResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/resident/health-records',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns PageResponseAppointmentResponse OK
     * @throws ApiError
     */
    public static appointments({
        page,
        size = 10,
    }: {
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseAppointmentResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/appointments',
            query: {
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns AppointmentResponse Created
     * @throws ApiError
     */
    public static createAppointment({
        requestBody,
    }: {
        requestBody: ResidentAppointmentRequest,
    }): CancelablePromise<AppointmentResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/resident/appointments',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns AppointmentResponse OK
     * @throws ApiError
     */
    public static cancelAppointment({
        id,
    }: {
        id: number,
    }): CancelablePromise<AppointmentResponse> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/resident/appointments/{id}/cancel',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns ResidentOverviewResponse OK
     * @throws ApiError
     */
    public static overview(): CancelablePromise<ResidentOverviewResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/summary',
        });
    }
    /**
     * @returns ResidentOverviewResponse OK
     * @throws ApiError
     */
    public static overview1(): CancelablePromise<ResidentOverviewResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/overview',
        });
    }
    /**
     * @returns PageResponseDoctorResponse OK
     * @throws ApiError
     */
    public static doctors({
        keyword = '',
        page,
        size = 50,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseDoctorResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/doctors',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns ChronicCaseResponse OK
     * @throws ApiError
     */
    public static chronicPlans(): CancelablePromise<Array<ChronicCaseResponse>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/chronic-cases',
        });
    }
    /**
     * @returns ChronicCaseResponse OK
     * @throws ApiError
     */
    public static chronicPlans1(): CancelablePromise<Array<ChronicCaseResponse>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/resident/chronic-plans',
        });
    }
}
