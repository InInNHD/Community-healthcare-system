/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ChronicCase } from '../models/ChronicCase';
import type { PageResponseChronicCase } from '../models/PageResponseChronicCase';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class ChronicCaseControllerService {
    /**
     * @returns ChronicCase OK
     * @throws ApiError
     */
    public static update3({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: ChronicCase,
    }): CancelablePromise<ChronicCase> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/chronic-cases/{id}',
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
    public static delete3({
        id,
    }: {
        id: number,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/chronic-cases/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns PageResponseChronicCase OK
     * @throws ApiError
     */
    public static list4({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseChronicCase> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/chronic-cases',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns ChronicCase Created
     * @throws ApiError
     */
    public static create4({
        requestBody,
    }: {
        requestBody: ChronicCase,
    }): CancelablePromise<ChronicCase> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/chronic-cases',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
