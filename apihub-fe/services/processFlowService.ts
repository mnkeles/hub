import api from './api';
import {
    ProcessFlowDto,
    GeneralPageRequest,
    PagedResponse,
    Result
} from '@/types/api';

export const processFlowService = {
    // Get all process flows without pagination (up to 1000 records)
    getAll: async (): Promise<ProcessFlowDto[]> => {
        try {
            // Try the /all endpoint first
            const response = await api.get<ProcessFlowDto[]>('/api/process-flow/all');
            return response.data;
        } catch (error) {
            // Fallback to /list endpoint if /all doesn't exist
            const response = await api.post<PagedResponse<ProcessFlowDto>>(
                '/api/process-flow/list',
                { offset: 0, limit: 1000, filterList: [] }
            );
            return response.data.data; // Return the data array from PagedResponse
        }
    },

    // Get all process flows with pagination
    list: async (request?: GeneralPageRequest): Promise<PagedResponse<ProcessFlowDto>> => {
        const response = await api.post<PagedResponse<ProcessFlowDto>>(
            '/api/process-flow/list',
            request || { offset: 0, limit: 100, filterList: [] }
        );
        return response.data;
    },

    // Get process flow by ID
    getById: async (processFlowId: number): Promise<ProcessFlowDto> => {
        const response = await api.get<ProcessFlowDto>(`/api/process-flow/${processFlowId}`);
        return response.data;
    },

    // Get process flow with relations
    getWithRelations: async (processFlowId: number): Promise<ProcessFlowDto> => {
        const response = await api.get<ProcessFlowDto>(
            `/api/process-flow/${processFlowId}/with-relations`
        );
        return response.data;
    },

    // Get process flows by project
    getByProject: async (projectId: number): Promise<ProcessFlowDto[]> => {
        const response = await api.get<ProcessFlowDto[]>(`/api/process-flow/project/${projectId}`);
        return response.data;
    },

    // Save process flow
    save: async (data: ProcessFlowDto): Promise<Result> => {
        const response = await api.post<Result>('/api/process-flow/save', data);
        return response.data;
    },

    // Update process flow
    update: async (data: ProcessFlowDto): Promise<Result> => {
        const response = await api.put<Result>('/api/process-flow/update', data);
        return response.data;
    },

    // Delete process flow
    delete: async (processFlowId: number): Promise<Result> => {
        const response = await api.delete<Result>(`/api/process-flow/${processFlowId}`);
        return response.data;
    },

    // Copy process flow with all steps and parameters
    copy: async (processFlowId: number): Promise<Result<number>> => {
        const response = await api.post<Result<number>>(`/api/process-flow/copy/${processFlowId}`);
        return response.data;
    },
};
