import api from './api';

export interface CurrentUser {
    username: string;
    authType: 'ldap' | 'local' | string;
    firstName?: string | null;
    lastName?: string | null;
    enabled: number;
    projectId?: number | null;
}

export const userService = {
    getCurrentUser: async (): Promise<CurrentUser> => {
        const response = await api.get<CurrentUser>('/api/user/current');
        return response.data;
    },
};
