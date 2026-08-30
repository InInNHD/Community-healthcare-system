/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Appointment } from '../models/Appointment';
import type { AppointmentResponse } from '../models/AppointmentResponse';
import type { AppointmentStatusRequest } from '../models/AppointmentStatusRequest';
import type { AppointmentWriteRequest } from '../models/AppointmentWriteRequest';
import type { PageResponseAppointment } from '../models/PageResponseAppointment';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AppointmentControllerService {
    /**
     * @returns Appointment OK
     * @throws ApiError
     */
    public static update4({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: AppointmentWriteRequest,
    }): CancelablePromise<Appointment> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/appointments/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns any OK
     * @throws ApiError
     */
    public static delete4({
        id,
    }: {
        id: number,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/appointments/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns PageResponseAppointment OK
     * @throws ApiError
     */
    public static list5({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseAppointment> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/appointments',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns Appointment Created
     * @throws ApiError
     */
    public static create5({
        requestBody,
    }: {
        requestBody: AppointmentWriteRequest,
    }): CancelablePromise<Appointment> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/appointments',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns AppointmentResponse OK
     * @throws ApiError
     */
    public static status({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: AppointmentStatusRequest,
    }): CancelablePromise<AppointmentResponse> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/appointments/{id}/status',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
