'use client';

import React, { useState } from 'react';
import {
    Box,
    Drawer,
    AppBar,
    Toolbar,
    List,
    Typography,
    Divider,
    IconButton,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    useTheme as useMuiTheme,
    useMediaQuery,
    Select,
    MenuItem,
    FormControl,
    Menu,
    Tooltip,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import SpeedIcon from '@mui/icons-material/Speed';
import ApiIcon from '@mui/icons-material/Api';
import SettingsIcon from '@mui/icons-material/Settings';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import StorageIcon from '@mui/icons-material/Storage';
import CodeIcon from '@mui/icons-material/Code';
import PaletteIcon from '@mui/icons-material/Palette';
import DescriptionIcon from '@mui/icons-material/Description';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import { useRouter, usePathname } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useProject } from '@/contexts/ProjectContext';
import { useTheme } from '@/contexts/ThemeContext';
import { ThemeColor, themeConfigs } from '@/themes/themeConfig';
import LanguageSwitcher from './LanguageSwitcher';

const drawerWidth = 260;

interface NavigationItem {
    text: string;
    icon: React.ReactNode;
    path: string;
    nested?: boolean;
}

type NavigationTranslator = (key: string) => string;

const getMenuItems = (t: NavigationTranslator): NavigationItem[] => [
    { text: t('apiInfo'), icon: <ApiIcon />, path: '/dashboard/api-information' },
    { text: t('flows'), icon: <AccountTreeIcon />, path: '/dashboard/process-flows' },
    { text: t('environments'), icon: <StorageIcon />, path: '/dashboard/systems' },
    { text: t('dataConnections'), icon: <StorageIcon />, path: '/dashboard/environments' },
    { text: t('performance'), icon: <SpeedIcon />, path: '/dashboard/performance' },
    { text: t('apiExecutor'), icon: <CodeIcon />, path: '/dashboard/api-executor' },
    { text: t('documents'), icon: <DescriptionIcon />, path: '/dashboard/documents' },
    { text: t('user'), icon: <AccountCircleIcon />, path: '/dashboard/user' },
    { text: t('settings'), icon: <SettingsIcon />, path: '/logout' },
];

