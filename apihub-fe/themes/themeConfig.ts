'use client';
import { createTheme, Theme } from '@mui/material/styles';

export type ThemeColor = 'indigo' | 'blue' | 'green' | 'purple' | 'orange' | 'red';
export type ThemeMode = 'light' | 'dark';

interface ThemeConfig {
    name: string;
    primary: string;
    primaryLight: string;
    primaryDark: string;
    secondary: string;
}

export const themeConfigs: Record<ThemeColor, ThemeConfig> = {
    indigo: {
        name: 'Indigo',
        primary: '#6366f1',
        primaryLight: '#818cf8',
        primaryDark: '#4f46e5',
        secondary: '#ec4899',
    },
    blue: {
        name: 'Mavi',
        primary: '#3b82f6',
        primaryLight: '#60a5fa',
        primaryDark: '#2563eb',
        secondary: '#8b5cf6',
    },
    green: {
        name: 'Yeşil',
        primary: '#10b981',
        primaryLight: '#34d399',
        primaryDark: '#059669',
        secondary: '#f59e0b',
    },
    purple: {
        name: 'Mor',
        primary: '#8b5cf6',
        primaryLight: '#a78bfa',
        primaryDark: '#7c3aed',
        secondary: '#ec4899',
    },
    orange: {
        name: 'Turuncu',
        primary: '#f59e0b',
        primaryLight: '#fbbf24',
        primaryDark: '#d97706',
        secondary: '#3b82f6',
    },
    red: {
        name: 'Kırmızı',
        primary: '#ef4444',
        primaryLight: '#f87171',
        primaryDark: '#dc2626',
        secondary: '#8b5cf6',
    },
};

export function createAppTheme(colorScheme: ThemeColor, mode: ThemeMode = 'light'): Theme {
    const config = themeConfigs[colorScheme];

    return createTheme({
        palette: {
            mode: mode,
            primary: {
                main: config.primary,
                light: config.primaryLight,
                dark: config.primaryDark,
                contrastText: '#ffffff',
            },
            secondary: {
                main: config.secondary,
                light: config.secondary,
                dark: config.secondary,
                contrastText: '#ffffff',
            },
            success: {
                main: '#10b981',
                light: '#34d399',
                dark: '#059669',
                ...(mode === 'light' ? { lighter: '#d1fae5' } : { lighter: '#064e3b' }),
            },
            warning: {
                main: '#f59e0b',
                light: '#fbbf24',
                dark: '#d97706',
                ...(mode === 'light' ? { lighter: '#fef3c7' } : { lighter: '#78350f' }),
            },
            error: {
                main: '#ef4444',
                light: '#f87171',
                dark: '#dc2626',
                ...(mode === 'light' ? { lighter: '#fee2e2' } : { lighter: '#7f1d1d' }),
            },
            info: {
                main: '#3b82f6',
                light: '#60a5fa',
                dark: '#2563eb',
                ...(mode === 'light' ? { lighter: '#dbeafe' } : { lighter: '#1e3a8a' }),
            },
            ...(mode === 'light'
                ? {
                      background: {
                          default: '#f8fafc',
                          paper: '#ffffff',
                      },
                      text: {
                          primary: '#1e293b',
                          secondary: '#64748b',
                      },
                  }
                : {
                      background: {
                          default: '#0f172a',
                          paper: '#1e293b',
                      },
                      text: {
                          primary: '#f1f5f9',
                          secondary: '#94a3b8',
                      },
                  }),
        },
        typography: {
            fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
            h1: {
                fontWeight: 700,
                fontSize: '2.5rem',
            },
            h2: {
                fontWeight: 700,
                fontSize: '2rem',
            },
            h3: {
                fontWeight: 600,
                fontSize: '1.75rem',
            },
            h4: {
                fontWeight: 600,
                fontSize: '1.5rem',
            },
            h5: {
                fontWeight: 600,
                fontSize: '1.25rem',
            },
            h6: {
                fontWeight: 600,
                fontSize: '1rem',
            },
            button: {
                textTransform: 'none',
                fontWeight: 500,
            },
        },
        shape: {
            borderRadius: 12,
        },
        shadows: [
            'none',
            '0 1px 2px 0 rgb(0 0 0 / 0.05)',
            '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
            '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
            '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
            '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
            '0 25px 50px -12px rgb(0 0 0 / 0.25)',
        ],
        components: {
            MuiButton: {
                styleOverrides: {
                    root: {
                        borderRadius: 8,
                        padding: '8px 16px',
                        fontWeight: 500,
                    },
                    contained: {
                        boxShadow: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
                        '&:hover': {
                            boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
                        },
                    },
                },
            },
            MuiCard: {
                styleOverrides: {
                    root: {
                        borderRadius: 12,
                        boxShadow: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
                    },
                },
            },
            MuiPaper: {
                styleOverrides: {
                    root: {
                        borderRadius: 12,
                    },
                },
            },
            MuiChip: {
                styleOverrides: {
                    root: {
                        borderRadius: 6,
                        fontWeight: 500,
                    },
                },
            },
        },
    });
}
