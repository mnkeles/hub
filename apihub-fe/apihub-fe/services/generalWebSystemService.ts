import api from './api';
import {
    GeneralWebSystemDto,
    GeneralPageRequest,
    PagedResponse,
    Result
} from '@/types/api';

export const generalWebSystemService = {
    // Get all general web systems without pagination (up to 1000 records)
    getAll: async (): Promise<GeneralWebSystemDto[]> => {
        try {
            // Try the /all endpoint first
            const response = await api.get<GeneralWebSystemDto[]>('/api/general-web-system/all');
            return response.data;
        } catch (error) {
            // Fallback to /list endpoint if /all doesn't exist
            const response = await api.post<PagedResponse<GeneralWebSystemDto>>(
                '/api/general-web-system/list',
                { offset: 0, limit: 1000, filterList: [] }
            );
            return response.data.data; // Return the data array from PagedResponse
        }
    },

    // Get all general web systems with pagination
    list: async (request?: GeneralPageRequest): Promise<PagedResponse<GeneralWebSystemDto>> => {
        const response = await api.post<PagedResponse<GeneralWebSystemDto>>(
            '/api/general-web-system/list',
            request || { offset: 0, limit: 100, filterList: [] }
        );
        return response.data;
    },

    // Save general web system
    save: async (data: GeneralWebSystemDto): Promise<Result> => {
        const response = await api.post<Result>('/api/general-web-system/save', data);
        return response.data;
    },

    // Delete general web system
    delete: async (id: number): Promise<Result> => {
        const response = await api.delete<Result>(`/api/general-web-system/${id}`);
        return response.data;
    },

    // Health check
    healthCheck: async (): Promise<string> => {
        const response = await api.get<string>('/api/general-web-system/health');
        return response.data;
    },
};
