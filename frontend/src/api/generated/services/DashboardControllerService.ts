/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Appointment } from '../models/Appointment';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DashboardControllerService {
    /**
     * @returns any OK
     * @throws ApiError
     */
    public static summary1(): CancelablePromise<Record<string, any>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/dashboard/summary',
        });
    }
    /**
     * @returns Appointment OK
     * @throws ApiError
     */
    public static recentAppointments(): CancelablePromise<Array<Appointment>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/dashboard/recent-appointments',
        });
    }
}
