'use client';

import React, { useState } from 'react';
import { IconButton, Menu, MenuItem, Box, Typography } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import { useLanguage } from '@/contexts/LanguageContext';
import { locales, Locale } from '@/i18n/config';

// SVG Flag Components
const TurkeyFlag = () => (
    <svg width="24" height="18" viewBox="0 0 24 18" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="24" height="18" fill="#E30A17"/>
        <circle cx="9" cy="9" r="4.5" fill="white"/>
        <circle cx="10.5" cy="9" r="3.5" fill="#E30A17"/>
        <path d="M14 6.5L15.5 10.5L12 8.5H16L12.5 10.5L14 6.5Z" fill="white"/>
    </svg>
);

const UKFlag = () => (
    <svg width="24" height="18" viewBox="0 0 24 18" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="24" height="18" fill="#012169"/>
        <path d="M0 0L24 18M24 0L0 18" stroke="white" strokeWidth="3"/>
        <path d="M0 0L24 18M24 0L0 18" stroke="#C8102E" strokeWidth="2"/>
        <path d="M12 0V18M0 9H24" stroke="white" strokeWidth="5"/>
        <path d="M12 0V18M0 9H24" stroke="#C8102E" strokeWidth="3"/>
    </svg>
);

const flagComponents: Record<Locale, React.FC> = {
    tr: TurkeyFlag,
    en: UKFlag,
};

const localeNames: Record<Locale, string> = {
    tr: 'Türkçe',
    en: 'English',
};

export default function LanguageSwitcher() {
    const { locale, setLocale } = useLanguage();
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const open = Boolean(anchorEl);

    const handleClick = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleLocaleChange = (newLocale: Locale) => {
        setLocale(newLocale);
        handleClose();
    };

    const CurrentFlag = flagComponents[locale];

    return (
        <>
            <IconButton
                onClick={handleClick}
                sx={{
                    color: 'text.primary',
                    padding: '8px',
                    '&:hover': {
                        backgroundColor: 'action.hover',
                    },
                }}
                title={localeNames[locale]}
            >
                <CurrentFlag />
            </IconButton>
            <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleClose}
                anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'right',
                }}
                transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                }}
                PaperProps={{
                    sx: {
                        mt: 1,
                        minWidth: 180,
                    },
                }}
            >
                {locales.map((loc) => {
                    const FlagComponent = flagComponents[loc];
                    return (
                        <MenuItem
                            key={loc}
                            onClick={() => handleLocaleChange(loc)}
                            selected={locale === loc}
                            sx={{ py: 1.5 }}
                        >
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, width: '100%' }}>
                                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                    <FlagComponent />
                                </Box>
                                <Typography sx={{ flex: 1 }}>
                                    {localeNames[loc]}
                                </Typography>
                                {locale === loc && (
                                    <CheckIcon fontSize="small" color="primary" />
                                )}
                            </Box>
                        </MenuItem>
                    );
                })}
            </Menu>
        </>
    );
}