interface DashboardLayoutProps {
    children: React.ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
    const [mobileOpen, setMobileOpen] = useState(false);
    const [desktopOpen, setDesktopOpen] = useState(true);
    const [themeMenuAnchor, setThemeMenuAnchor] = useState<null | HTMLElement>(null);
    const { projects, selectedProject, setSelectedProject, projectSelectDisabled } = useProject();
    const { themeColor, themeMode, setThemeColor, toggleThemeMode } = useTheme();
    const t = useTranslations('nav');
    const theme = useMuiTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));
    const router = useRouter();
    const pathname = usePathname();
    const menuItems = getMenuItems(t);

    const handleDrawerToggle = () => {
        if (isMobile) {
            setMobileOpen(!mobileOpen);
        } else {
            setDesktopOpen(!desktopOpen);
        }
    };

    const handleNavigation = (path: string) => {
        router.push(path);
        if (isMobile) {
            setMobileOpen(false);
        }
    };

    const handleThemeMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        setThemeMenuAnchor(event.currentTarget);
    };

    const handleThemeMenuClose = () => {
        setThemeMenuAnchor(null);
    };

    const handleThemeChange = (color: ThemeColor) => {
        setThemeColor(color);
        handleThemeMenuClose();
    };

    const drawer = (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Toolbar
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    px: [1],
                }}
            >
                <Typography 
                    variant="h6" 
                    noWrap 
                    component="div" 
                    onClick={() => handleNavigation('/dashboard')}
                    sx={{ 
                        fontWeight: 700, 
                        fontSize: '0.95rem',
                        cursor: 'pointer',
                        '&:hover': {
                            color: 'primary.main',
                        }
                    }}
                >
                    {selectedProject?.shortCode ? `${selectedProject.shortCode} OTOMASYON` : 'OMNI OTOMASYON'}
                </Typography>
                {!isMobile && (
                    <IconButton onClick={handleDrawerToggle}>
                        <ChevronLeftIcon />
                    </IconButton>
                )}
            </Toolbar>
            <Divider />
            <List sx={{ flex: 1, pt: 2 }}>
                {menuItems.map((item) => (
                    <ListItem key={item.text} disablePadding sx={{ display: 'block', mb: 0.5 }}>
                        <ListItemButton
                            onClick={() => handleNavigation(item.path)}
                            selected={pathname === item.path}
                            sx={{
                                minHeight: 48,
                                justifyContent: desktopOpen ? 'initial' : 'center',
                                px: 2.5,
                                mx: 1,
                                borderRadius: 1,
                                ml: 1,
                                '&.Mui-selected': {
                                    backgroundColor: theme.palette.primary.main,
                                    color: theme.palette.primary.contrastText,
                                    '&:hover': {
                                        backgroundColor: theme.palette.primary.dark,
                                    },
                                    '& .MuiListItemIcon-root': {
                                        color: theme.palette.primary.contrastText,
                                    },
                                },
                            }}
                        >
                            <ListItemIcon
                                sx={{
                                    minWidth: 0,
                                    mr: desktopOpen ? 3 : 'auto',
                                    justifyContent: 'center',
                                }}
                            >
                                {item.icon}
                            </ListItemIcon>
                            <ListItemText
                                primary={item.text}
                                primaryTypographyProps={{
                                    fontSize: '0.95rem',
                                    fontWeight: 400,
                                    lineHeight: 1.35,
                                    whiteSpace: 'normal',
                                }}
                                sx={{ opacity: desktopOpen ? 1 : 0 }}
                            />
                        </ListItemButton>
                    </ListItem>
                ))}
            </List>
            <Divider />
        </Box>
    );

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh' }}>
            <AppBar
                position="fixed"
                sx={{
                    zIndex: theme.zIndex.drawer + 1,
                    backgroundColor: theme.palette.background.paper,
                    color: theme.palette.text.primary,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
                }}
            >
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        edge="start"
                        onClick={handleDrawerToggle}
                        sx={{ mr: 2 }}
                    >
                        <MenuIcon />
                    </IconButton>
                    <Typography 
                        variant="h6" 
                        noWrap 
                        component="div" 
                        onClick={() => handleNavigation('/dashboard')}
                        sx={{ 
                            fontWeight: 700,
                            cursor: 'pointer',
                            '&:hover': {
                                color: 'primary.main',
                            }
                        }}
                    >
                        {selectedProject?.shortCode ? `${selectedProject.shortCode} OTOMASYON` : 'OMNI OTOMASYON'}
                    </Typography>
                    <Box sx={{ flexGrow: 1 }} />
                    <Tooltip title={themeMode === 'light' ? 'Koyu Tema' : 'Açık Tema'}>
                        <IconButton
                            onClick={toggleThemeMode}
                            sx={{ 
                                mr: 1,
                                color: 'inherit',
                                backgroundColor: 'rgba(99, 102, 241, 0.08)',
                                '&:hover': {
                                    backgroundColor: 'rgba(99, 102, 241, 0.15)',
                                }
                            }}
                        >
                            {themeMode === 'light' ? <Brightness4Icon /> : <Brightness7Icon />}
                        </IconButton>
                    </Tooltip>
                    <Tooltip title="Dil / Language">
                        <Box sx={{ mr: 1 }}>
                            <LanguageSwitcher />
                        </Box>
                    </Tooltip>
                    <Tooltip title="Renk Teması">
                        <IconButton
                            onClick={handleThemeMenuOpen}
                            sx={{ 
                                mr: 2,
                                color: 'inherit',
                                backgroundColor: 'rgba(99, 102, 241, 0.08)',
                                '&:hover': {
                                    backgroundColor: 'rgba(99, 102, 241, 0.15)',
                                }
                            }}
                        >
                            <PaletteIcon />
                        </IconButton>
                    </Tooltip>
                    <Menu
                        anchorEl={themeMenuAnchor}
                        open={Boolean(themeMenuAnchor)}
                        onClose={handleThemeMenuClose}
                        PaperProps={{
                            sx: {
                                mt: 1,
                                minWidth: 200,
                                borderRadius: 2,
                                boxShadow: 3,
                            }
                        }}
                    >
                        {(Object.keys(themeConfigs) as ThemeColor[]).map((color) => (
                            <MenuItem
                                key={color}
                                onClick={() => handleThemeChange(color)}
                                selected={themeColor === color}
                                sx={{
                                    display: 'flex',
                                    gap: 2,
                                    py: 1.5,
                                    '&.Mui-selected': {
                                        backgroundColor: 'rgba(99, 102, 241, 0.12)',
                                        '&:hover': {
                                            backgroundColor: 'rgba(99, 102, 241, 0.18)',
                                        }
                                    }
                                }}
                            >
                                <Box
                                    sx={{
                                        width: 24,
                                        height: 24,
                                        borderRadius: '50%',
                                        backgroundColor: themeConfigs[color].primary,
                                        border: '2px solid',
                                        borderColor: themeColor === color ? 'primary.main' : 'transparent',
                                        boxShadow: 1,
                                    }}
                                />
                                <Typography variant="body2" sx={{ fontWeight: themeColor === color ? 600 : 400 }}>
                                    {themeConfigs[color].name}
                                </Typography>
                            </MenuItem>
                        ))}
                    </Menu>
                    <FormControl sx={{ minWidth: 220 }}>
                        <Select
                            value={selectedProject?.projectId || ''}
                            disabled={projectSelectDisabled || projects.length <= 1}
                            onChange={(e) => {
                                if (projectSelectDisabled) {
                                    return;
                                }

                                const project = projects.find(p => p.projectId === e.target.value);
                                setSelectedProject(project || null);
                            }}
                            sx={{
                                color: pathname === '/dashboard/api-executor' ? 'primary.main' : 'inherit',
                                backgroundColor: pathname === '/dashboard/api-executor' 
                                    ? 'rgba(25, 118, 210, 0.12)' 
                                    : 'rgba(99, 102, 241, 0.08)',
                                borderRadius: '20px',
                                fontWeight: 600,
                                fontSize: '0.95rem',
                                '.MuiOutlinedInput-notchedOutline': { 
                                    borderColor: pathname === '/dashboard/api-executor'
                                        ? 'primary.main'
                                        : 'rgba(99, 102, 241, 0.3)',
                                    borderWidth: '2px',
                                    borderRadius: '20px'
                                },
                                '&:hover': {
                                    backgroundColor: pathname === '/dashboard/api-executor'
                                        ? 'rgba(25, 118, 210, 0.18)'
                                        : 'rgba(99, 102, 241, 0.12)',
                                },
                                '&:hover .MuiOutlinedInput-notchedOutline': { 
                                    borderColor: pathname === '/dashboard/api-executor'
                                        ? 'primary.main'
                                        : 'rgba(99, 102, 241, 0.5)',
                                    borderWidth: '2px'
                                },
                                '&.Mui-disabled': {
                                    color: 'text.primary',
                                    backgroundColor: 'action.disabledBackground',
                                    opacity: 0.85,
                                },
                                '&.Mui-disabled .MuiOutlinedInput-notchedOutline': {
                                    borderColor: 'divider',
                                }
                            }}
                        >
                            {projects.map((project) => (
                                <MenuItem key={project.projectId || 0} value={project.projectId || 0}>
                                    {project.shortCode}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Toolbar>
            </AppBar>

            {/* Mobile drawer */}
            <Drawer
                variant="temporary"
                open={mobileOpen}
                onClose={handleDrawerToggle}
                ModalProps={{
                    keepMounted: true, // Better open performance on mobile.
                }}
                sx={{
                    display: { xs: 'block', md: 'none' },
                    '& .MuiDrawer-paper': {
                        boxSizing: 'border-box',
                        width: drawerWidth,
                    },
                }}
            >
                {drawer}
            </Drawer>

            {/* Desktop drawer */}
            <Drawer
                variant="permanent"
                open={desktopOpen}
                sx={{
                    display: { xs: 'none', md: 'block' },
                    width: desktopOpen ? drawerWidth : theme.spacing(7),
                    flexShrink: 0,
                    '& .MuiDrawer-paper': {
                        width: desktopOpen ? drawerWidth : theme.spacing(7),
                        boxSizing: 'border-box',
                        transition: theme.transitions.create('width', {
                            easing: theme.transitions.easing.sharp,
                            duration: theme.transitions.duration.enteringScreen,
                        }),
                        overflowX: 'hidden',
                    },
                }}
            >
                {drawer}
            </Drawer>

            {/* Main content */}
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    p: 3,
                    width: '100%',
                    minHeight: '100vh',
                    backgroundColor: theme.palette.background.default,
                }}
            >
                <Toolbar />
                {children}
            </Box>
        </Box>
    );
}
