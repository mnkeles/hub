import api from './api';
import { ParameterRequestDto, ApiCallResponse } from '@/types/api';

export const apiCallService = {
    // Call single step
    callStep: async (
        project: string,
        systemShortCode: string,
        stepShortCode: string
    ): Promise<string> => {
        const response = await api.get<string>(
            `/api/callStep/${project}/${systemShortCode}/${stepShortCode}`
        );
        return response.data;
    },

    // Call process flow
    callProcess: async (
        project: string,
        systemShortCode: string,
        processFlow: string
    ): Promise<ApiCallResponse> => {
        const response = await api.get<ApiCallResponse>(
            `/api/callProcess/${project}/${systemShortCode}/${processFlow}`
        );
        return response.data;
    },

    // Call process flow with parameters (v2)
    callProcessV2: async (
        project: string,
        systemShortCode: string,
        processFlow: string,
        parameters?: ParameterRequestDto
    ): Promise<ApiCallResponse> => {
        const response = await api.post<ApiCallResponse>(
            `/api/callProcess/${project}/${systemShortCode}/${processFlow}/v2`,
            parameters || { parameterContext: {}, globalHeaders: {}, combination: false }
        );
        return response.data;
    },

    // Call process flow with parameters (v2) and continueOnError=true
    callProcessV2WithContinueOnError: async (
        project: string,
        systemShortCode: string,
        processFlow: string,
        parameters?: ParameterRequestDto
    ): Promise<ApiCallResponse> => {
        const response = await api.post<ApiCallResponse>(
            `/api/callProcess/${project}/${systemShortCode}/${processFlow}/v2?continueOnError=true`,
            parameters || { parameterContext: {}, globalHeaders: {}, combination: false }
        );
        return response.data;
    },
};
