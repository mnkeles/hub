import api from './api';
import { DatabaseDto, GeneralPageRequest, PagedResponse } from '@/types/api';

export const databaseService = {
    // Get all databases
    getAll: async (): Promise<DatabaseDto[]> => {
        const response = await api.get<DatabaseDto[]>('/api/database/all');
        return response.data;
    },

    // Get database by ID
    getById: async (id: number): Promise<DatabaseDto> => {
        const response = await api.get<DatabaseDto>(`/api/database/${id}`);
        return response.data;
    },

    // List databases with pagination
    list: async (request: GeneralPageRequest): Promise<PagedResponse<DatabaseDto>> => {
        const response = await api.post<PagedResponse<DatabaseDto>>('/api/database/list', request);
        return response.data;
    },

    // Save (create or update) database
    save: async (database: DatabaseDto): Promise<DatabaseDto> => {
        const response = await api.post<DatabaseDto>('/api/database/save', database);
        return response.data;
    },

    // Update database
    update: async (database: DatabaseDto): Promise<DatabaseDto> => {
        const response = await api.put<DatabaseDto>('/api/database/update', database);
        return response.data;
    },

    // Delete database
    delete: async (id: number): Promise<void> => {
        await api.delete(`/api/database/delete/${id}`);
    },
};
