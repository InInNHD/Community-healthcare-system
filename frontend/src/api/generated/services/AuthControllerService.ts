/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CurrentUser } from '../models/CurrentUser';
import type { LoginRequest } from '../models/LoginRequest';
import type { LoginResponse } from '../models/LoginResponse';
import type { PasswordChangeRequest } from '../models/PasswordChangeRequest';
import type { PasswordChangeResponse } from '../models/PasswordChangeResponse';
import type { PasswordPolicyResponse } from '../models/PasswordPolicyResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AuthControllerService {
    /**
     * @returns PasswordChangeResponse OK
     * @throws ApiError
     */
    public static changePassword1({
        requestBody,
    }: {
        requestBody: PasswordChangeRequest,
    }): CancelablePromise<PasswordChangeResponse> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/auth/change-password',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns PasswordChangeResponse OK
     * @throws ApiError
     */
    public static changePassword({
        requestBody,
    }: {
        requestBody: PasswordChangeRequest,
    }): CancelablePromise<PasswordChangeResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/change-password',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns LoginResponse OK
     * @throws ApiError
     */
    public static login({
        requestBody,
    }: {
        requestBody: LoginRequest,
    }): CancelablePromise<LoginResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/login',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @returns PasswordPolicyResponse OK
     * @throws ApiError
     */
    public static passwordPolicy(): CancelablePromise<PasswordPolicyResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/auth/password-policy',
        });
    }
    /**
     * @returns CurrentUser OK
     * @throws ApiError
     */
    public static me(): CancelablePromise<CurrentUser> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/auth/me',
        });
    }
}
