'use client';

import React, { createContext, useContext, useState, ReactNode } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeColor, ThemeMode, createAppTheme } from '@/themes/themeConfig';

const themeColors: ThemeColor[] = ['indigo', 'blue', 'green', 'purple', 'orange', 'red'];
const themeModes: ThemeMode[] = ['light', 'dark'];

const getStoredThemeColor = (): ThemeColor => {
    if (typeof window === 'undefined') {
        return 'indigo';
    }

    const savedColor = localStorage.getItem('themeColor');
    return themeColors.includes(savedColor as ThemeColor) ? savedColor as ThemeColor : 'indigo';
};

const getStoredThemeMode = (): ThemeMode => {
    if (typeof window === 'undefined') {
        return 'light';
    }

    const savedMode = localStorage.getItem('themeMode');
    return themeModes.includes(savedMode as ThemeMode) ? savedMode as ThemeMode : 'light';
};

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
    const [themeColor, setThemeColorState] = useState<ThemeColor>(getStoredThemeColor);
    const [themeMode, setThemeModeState] = useState<ThemeMode>(getStoredThemeMode);

    const setThemeColor = (color: ThemeColor) => {
        setThemeColorState(color);
        if (typeof window !== 'undefined') {
            localStorage.setItem('themeColor', color);
        }
    };

    const setThemeMode = (mode: ThemeMode) => {
        setThemeModeState(mode);
        if (typeof window !== 'undefined') {
            localStorage.setItem('themeMode', mode);
        }
    };

    const toggleThemeMode = () => {
        const newMode: ThemeMode = themeMode === 'light' ? 'dark' : 'light';
        setThemeMode(newMode);
    };

    const theme = createAppTheme(themeColor, themeMode);

    return (
        <ThemeContext.Provider value={{ themeColor, themeMode, setThemeColor, setThemeMode, toggleThemeMode }}>
            <ThemeProvider theme={theme}>
                <CssBaseline />
                {children}
            </ThemeProvider>
        </ThemeContext.Provider>
    );
}
