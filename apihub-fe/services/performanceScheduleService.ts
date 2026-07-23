import api from './api';
import {
    PerformanceResultDto,
    PerformanceSchedule,
    PerformanceScheduleRequest,
} from '@/types/performance';

export const performanceScheduleService = {
    async list(projectId: number, processFlowId: number): Promise<PerformanceSchedule[]> {
        const response = await api.get('/performance/schedules', {
            params: { projectId, processFlowId },
        });
        return response.data;
    },

    async create(request: PerformanceScheduleRequest): Promise<PerformanceSchedule> {
        const response = await api.post('/performance/schedules', request);
        return response.data;
    },

    async update(scheduleId: number, request: PerformanceScheduleRequest): Promise<PerformanceSchedule> {
        const response = await api.put(`/performance/schedules/${scheduleId}`, request);
        return response.data;
    },

    async setEnabled(scheduleId: number, enabled: boolean): Promise<PerformanceSchedule> {
        const response = await api.post(`/performance/schedules/${scheduleId}/enabled`, null, {
            params: { enabled },
        });
        return response.data;
    },

    async deactivate(scheduleId: number): Promise<void> {
        await api.delete(`/performance/schedules/${scheduleId}`);
    },

    async runNow(scheduleId: number): Promise<PerformanceResultDto> {
        const response = await api.post(`/performance/schedules/${scheduleId}/run-now`);
        return response.data;
    },
};
