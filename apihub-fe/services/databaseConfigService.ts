import api from './api';
import { GeneralPageRequest, PagedResponse, Result } from '@/types/api';

export interface DatabaseConfigDto {
    dbConfigId?: number;
    shortCode: string;
    url: string;
    username: string;
    password?: string;
    isActv: boolean;
    actv?: boolean;
    schema: string;
    driver: string;
    projectId: number;
}

export const databaseConfigService = {
    // Get paginated database config list
    list: async (request?: GeneralPageRequest): Promise<PagedResponse<DatabaseConfigDto>> => {
        const response = await api.post<PagedResponse<DatabaseConfigDto>>(
            '/api/database-config/list',
            request || { offset: 0, limit: 100, filterList: [] }
        );
        return response.data;
    },

    // Get all database configs (max 1000 records)
    getAll: async (): Promise<DatabaseConfigDto[]> => {
        const response = await api.get<DatabaseConfigDto[]>('/api/database-config/all');
        return response.data;
    },

    // Get database config by ID
    getById: async (id: number): Promise<DatabaseConfigDto> => {
        const response = await api.get<DatabaseConfigDto>(`/api/database-config/${id}`);
        return response.data;
    },

    // Get database config by short code
    getByShortCode: async (shortCode: string): Promise<DatabaseConfigDto> => {
        const response = await api.get<DatabaseConfigDto>(`/api/database-config/short-code/${shortCode}`);
        return response.data;
    },

    // Get database configs by project
    getByProject: async (projectShortCode: string): Promise<DatabaseConfigDto[]> => {
        const response = await api.get<DatabaseConfigDto[]>(`/api/database-config/project/${projectShortCode}`);
        return response.data;
    },

    // Get active database configs by project
    getActiveByProject: async (projectShortCode: string): Promise<DatabaseConfigDto[]> => {
        const response = await api.get<DatabaseConfigDto[]>(`/api/database-config/project/${projectShortCode}/active`);
        return response.data;
    },

    // Save new database config
    save: async (data: DatabaseConfigDto): Promise<Result> => {
        const response = await api.post<Result>('/api/database-config/save', data);
        return response.data;
    },

    // Update database config
    update: async (data: DatabaseConfigDto): Promise<Result> => {
        const response = await api.put<Result>('/api/database-config/update', data);
        return response.data;
    },

    // Delete database config
    delete: async (id: number): Promise<Result> => {
        const response = await api.delete<Result>(`/api/database-config/${id}`);
        return response.data;
    },

    // Health check
    healthCheck: async (): Promise<string> => {
        const response = await api.get<string>('/api/database-config/health');
        return response.data;
    },
};
