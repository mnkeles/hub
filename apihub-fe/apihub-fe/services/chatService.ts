import api from './api';
import { ChatRequest } from '@/types/api';

export interface ChatMessage {
    role: 'system' | 'user' | 'assistant';
    content: string;
    reasoning?: string | null;
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
}

export type SendMessageRequest = ChatRequest;

export interface SendMessageResponse {
    message: string;
    history: ChatMessage[];
}

export const chatService = {
    // Streaming mesaj gönder (Önerilen - ChatGPT benzeri)
    sendStreamingMessage: async (
        request: string | SendMessageRequest,
        onChunk: (chunk: string) => void,
        signal?: AbortSignal
    ): Promise<string> => {
        const requestPayload: SendMessageRequest = typeof request === 'string'
            ? { message: request }
            : request;
        const token = localStorage.getItem('token');
        const headers: HeadersInit = {
            'Content-Type': 'application/json',
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053';
        const response = await fetch(`${baseURL}/api/chat/stream`, {
            method: 'POST',
            headers,
            body: JSON.stringify(requestPayload),
            signal,
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body?.getReader();
        if (!reader) {
            throw new Error('Response body is not readable');
        }

        const decoder = new TextDecoder();
        let fullResponse = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');
            
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    // SSE formatını parse et - substring(5) ile 'data:' kısmını atla
                    // trim() KULLANMA - backend'in gönderdiği boşlukları koru
                    const data = line.substring(5);
                    if (data && data !== '[DONE]') {
                        fullResponse += data;
                        onChunk(data);
                    }
                } else if (line.trim() && !line.startsWith(':')) {
                    // SSE formatı yoksa direkt ekle (yorum satırları hariç)
                    fullResponse += line;
                    onChunk(line);
                }
            }
        }

        return fullResponse;
    },

    // Normal mesaj gönder (geçmişle) - Eski yöntem
    sendMessage: async (request: string | SendMessageRequest): Promise<SendMessageResponse> => {
        const requestPayload: SendMessageRequest = typeof request === 'string'
            ? { message: request }
            : request;
        const response = await api.post<SendMessageResponse>('/api/chat/send', requestPayload);
        return response.data;
    },

    // Tek mesaj gönder (geçmişsiz)
    sendSingleMessage: async (request: string | SendMessageRequest): Promise<SendMessageResponse> => {
        const requestPayload: SendMessageRequest = typeof request === 'string'
            ? { message: request }
            : request;
        const response = await api.post<SendMessageResponse>('/api/chat/single', requestPayload);
        return response.data;
    },

    // Geçmişi getir
    getHistory: async (): Promise<ChatMessage[]> => {
        const response = await api.get<ChatMessage[]>('/api/chat/history');
        return response.data;
    },

    // Geçmişi temizle
    clearHistory: async (): Promise<void> => {
        await api.delete('/api/chat/history');
    },

    // Health check
    healthCheck: async (): Promise<string> => {
        const response = await api.get<string>('/api/chat/health');
        return response.data;
    }
};
