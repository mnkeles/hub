import type { Metadata } from "next";
import { NextIntlClientProvider } from 'next-intl';
import { getMessages } from 'next-intl/server';
import { CustomThemeProvider } from '@/contexts/ThemeContext';
import { AuthProvider } from '@/contexts/AuthContext';
import { ProjectProvider } from '@/contexts/ProjectContext';
import { LanguageProvider } from '@/contexts/LanguageContext';
import ProtectedRoute from '@/components/ProtectedRoute';

export const metadata: Metadata = {
    title: "APIHUB",
    description: "Performance testing dashboard",
};

export default async function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    const messages = await getMessages();
    
    return (
        <html lang="tr">
        <body>
        <NextIntlClientProvider messages={messages}>
            <LanguageProvider>
                <CustomThemeProvider>
                    <AuthProvider>
                        <ProjectProvider>
                            <ProtectedRoute>
                                {children}
                            </ProtectedRoute>
                        </ProjectProvider>
                    </AuthProvider>
                </CustomThemeProvider>
            </LanguageProvider>
        </NextIntlClientProvider>
        </body>
        </html>
    );
}