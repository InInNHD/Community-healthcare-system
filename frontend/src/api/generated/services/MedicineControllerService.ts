/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Medicine } from '../models/Medicine';
import type { MedicineResponse } from '../models/MedicineResponse';
import type { PageResponseMedicine } from '../models/PageResponseMedicine';
import type { StockAdjustmentRequest } from '../models/StockAdjustmentRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class MedicineControllerService {
    /**
     * @returns Medicine OK
     * @throws ApiError
     */
    public static update1({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: Medicine,
    }): CancelablePromise<Medicine> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/medicines/{id}',
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
    public static delete1({
        id,
    }: {
        id: number,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/medicines/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @returns PageResponseMedicine OK
     * @throws ApiError
     */
    public static list1({
        keyword = '',
        page,
        size = 10,
    }: {
        keyword?: string,
        page?: number,
        size?: number,
    }): CancelablePromise<PageResponseMedicine> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/medicines',
            query: {
                'keyword': keyword,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * @returns Medicine Created
     * @throws ApiError
     */
    public static create1({
        requestBody,
    }: {
        requestBody: Medicine,
    }): CancelablePromise<Medicine> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/medicines',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns MedicineResponse OK
     * @throws ApiError
     */
    public static adjustStock({
        id,
        requestBody,
    }: {
        id: number,
        requestBody: StockAdjustmentRequest,
    }): CancelablePromise<MedicineResponse> {
        return __request(OpenAPI, {
            method: 'PATCH',
            url: '/api/medicines/{id}/stock',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
