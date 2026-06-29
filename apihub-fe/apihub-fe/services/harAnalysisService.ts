import api from './api';
import { HarAnalysisResponse } from '@/types/harAnalysis';

const createHarFormData = (
    file: File,
    projectShortCode: string,
    systemShortCode: string
): FormData => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectShortCode', projectShortCode);
    formData.append('systemShortCode', systemShortCode);
    return formData;
};

export const harAnalysisService = {
    uploadAndAnalyze: async (
        file: File,
        projectShortCode: string,
        systemShortCode: string
    ): Promise<HarAnalysisResponse> => {
        const response = await api.post<HarAnalysisResponse>(
            '/api/har/upload',
            createHarFormData(file, projectShortCode, systemShortCode)
        );
        return response.data;
    },

    analyzeHarContent: async (
        harContent: string,
        projectShortCode: string,
        systemShortCode: string
    ): Promise<HarAnalysisResponse> => {
        const response = await api.post<HarAnalysisResponse>('/api/har/analyze', {
            harContent,
            projectShortCode,
            systemShortCode,
        });
        return response.data;
    },

    generateServiceFromHar: async (
        file: File,
        projectShortCode: string,
        systemShortCode: string
    ): Promise<HarAnalysisResponse> => {
        const response = await api.post<HarAnalysisResponse>(
            '/api/har/generate-service',
            createHarFormData(file, projectShortCode, systemShortCode)
        );
        return response.data;
    },
};
