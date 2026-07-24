import api from './api';

export interface PermissionItem {
    permissionId: number;
    permissionKey: string;
    name: string;
    description?: string;
    category?: string;
    uiVisible?: boolean;
    serviceAssignable?: boolean;
    enabled: number;
}

export interface ServiceAccountItem {
    serviceAccountId: number;
    serviceCode: string;
    name: string;
    description?: string;
    owner?: string;
    enabled: number;
}

export interface ServiceAccountRequest {
    serviceCode: string;
    name: string;
    description?: string;
    owner?: string;
}

export interface ServiceTokenCreateRequest {
    tokenName: string;
    expiresAt?: string;
}

export interface ServiceTokenCreateResponse {
    serviceTokenId: number;
    tokenName: string;
    token: string;
    tokenPrefix: string;
    expiresAt?: string;
}

export interface UserPermissionAssignmentRequest {
    permissionIds: number[];
}

export interface UserSummary {
    userId: number;
    email: string;
    firstName?: string;
    lastName?: string;
    authType?: string;
    enabled: number;
}

export interface UserPermissionsResponse {
    userId: number;
    email: string;
    permissions: PermissionItem[];
}

export interface ServiceAccountPermissionAssignmentRequest {
    assignments: { permissionId: number; projectId?: number }[];
}

export const authorizationService = {
    getPermissions: async (): Promise<PermissionItem[]> => {
        const response = await api.get<PermissionItem[]>('/api/authorization/permissions');
        return response.data;
    },

    getUsers: async (): Promise<UserSummary[]> => {
        const response = await api.get<UserSummary[]>('/api/authorization/users');
        return response.data;
    },

    getUserPermissions: async (userId: number): Promise<UserPermissionsResponse> => {
        const response = await api.get<UserPermissionsResponse>(`/api/authorization/users/${userId}/permissions`);
        return response.data;
    },

    assignUserPermissions: async (userId: number, request: UserPermissionAssignmentRequest): Promise<void> => {
        await api.put(`/api/authorization/users/${userId}/permissions`, request);
    },

    assignServiceAccountPermissions: async (serviceAccountId: number, request: ServiceAccountPermissionAssignmentRequest): Promise<void> => {
        await api.put(`/api/authorization/service-accounts/${serviceAccountId}/permissions`, request);
    },

    getServiceAccounts: async (): Promise<ServiceAccountItem[]> => {
        const response = await api.get<ServiceAccountItem[]>('/api/authorization/service-accounts');
        return response.data;
    },

    createServiceAccount: async (request: ServiceAccountRequest): Promise<ServiceAccountItem> => {
        const response = await api.post<ServiceAccountItem>('/api/authorization/service-accounts', request);
        return response.data;
    },

    createServiceToken: async (serviceAccountId: number, request: ServiceTokenCreateRequest): Promise<ServiceTokenCreateResponse> => {
        const response = await api.post<ServiceTokenCreateResponse>(`/api/authorization/service-accounts/${serviceAccountId}/tokens`, request);
        return response.data;
    },

    revokeServiceToken: async (tokenId: number): Promise<void> => {
        await api.post(`/api/authorization/service-tokens/${tokenId}/revoke`);
    },
};
