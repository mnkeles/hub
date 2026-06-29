import api from './api';
import {
    PerformanceComparisonResult,
    PerformanceDetailResponse,
    PerformanceExportPayload,
    PerformanceHistoryItem,
    PerformanceLiveSnapshot,
    PerformanceRequest,
    PerformanceResultDto,
    PerformanceValidationChecklist,
} from '@/types/performance';

export const performanceService = {
    async runPerformanceTest(request: PerformanceRequest): Promise<PerformanceResultDto> {
        const response = await api.post('/performance/run', request);
        return response.data;
    },

    async getPerformanceDetail(performanceResultId: number): Promise<PerformanceDetailResponse> {
        const response = await api.get('/performance/detail', {
            params: { performanceResultId },
        });
        return response.data;
    },

    async getHistory(projectId: number, processFlowId: number): Promise<PerformanceHistoryItem[]> {
        const response = await api.get('/performance/getHistory', {
            params: { projectId, processFlowId },
        });
        return response.data;
    },

    async setBaseline(performanceResultId: number): Promise<PerformanceHistoryItem> {
        const response = await api.post('/performance/baseline', null, {
            params: { performanceResultId },
        });
        return response.data;
    },

    async getBaseline(projectId: number, processFlowId: number): Promise<PerformanceHistoryItem | null> {
        const response = await api.get('/performance/baseline', {
            params: { projectId, processFlowId },
            validateStatus: (status) => (status >= 200 && status < 300) || status === 204,
        });
        return response.status === 204 ? null : response.data;
    },

    async updateValidationNote(performanceResultId: number, note: string): Promise<PerformanceValidationChecklist> {
        const response = await api.post('/performance/validation-note', {
            performanceResultId,
            note,
        });
        return response.data;
    },

    async getAnalysis(performanceResultId: number): Promise<PerformanceExportPayload> {
        const response = await api.get('/performance/analysis', {
            params: { performanceResultId },
        });
        return response.data;
    },

    async getLive(performanceResultId: number): Promise<PerformanceLiveSnapshot> {
        const response = await api.get('/performance/live', {
            params: { performanceResultId },
        });
        return response.data;
    },

    async stop(performanceResultId: number): Promise<PerformanceLiveSnapshot> {
        const response = await api.post('/performance/stop', null, {
            params: { performanceResultId },
        });
        return response.data;
    },

    async forceStop(performanceResultId: number): Promise<PerformanceLiveSnapshot> {
        const response = await api.post('/performance/force-stop', null, {
            params: { performanceResultId },
        });
        return response.data;
    },

    async compare(baseResultId: number, targetResultId: number): Promise<PerformanceComparisonResult> {
        const response = await api.get('/performance/compare', {
            params: { baseResultId, targetResultId },
        });
        return response.data;
    },

    async exportJson(performanceResultId: number): Promise<PerformanceExportPayload> {
        const response = await api.get('/performance/export', {
            params: { performanceResultId, format: 'json' },
        });
        return response.data;
    },

    async exportCsv(performanceResultId: number): Promise<Blob> {
        const response = await api.get('/performance/export', {
            params: { performanceResultId, format: 'csv' },
            responseType: 'blob',
        });
        return response.data;
    },
};
