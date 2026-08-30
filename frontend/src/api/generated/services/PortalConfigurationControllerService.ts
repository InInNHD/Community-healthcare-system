/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PortalConfigurationResponse } from '../models/PortalConfigurationResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class PortalConfigurationControllerService {
    /**
     * @returns PortalConfigurationResponse OK
     * @throws ApiError
     */
    public static portalConfig(): CancelablePromise<PortalConfigurationResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/public/portal-config',
        });
    }
}
