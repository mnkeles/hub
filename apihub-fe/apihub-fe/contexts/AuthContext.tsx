'use client';
import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { authService, LoginCredentials } from '@/services/authService';
import { useRouter } from 'next/navigation';

interface User {
    id: string;
    username: string;
    email?: string;
}

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const router = useRouter();

    const logout = useCallback(() => {
        authService.logout();
        setUser(null);
        router.push('/login');
    }, [router]);

    // Token yenileme fonksiyonu
    const refreshAccessToken = useCallback(async () => {
        try {
            await authService.refreshToken();
            console.log('Token refreshed successfully');
        } catch (error) {
            console.error('Token refresh failed:', error);
            // Token yenileme başarısız olursa kullanıcıyı logout yap
            logout();
        }
    }, [logout]);

    // Token yenileme zamanlayıcısını ayarla
    useEffect(() => {
        const token = authService.getToken();
        const refreshToken = authService.getRefreshToken();
        let initialRefreshTimeoutId: ReturnType<typeof setTimeout> | null = null;
        
        if (!token || !refreshToken) {
            return;
        }

        // 25 dakika sonra token'ı yenile (30 dakikalık sürenin 5 dakika öncesi)
        const REFRESH_INTERVAL = 25 * 60 * 1000; // 25 dakika (milisaniye cinsinden)
        
        // İlk yükleme sırasında token'ın ne kadar süredir geçerli olduğunu kontrol et
        const tokenTimestamp = authService.getTokenTimestamp();
        if (tokenTimestamp) {
            const elapsed = Date.now() - tokenTimestamp;
            const remaining = REFRESH_INTERVAL - elapsed;
            
            // Eğer token zaten 25 dakikadan eski ise hemen yenile
            if (remaining <= 0) {
                initialRefreshTimeoutId = setTimeout(() => {
                    void refreshAccessToken();
                }, 0);
            }
        }

        // Periyodik olarak token'ı yenile
        const intervalId = setInterval(() => {
            refreshAccessToken();
        }, REFRESH_INTERVAL);

        return () => {
            clearInterval(intervalId);

            if (initialRefreshTimeoutId) {
                clearTimeout(initialRefreshTimeoutId);
            }
        };
    }, [refreshAccessToken, user]);

    useEffect(() => {
        // Check if user is already logged in
        const token = authService.getToken();
        const refreshToken = authService.getRefreshToken();
        const savedUser = authService.getUser();

        const initializeAuth = async () => {
            if (token && refreshToken && savedUser) {
                setUser(savedUser);

                try {
                    await authService.refreshToken();
                } catch (error) {
                    console.error('Initial auth refresh failed:', error);
                    authService.logout();
                    setUser(null);
                }
            }

            setIsLoading(false);
        };

        void initializeAuth();
    }, []);

    const login = async (credentials: LoginCredentials) => {
        try {
            await authService.login(credentials);
            // User'ı localStorage'dan al (authService.login içinde kaydedildi)
            const savedUser = authService.getUser();
            if (savedUser) {
                setUser(savedUser);
            }
            // Router.push yerine window.location kullan (client-side navigation sorunu için)
            window.location.href = '/dashboard';
        } catch (error) {
            console.error('Login failed:', error);
            throw error;
        }
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                isAuthenticated: !!user,
                isLoading,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
