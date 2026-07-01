import api from './api';
import {
    ProjectDto,
    Result
} from '@/types/api';

export const projectService = {
    // Get all projects
    getAll: async (): Promise<ProjectDto[]> => {
        const response = await api.get<ProjectDto[]>('/api/project/all');
        return response.data;
    },

    // Get project by ID
    getById: async (projectId: number): Promise<ProjectDto> => {
        const response = await api.get<ProjectDto>(`/api/project/${projectId}`);
        return response.data;
    },

    // Get project by short code
    getByShortCode: async (shortCode: string): Promise<ProjectDto> => {
        const response = await api.get<ProjectDto>(`/api/project/short-code/${shortCode}`);
        return response.data;
    },

    // Get projects by user ID
    getByUserId: async (userId: number): Promise<ProjectDto[]> => {
        const response = await api.get<ProjectDto[]>(`/api/project/user/${userId}`);
        return response.data;
    },

    // Get my projects (authenticated user)
    getMyProjects: async (): Promise<ProjectDto[]> => {
        const response = await api.get<ProjectDto[]>('/api/project/my-projects');
        return response.data;
    },

    // Save project
    save: async (data: ProjectDto): Promise<Result> => {
        const response = await api.post<Result>('/api/project/save', data);
        return response.data;
    },

    // Update project
    update: async (data: ProjectDto): Promise<Result> => {
        const response = await api.put<Result>('/api/project/update', data);
        return response.data;
    },

    // Delete project
    delete: async (projectId: number): Promise<Result> => {
        const response = await api.delete<Result>(`/api/project/${projectId}`);
        return response.data;
    },

    // Health check
    healthCheck: async (): Promise<string> => {
        const response = await api.get<string>('/api/project/health');
        return response.data;
    },
};
