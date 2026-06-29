import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_MCP_API_URL || 'http://localhost:9999/api/documents';

const documentApi = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

documentApi.interceptors.request.use(
    (config) => {
        if (typeof window !== 'undefined') {
            const token = localStorage.getItem('token');
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export interface Document {
    id: string;
    content: string;
    title: string;
    category: string;
    ingestedAt: string;
    metadata?: Record<string, unknown>;
}

export interface DocumentCreateRequest {
    content: string;
    title: string;
    category: string;
    metadata?: Record<string, unknown>;
}

export interface DocumentResponse {
    success: boolean;
    id?: string;
    message: string;
}

export const documentService = {
    getAllDocuments: async (): Promise<Document[]> => {
        const response = await documentApi.get('/list');
        return response.data;
    },

    getDocumentById: async (id: string): Promise<Document> => {
        const response = await documentApi.get(`/${id}`);
        return response.data;
    },

    createDocument: async (documentData: DocumentCreateRequest): Promise<DocumentResponse> => {
        const response = await documentApi.post('/createDocument', documentData);
        return response.data;
    },

    updateDocument: async (id: string, documentData: DocumentCreateRequest): Promise<DocumentResponse> => {
        const response = await documentApi.put(`/${id}`, documentData);
        return response.data;
    },

    deleteDocument: async (id: string): Promise<DocumentResponse> => {
        const response = await documentApi.delete(`/${id}`);
        return response.data;
    },

    searchDocuments: async (query: string, topK: number = 5): Promise<Document[]> => {
        const response = await documentApi.get('/search', {
            params: { query, topK },
        });
        return response.data;
    },
};
