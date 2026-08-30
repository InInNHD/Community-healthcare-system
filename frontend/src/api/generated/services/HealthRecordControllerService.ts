/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { HealthRecord } from '../models/HealthRecord';
import type { PageResponseHealthRecord } from '../models/PageResponseHealthRecord';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class HealthRecordControllerService {
    /**
     * @returns PageResponseHealthRecord OK
     * @throws ApiError
     */
    public static list2({
        patientId,
        page,
        size = 10,
    }: {
        patientId?: number,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseHealthRecord> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/health-records',
            query: {
                'patientId': patientId,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns HealthRecord Created
     * @throws ApiError
     */
    public static create2({
        requestBody,
    }: {
        requestBody: HealthRecord,
    }): CancelablePromise<HealthRecord> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/health-records',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns any OK
     * @throws ApiError
     */
    public static delete5({
        id,
    }: {
        id: number,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/health-records/{id}',
            path: {
                'id': id,
            },
        });
    }
}
