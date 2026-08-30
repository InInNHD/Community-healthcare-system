/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Doctor } from '../models/Doctor';
import type { PageResponseDoctor } from '../models/PageResponseDoctor';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DoctorControllerService {
    /**
     * @returns Doctor OK
     * @throws ApiError
     */
    public static get1({
        id,
    }: {
        id: number,
    }): CancelablePromise<Doctor> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/doctors/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns Doctor OK
     * @throws ApiError
     */
    public static update2({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: Doctor,
    }): CancelablePromise<Doctor> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/doctors/{id}',
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
    public static delete2({
        id,
    }: {
        id: number,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/doctors/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns PageResponseDoctor OK
     * @throws ApiError
     */
    public static list3({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseDoctor> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/doctors',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns Doctor Created
     * @throws ApiError
     */
    public static create3({
        requestBody,
    }: {
        requestBody: Doctor,
    }): CancelablePromise<Doctor> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/doctors',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
