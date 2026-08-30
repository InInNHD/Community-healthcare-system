/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type LoginResponse = {
    accessToken?: string;
    tokenType?: string;
    expiresIn?: number;
    username?: string;
    displayName?: string;
    roles?: Array<string>;
    portal?: string;
    subjectId?: number;
    staffId?: number;
    patientId?: number;
    mustChangePassword?: boolean;
    passwordChangedAt?: string;
};

