'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeColor, ThemeMode, createAppTheme } from '@/themes/themeConfig';

interface ThemeContextType {
    themeColor: ThemeColor;
    themeMode: ThemeMode;
    setThemeColor: (color: ThemeColor) => void;
    setThemeMode: (mode: ThemeMode) => void;
    toggleThemeMode: () => void;
}

const ThemeContext = createContext<ThemeContextType>({
    themeColor: 'indigo',
    themeMode: 'light',
    setThemeColor: () => {},
    setThemeMode: () => {},
    toggleThemeMode: () => {},
});

export const useTheme = () => useContext(ThemeContext);

interface CustomThemeProviderProps {
    children: ReactNode;
}

export function CustomThemeProvider({ children }: CustomThemeProviderProps) {
    const [themeColor, setThemeColorState] = useState<ThemeColor>('indigo');
    const [themeMode, setThemeModeState] = useState<ThemeMode>('light');
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
        const savedColor = localStorage.getItem('themeColor') as ThemeColor;
        const savedMode = localStorage.getItem('themeMode') as ThemeMode;
        if (savedColor) {
            setThemeColorState(savedColor);
        }
        if (savedMode) {
            setThemeModeState(savedMode);
        }
    }, []);

    const setThemeColor = (color: ThemeColor) => {
        setThemeColorState(color);
        localStorage.setItem('themeColor', color);
    };

    const setThemeMode = (mode: ThemeMode) => {
        setThemeModeState(mode);
        localStorage.setItem('themeMode', mode);
    };

    const toggleThemeMode = () => {
        const newMode: ThemeMode = themeMode === 'light' ? 'dark' : 'light';
        setThemeMode(newMode);
    };

    const theme = createAppTheme(themeColor, themeMode);

    // Prevent flash of unstyled content
    if (!mounted) {
        return null;
    }

    return (
        <ThemeContext.Provider value={{ themeColor, themeMode, setThemeColor, setThemeMode, toggleThemeMode }}>
            <ThemeProvider theme={theme}>
                <CssBaseline />
                {children}
            </ThemeProvider>
        </ThemeContext.Provider>
    );
}
