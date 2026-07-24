'use client';

import React from 'react';
import { useAuth } from '@/contexts/AuthContext';

interface RequirePermissionProps {
    permission: string;
    children: React.ReactNode;
    fallback?: React.ReactNode;
}

export default function RequirePermission({ permission, children, fallback = null }: RequirePermissionProps) {
    const { hasPermission } = useAuth();
    return hasPermission(permission) ? <>{children}</> : <>{fallback}</>;
}
