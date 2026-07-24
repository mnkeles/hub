import { useAuth } from '@/contexts/AuthContext';

export const useAuthorization = () => {
    const { hasPermission, hasAnyPermission, permissions } = useAuth();

    return {
        permissions,
        hasPermission,
        hasAnyPermission,
        canManageAuthorization: () => hasPermission('PERMISSION.MANAGE'),
        canManageServiceAccounts: () => hasPermission('SERVICE_ACCOUNT.MANAGE'),
        canUseAiChat: () => hasPermission('AI_CHAT.USE'),
    };
};
