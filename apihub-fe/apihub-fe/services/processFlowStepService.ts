import api from './api';
import {
    ProcessFlowStepDto,
    ProcessFlowStepRelationDto,
    GeneralPageRequest,
    PagedResponse,
    Result
} from '@/types/api';

const buildStepPayload = (data: ProcessFlowStepDto): ProcessFlowStepDto => ({
    processFlowStepId: data.processFlowStepId,
    gnlApiInformationId: data.gnlApiInformationId || data.apiInformation?.id || data.apiInformation?.gnlApiInformationId || 0,
    processFlowId: data.processFlowId,
    stepOrder: data.stepOrder,
    stepShortCode: data.stepShortCode,
    plIn: data.plIn || '',
    headerExtractor: data.headerExtractor || '',
    parameterExtractor: data.parameterExtractor || '',
    processFlowStepParmList: data.processFlowStepParmList || [],
    preHeader: data.preHeader || '',
});

export const processFlowStepService = {
    // Get single process flow step by ID with all relations
    getById: async (processFlowStepId: number): Promise<ProcessFlowStepDto> => {
        const response = await api.get<ProcessFlowStepDto>(`/api/process-flow-step/${processFlowStepId}`);
        return response.data;
    },

    // Get all process flow steps without pagination (up to 1000 records)
    getAll: async (): Promise<ProcessFlowStepDto[]> => {
        try {
            // Try the /all endpoint first
            const response = await api.get<ProcessFlowStepDto[]>('/api/process-flow-step/all');
            return response.data;
        } catch (error) {
            // Fallback to /list endpoint if /all doesn't exist
            const response = await api.post<PagedResponse<ProcessFlowStepDto>>(
                '/api/process-flow-step/list',
                { offset: 0, limit: 1000, filterList: [] }
            );
            return response.data.data; // Return the data array from PagedResponse
        }
    },

    // Get all process flow steps with pagination
    list: async (request?: GeneralPageRequest): Promise<PagedResponse<ProcessFlowStepDto>> => {
        const response = await api.post<PagedResponse<ProcessFlowStepDto>>(
            '/api/process-flow-step/list',
            request || { offset: 0, limit: 100, filterList: [] }
        );
        return response.data;
    },

    // Get process flow step relations
    getRelations: async (
        processFlowStepId: number,
        projectId: number
    ): Promise<ProcessFlowStepRelationDto[]> => {
        const response = await api.get<ProcessFlowStepRelationDto[]>(
            `/api/process-flow-step/relations/${processFlowStepId}/${projectId}`
        );
        return response.data;
    },

    // Save process flow step
    save: async (data: ProcessFlowStepDto): Promise<Result> => {
        const response = await api.post<Result>('/api/process-flow-step/save', buildStepPayload(data));
        return response.data;
    },

    // Update process flow step
    update: async (data: ProcessFlowStepDto): Promise<Result> => {
        const response = await api.put<Result>('/api/process-flow-step/update', buildStepPayload(data));
        return response.data;
    },

    // Delete process flow step
    delete: async (processFlowStepId: number): Promise<void> => {
        await api.delete(`/api/process-flow-step/${processFlowStepId}`);
    },

    // Delete process flow step parameter
    deleteParameter: async (relationId: number): Promise<void> => {
        await api.delete(`/api/process-flow-step/parameter/${relationId}`);
    },

    // Create new process step
    createStep: async (data: ProcessFlowStepRelationDto): Promise<Result> => {
        const response = await api.post<Result>('/api/process-flow-step/create-step', data);
        return response.data;
    },

    // Update step orders after drag and drop
    updateStepOrders: async (steps: { processFlowStepId: number; stepOrder: number }[]): Promise<Result> => {
        const response = await api.post<Result>('/api/process-flow-step/update-orders', steps);
        return response.data;
    },
};
