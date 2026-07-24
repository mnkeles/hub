import api from './api';
import {
    PerformanceDataset,
    PerformanceDatasetPreview,
    PerformanceDatasetRequest,
    PerformanceDatasetRow,
    PerformanceDatasetRowRequest,
} from '@/types/performance';

export const performanceDatasetService = {
    async list(projectId: number): Promise<PerformanceDataset[]> {
        const response = await api.get('/performance/datasets', {
            params: { projectId },
        });
        return response.data;
    },

    async preview(datasetId: number): Promise<PerformanceDatasetPreview> {
        const response = await api.get(`/performance/datasets/${datasetId}/preview`);
        return response.data;
    },

    async create(request: PerformanceDatasetRequest): Promise<PerformanceDataset> {
        const response = await api.post('/performance/datasets', request);
        return response.data;
    },

    async update(datasetId: number, request: PerformanceDatasetRequest): Promise<PerformanceDataset> {
        const response = await api.put(`/performance/datasets/${datasetId}`, request);
        return response.data;
    },

    async deactivate(datasetId: number): Promise<void> {
        await api.delete(`/performance/datasets/${datasetId}`);
    },

    async addRow(datasetId: number, request: PerformanceDatasetRowRequest): Promise<PerformanceDatasetRow> {
        const response = await api.post(`/performance/datasets/${datasetId}/rows`, request);
        return response.data;
    },

    async updateRow(datasetId: number, rowId: number, request: PerformanceDatasetRowRequest): Promise<PerformanceDatasetRow> {
        const response = await api.put(`/performance/datasets/${datasetId}/rows/${rowId}`, request);
        return response.data;
    },

    async deactivateRow(datasetId: number, rowId: number): Promise<void> {
        await api.delete(`/performance/datasets/${datasetId}/rows/${rowId}`);
    },

    async upload(
        projectId: number,
        name: string,
        description: string | null,
        defaultMapping: Record<string, string>,
        file: File
    ): Promise<PerformanceDataset> {
        const formData = new FormData();
        formData.append('projectId', String(projectId));
        formData.append('name', name);
        if (description) {
            formData.append('description', description);
        }
        formData.append('defaultMapping', JSON.stringify(defaultMapping ?? {}));
        formData.append('file', file);

        const response = await api.post('/performance/datasets/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
        return response.data;
    },
};
