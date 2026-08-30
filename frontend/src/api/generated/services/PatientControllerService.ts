/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PageResponsePatient } from '../models/PageResponsePatient';
import type { Patient } from '../models/Patient';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class PatientControllerService {
    /**
     * @returns Patient OK
     * @throws ApiError
     */
    public static get({
        id,
    }: {
        id: number,
    }): CancelablePromise<Patient> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/patients/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns Patient OK
     * @throws ApiError
     */
    public static update({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: Patient,
    }): CancelablePromise<Patient> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/patients/{id}',
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
    public static delete({
        id,
    }: {
        id: number,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/patients/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns PageResponsePatient OK
     * @throws ApiError
     */
    public static list({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponsePatient> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/patients',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns Patient Created
     * @throws ApiError
     */
    public static create({
        requestBody,
    }: {
        requestBody: Patient,
    }): CancelablePromise<Patient> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/patients',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
