import axios from 'axios';

const resolveBaseUrl = () => {
    const configuredBaseUrl = process.env.NEXT_PUBLIC_API_URL?.trim();
    if (configuredBaseUrl) {
        return configuredBaseUrl;
    }

    if (typeof window !== 'undefined') {
        return `${window.location.protocol}//${window.location.hostname}:4053`;
    }

    return 'http://localhost:4053';
};

const api = axios.create({
    baseURL: resolveBaseUrl(),
});

// Request interceptor for adding auth token
api.interceptors.request.use(
    (config) => {
        if (config.data instanceof FormData) {
            delete config.headers['Content-Type'];
        } else if (!config.headers['Content-Type']) {
            config.headers['Content-Type'] = 'application/json';
        }

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

// Response interceptor for handling token verification errors
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            // Token is invalid or expired
            if (typeof window !== 'undefined') {
                void fetch('/api/auth/logout', {
                    method: 'POST',
                    credentials: 'same-origin',
                }).catch(() => undefined);
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('tokenTimestamp');
                localStorage.removeItem('user');
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default api;