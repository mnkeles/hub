import { useState, useRef, useCallback } from 'react';
import { ChatRequest } from '@/types/api';

export interface ChatMessage {
    role: 'user' | 'assistant' | 'system';
    content: string;
    reasoning?: string | null;
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
}

export type SendStreamingChatRequest = ChatRequest;
export type StreamingActivityState = 'idle' | 'submitting' | 'waiting' | 'thinking' | 'responding';

interface StreamingEventPayload {
    type?: string;
    content?: string | null;
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
    reasoning?: string | null;
    error?: string | null;
}

interface StreamUsageState {
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
}

interface UseStreamingChatOptions {
    apiUrl?: string;
    onError?: (error: Error) => void;
    onActivityChange?: (activity: StreamingActivityState) => void;
}

export const useStreamingChat = (options: UseStreamingChatOptions = {}) => {
    const { 
        apiUrl = '/api/chat/stream',
        onError,
        onActivityChange,
    } = options;

    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [currentAiMessage, setCurrentAiMessage] = useState('');
    const [currentAiReasoning, setCurrentAiReasoning] = useState('');
    const [currentAiUsage, setCurrentAiUsage] = useState<StreamUsageState | null>(null);
    const [isStreaming, setIsStreaming] = useState(false);
    const [streamActivity, setStreamActivity] = useState<StreamingActivityState>('idle');
    const [error, setError] = useState<string | null>(null);
    const abortControllerRef = useRef<AbortController | null>(null);
    const streamActivityRef = useRef<StreamingActivityState>('idle');

    const updateStreamActivity = useCallback((nextActivity: StreamingActivityState) => {
        if (streamActivityRef.current === nextActivity) {
            return;
        }

        streamActivityRef.current = nextActivity;
        setStreamActivity(nextActivity);
        onActivityChange?.(nextActivity);
    }, [onActivityChange]);

    const handleStreamPayload = useCallback((
        payload: string,
        currentResponse: string,
        currentReasoning: string,
        currentActivity: StreamingActivityState
    ) => {
        let nextResponse = currentResponse;
        let nextReasoning = currentReasoning;
        let nextUsage: StreamUsageState | null = null;
        let streamError: string | null = null;
        let nextActivity = currentActivity;

        try {
            const event = JSON.parse(payload) as StreamingEventPayload;

            if (event.type === 'error') {
                streamError = event.error || 'Unknown streaming error';
                return { nextResponse, nextReasoning, nextUsage, streamError, nextActivity };
            }

            if (event.type === 'usage') {
                nextUsage = {
                    promptTokens: event.promptTokens ?? null,
                    completionTokens: event.completionTokens ?? null,
                    totalTokens: event.totalTokens ?? null,
                };
                return { nextResponse, nextReasoning, nextUsage, streamError, nextActivity };
            }

            if (event.type === 'reasoning') {
                const reasoningText = typeof event.reasoning === 'string' ? event.reasoning : event.content || '';
                if (reasoningText) {
                    nextReasoning += reasoningText;
                    if (!nextResponse) {
                        nextActivity = 'thinking';
                    }
                }
                return { nextResponse, nextReasoning, nextUsage, streamError, nextActivity };
            }

            if (typeof event.content === 'string' && event.content.length > 0) {
                nextResponse += event.content;
                nextActivity = 'responding';
                return { nextResponse, nextReasoning, nextUsage, streamError, nextActivity };
            }

            return { nextResponse, nextReasoning, nextUsage, streamError, nextActivity };
        } catch {
            nextResponse += payload;
            nextActivity = 'responding';
            return { nextResponse, nextReasoning, nextUsage, streamError, nextActivity };
        }
    }, []);

    const sendMessage = useCallback(async (request: string | SendStreamingChatRequest) => {
        const requestPayload: SendStreamingChatRequest = typeof request === 'string'
            ? { message: request }
            : request;
        const message = requestPayload.message?.trim() || '';

        if (!message) return;

        const userMsg: ChatMessage = { role: 'user', content: message };
        setMessages(prev => [...prev, userMsg]);
        setIsStreaming(true);
        setCurrentAiMessage('');
        setCurrentAiReasoning('');
        setCurrentAiUsage(null);
        setError(null);
        updateStreamActivity('submitting');

        abortControllerRef.current = new AbortController();

        try {
            // Token'ı localStorage'dan al
            const token = localStorage.getItem('token');
            const headers: HeadersInit = {
                'Content-Type': 'application/json',
            };

            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053';
            const response = await fetch(`${baseURL}${apiUrl}`, {
                method: 'POST',
                headers,
                body: JSON.stringify({
                    ...requestPayload,
                    message,
                }),
                signal: abortControllerRef.current.signal
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body?.getReader();
            if (!reader) {
                throw new Error('Response body is not readable');
            }

            updateStreamActivity('waiting');
            const decoder = new TextDecoder();
            let aiResponse = '';
            let aiReasoning = '';
            let aiUsage: StreamUsageState | null = null;
            let pendingChunk = '';
            let aiActivity: StreamingActivityState = 'waiting';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                pendingChunk += decoder.decode(value, { stream: true });
                const lines = pendingChunk.split('\n');
                pendingChunk = lines.pop() || '';
                
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const data = line.substring(5);
                        if (data && data !== '[DONE]') {
                            const { nextResponse, nextReasoning, nextUsage, streamError, nextActivity } = handleStreamPayload(data, aiResponse, aiReasoning, aiActivity);
                            aiResponse = nextResponse;
                            aiReasoning = nextReasoning;
                            aiActivity = nextActivity;
                            if (nextUsage) {
                                aiUsage = nextUsage;
                            }
                            updateStreamActivity(aiActivity);
                            setCurrentAiMessage(aiResponse);
                            setCurrentAiReasoning(aiReasoning);
                            setCurrentAiUsage(aiUsage);

                            if (streamError) {
                                throw new Error(streamError);
                            }
                        }
                    } else if (line.trim() && !line.startsWith(':')) {
                        const { nextResponse, nextReasoning, nextUsage, streamError, nextActivity } = handleStreamPayload(line, aiResponse, aiReasoning, aiActivity);
                        aiResponse = nextResponse;
                        aiReasoning = nextReasoning;
                        aiActivity = nextActivity;
                        if (nextUsage) {
                            aiUsage = nextUsage;
                        }
                        updateStreamActivity(aiActivity);
                        setCurrentAiMessage(aiResponse);
                        setCurrentAiReasoning(aiReasoning);
                        setCurrentAiUsage(aiUsage);

                        if (streamError) {
                            throw new Error(streamError);
                        }
                    }
                }
            }

            if (pendingChunk.trim() && pendingChunk.trim() !== 'data: [DONE]') {
                const finalLine = pendingChunk.startsWith('data:') ? pendingChunk.substring(5) : pendingChunk;
                if (finalLine && finalLine !== '[DONE]') {
                    const { nextResponse, nextReasoning, nextUsage, streamError, nextActivity } = handleStreamPayload(finalLine, aiResponse, aiReasoning, aiActivity);
                    aiResponse = nextResponse;
                    aiReasoning = nextReasoning;
                    aiActivity = nextActivity;
                    if (nextUsage) {
                        aiUsage = nextUsage;
                    }
                    updateStreamActivity(aiActivity);
                    setCurrentAiMessage(aiResponse);
                    setCurrentAiReasoning(aiReasoning);
                    setCurrentAiUsage(aiUsage);

                    if (streamError) {
                        throw new Error(streamError);
                    }
                }
            }

            setMessages(prev => [...prev, {
                role: 'assistant',
                content: aiResponse,
                reasoning: aiReasoning || null,
                promptTokens: aiUsage?.promptTokens ?? null,
                completionTokens: aiUsage?.completionTokens ?? null,
                totalTokens: aiUsage?.totalTokens ?? null,
            }]);
            setCurrentAiMessage('');
            setCurrentAiReasoning('');
            setCurrentAiUsage(null);

        } catch (err) {
            if (err instanceof Error && err.name !== 'AbortError') {
                const errorMessage = err.message;
                setError(errorMessage);
                setMessages(prev => [...prev, { 
                    role: 'assistant', 
                    content: errorMessage,
                    reasoning: null,
                    promptTokens: null,
                    completionTokens: null,
                    totalTokens: null,
                }]);
                onError?.(err);
            }
        } finally {
            setIsStreaming(false);
            updateStreamActivity('idle');
            abortControllerRef.current = null;
        }
    }, [apiUrl, handleStreamPayload, onError, updateStreamActivity]);

    const cancelStream = useCallback(() => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setIsStreaming(false);
            setCurrentAiMessage('');
            setCurrentAiReasoning('');
            setCurrentAiUsage(null);
            updateStreamActivity('idle');
        }
    }, [updateStreamActivity]);

    const clearMessages = useCallback(() => {
        setMessages([]);
        setCurrentAiMessage('');
        setCurrentAiReasoning('');
        setCurrentAiUsage(null);
        setError(null);
        updateStreamActivity('idle');
    }, [updateStreamActivity]);

    const loadHistory = useCallback(async () => {
        try {
            const token = localStorage.getItem('token');
            const headers: HeadersInit = {
                'Content-Type': 'application/json',
            };

            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053';
            const response = await fetch(`${baseURL}/api/chat/history`, {
                method: 'GET',
                headers,
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const history: ChatMessage[] = await response.json();
            setMessages(history);
        } catch (err) {
            console.error('Failed to load history:', err);
        }
    }, []);

    const clearHistory = useCallback(async () => {
        try {
            const token = localStorage.getItem('token');
            const headers: HeadersInit = {
                'Content-Type': 'application/json',
            };

            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053';
            const response = await fetch(`${baseURL}/api/chat/history`, {
                method: 'DELETE',
                headers,
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            setMessages([]);
            setCurrentAiMessage('');
            setCurrentAiReasoning('');
            setCurrentAiUsage(null);
            updateStreamActivity('idle');
        } catch (err) {
            console.error('Failed to clear history:', err);
        }
    }, [updateStreamActivity]);

    return {
        messages,
        currentAiMessage,
        currentAiReasoning,
        currentAiUsage,
        isStreaming,
        streamActivity,
        error,
        sendMessage,
        cancelStream,
        clearMessages,
        loadHistory,
        clearHistory,
    };
};
