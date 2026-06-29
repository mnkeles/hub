import api from './api';
import { ApiInformationDto, Result, GeneralPageRequest, PagedResponse } from '@/types/api';

export const apiInformationService = {
    // Get paginated API information list
    list: async (request?: GeneralPageRequest): Promise<PagedResponse<ApiInformationDto>> => {
        const response = await api.post<PagedResponse<ApiInformationDto>>(
            '/api/api-information/list',
            request || { offset: 0, limit: 100, filterList: [] }
        );
        return response.data;
    },

    // Get all API information (max 1000 records)
    getAll: async (): Promise<ApiInformationDto[]> => {
        const response = await api.get<ApiInformationDto[]>('/api/api-information/all');
        return response.data;
    },

    // Get API information by ID
    getById: async (id: number): Promise<ApiInformationDto> => {
        const response = await api.get<ApiInformationDto>(`/api/api-information/${id}`);
        return response.data;
    },

    // Get API information by short code
    getByShortCode: async (shortCode: string): Promise<ApiInformationDto> => {
        const response = await api.get<ApiInformationDto>(`/api/api-information/short-code/${shortCode}`);
        return response.data;
    },

    // Get API information by project
    getByProject: async (projectShortCode: string): Promise<ApiInformationDto[]> => {
        const response = await api.get<ApiInformationDto[]>(`/api/api-information/project/${projectShortCode}`);
        return response.data;
    },

    // Save new API information
    save: async (data: ApiInformationDto): Promise<Result> => {
        const response = await api.post<Result>('/api/api-information/save', data);
        return response.data;
    },

    // Update API information
    update: async (data: ApiInformationDto): Promise<Result> => {
        const response = await api.put<Result>('/api/api-information/update', data);
        return response.data;
    },

    // Delete API information
    delete: async (id: number): Promise<Result> => {
        const response = await api.delete<Result>(`/api/api-information/${id}`);
        return response.data;
    },

    // Health check
    healthCheck: async (): Promise<string> => {
        const response = await api.get<string>('/api/api-information/health');
        return response.data;
    },
};
