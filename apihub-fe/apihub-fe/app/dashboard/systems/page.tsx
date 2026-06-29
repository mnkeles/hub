'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    IconButton,
    Chip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Switch,
    FormControlLabel,
    CircularProgress,
    Alert,
    Card,
    CardContent,
    Divider,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import StorageIcon from '@mui/icons-material/Storage';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { generalWebSystemService } from '@/services/generalWebSystemService';
import { GeneralWebSystemDto } from '@/types/api';
import { useProject } from '@/contexts/ProjectContext';
import { getErrorMessage } from '@/lib/errorUtils';

export default function SystemsPage() {
    const t = useTranslations('systems');
    const { projects, selectedProject } = useProject();
    const [systems, setSystems] = useState<GeneralWebSystemDto[]>([]);
    const [filteredSystems, setFilteredSystems] = useState<GeneralWebSystemDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingSystem, setEditingSystem] = useState<GeneralWebSystemDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [deletingSystemId, setDeletingSystemId] = useState<number | null>(null);
    const [searchQuery, setSearchQuery] = useState('');

    const [formData, setFormData] = useState<GeneralWebSystemDto>({
        gnlWebSysId: null,
        name: '',
        shortCode: '',
        url: '',
        actv: true,
        projectId: 1,
        isTokenUrl: false,
        baseUrlShortCode: '',
    });

    useEffect(() => {
        loadSystems();
    }, []);

    useEffect(() => {
        filterSystemsByProject();
    }, [selectedProject, systems, searchQuery]);

    const loadSystems = async () => {
        try {
            setLoading(true);
            setError(null);
            const systemsData = await generalWebSystemService.getAll();
            setSystems(systemsData);
        } catch (err) {
            console.error('Error loading data:', err);
            setError(getErrorMessage(err, 'Failed to load data'));
        } finally {
            setLoading(false);
        }
    };

    const filterSystemsByProject = () => {
        let filtered = systems;
        
        // Proje filtreleme
        if (selectedProject) {
            filtered = filtered.filter(sys => sys.projectId === selectedProject.projectId);
        }
        
        // Arama filtreleme
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(sys => 
                sys.name?.toLowerCase().includes(query) ||
                sys.shortCode?.toLowerCase().includes(query) ||
                sys.url?.toLowerCase().includes(query)
            );
        }
        
        setFilteredSystems(filtered);
    };

    const handleOpenDialog = (system?: GeneralWebSystemDto) => {
        if (system) {
            console.log('Opening dialog with system:', system);
            setEditingSystem(system);
            setFormData({
                ...system,
                gnlWebSysId: system.gnlWebSysId || null,
                name: system.name || '',
                shortCode: system.shortCode || '',
                url: system.url || '',
                actv: system.actv !== undefined ? system.actv : true,
                projectId: system.projectId || selectedProject?.projectId || 1,
                isTokenUrl: system.isTokenUrl !== undefined ? system.isTokenUrl : false,
                baseUrlShortCode: system.baseUrlShortCode || '',
            });
        } else {
            setEditingSystem(null);
            setFormData({
                gnlWebSysId: null,
                name: '',
                shortCode: '',
                url: '',
                actv: true,
                projectId: selectedProject?.projectId || 1,
                isTokenUrl: false,
                baseUrlShortCode: '',
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingSystem(null);
        setError(null);
    };

    const handleSave = async () => {
        try {
            setError(null);
            await generalWebSystemService.save(formData);
            setSuccess(editingSystem ? 'System updated successfully' : 'System created successfully');
            handleCloseDialog();
            await loadSystems();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to save system'));
        }
    };

    const handleInputChange = <K extends keyof GeneralWebSystemDto>(field: K, value: GeneralWebSystemDto[K]) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    const handleDeleteClick = (systemId: number) => {
        setDeletingSystemId(systemId);
        setDeleteDialogOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (deletingSystemId) {
            try {
                await generalWebSystemService.delete(deletingSystemId);
                setSuccess('Ortam bağlantısı başarıyla silindi');
                setDeleteDialogOpen(false);
                setDeletingSystemId(null);
                await loadSystems();
                setTimeout(() => setSuccess(null), 3000);
            } catch (err) {
                setError(getErrorMessage(err, 'Ortam bağlantısı silinemedi'));
            }
        }
    };

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <StorageIcon sx={{ fontSize: 40, color: 'primary.main' }} />
                        <Typography variant="h4" sx={{ fontWeight: 700 }}>
                            {t('title')}
                        </Typography>
                    </Box>
                    
                    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                        <TextField
                            placeholder={t('search')}
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            size="small"
                            InputProps={{
                                startAdornment: <SearchIcon sx={{ color: 'text.secondary', mr: 1 }} />
                            }}
                            sx={{ 
                                minWidth: 300,
                                '& .MuiOutlinedInput-root': {
                                    backgroundColor: 'background.paper',
                                    borderRadius: '12px',
                                    fontSize: '0.95rem'
                                }
                            }}
                        />
                        <Button
                            variant="contained"
                            startIcon={<AddIcon />}
                            onClick={() => handleOpenDialog()}
                            sx={{
                                px: 3,
                                py: 1.5,
                                borderRadius: '12px',
                                textTransform: 'none',
                                fontSize: '1rem',
                                fontWeight: 600,
                                boxShadow: 2,
                                '&:hover': {
                                    boxShadow: 4,
                                }
                            }}
                        >
                            {t('addNew')}
                        </Button>
                    </Box>
                </Box>

                {error && (
                    <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                        {error}
                    </Alert>
                )}

                {success && (
                    <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
                        {success}
                    </Alert>
                )}

                {/* Systems List */}
                {loading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
                        <CircularProgress size={60} />
                    </Box>
                ) : filteredSystems.length === 0 ? (
                    <Paper sx={{ p: 8, textAlign: 'center' }}>
                        <StorageIcon sx={{ fontSize: 80, color: 'text.disabled', mb: 2 }} />
                        <Typography variant="h6" color="text.secondary">
                            {t('noSystems')}
                        </Typography>
                    </Paper>
                ) : (
                    <Paper variant="outlined" sx={{ overflow: 'hidden', borderRadius: 2, boxShadow: 2 }}>
                        <Box sx={{ overflow: 'auto', width: '100%' }}>
                            {/* Header Row */}
                            <Box sx={{ 
                                display: 'grid',
                                gridTemplateColumns: '100px 200px 1fr 120px 150px',
                                alignItems: 'center', 
                                px: 3, 
                                py: 2,
                                backgroundColor: 'action.hover',
                                borderBottom: '1px solid',
                                borderColor: 'divider',
                                minWidth: '900px'
                            }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    Database ID
                                </Typography>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('shortCode')}
                                </Typography>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('url')}
                                </Typography>
                                <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('active')}
                                </Typography>
                                <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('actions')}
                                </Typography>
                            </Box>

                            {/* Data Rows */}
                            {filteredSystems.map((system, index) => (
                                <Box 
                                    key={system.gnlWebSysId}
                                    sx={{ 
                                        display: 'grid',
                                        gridTemplateColumns: '100px 200px 1fr 120px 150px',
                                        alignItems: 'center',
                                        px: 3,
                                        py: 2,
                                        backgroundColor: index % 2 === 0 ? 'background.paper' : 'action.hover',
                                        borderBottom: index < filteredSystems.length - 1 ? '1px solid' : 'none',
                                        borderColor: 'divider',
                                        transition: 'all 150ms ease',
                                        minWidth: '900px',
                                        '&:hover': {
                                            backgroundColor: 'action.selected',
                                        }
                                    }}
                                >
                                    <Typography variant="body2" sx={{ color: 'text.primary' }}>
                                        {system.gnlWebSysId}
                                    </Typography>

                                    <Typography variant="body2" sx={{ fontWeight: 500, color: 'primary.main' }}>
                                        {system.shortCode}
                                    </Typography>

                                    <Typography 
                                        variant="body2" 
                                        sx={{ 
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap',
                                            color: 'primary.main',
                                            cursor: 'pointer',
                                            '&:hover': {
                                                textDecoration: 'underline'
                                            }
                                        }}
                                        onClick={() => window.open(system.url, '_blank')}
                                        title={system.url}
                                    >
                                        {system.url}
                                    </Typography>

                                    <Typography 
                                        variant="body2" 
                                        sx={{ textAlign: 'center', fontWeight: 500 }}
                                    >
                                        {system.actv ? 'True' : 'False'}
                                    </Typography>

                                    <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                                        <IconButton
                                            size="small"
                                            onClick={() => handleOpenDialog(system)}
                                            title="Düzenle"
                                            sx={{ 
                                                transition: 'all 150ms ease',
                                                '&:hover': { color: 'primary.main' }
                                            }}
                                        >
                                            <EditIcon fontSize="small" />
                                        </IconButton>
                                        <IconButton
                                            size="small"
                                            onClick={() => handleDeleteClick(system.gnlWebSysId!)}
                                            title="Sil"
                                            sx={{ 
                                                transition: 'all 150ms ease',
                                                '&:hover': { color: 'error.main' }
                                            }}
                                        >
                                            <DeleteIcon fontSize="small" />
                                        </IconButton>
                                    </Box>
                                </Box>
                            ))}
                        </Box>
                    </Paper>
                )}

                {/* Add/Edit Dialog */}
                <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
                    <DialogTitle sx={{ fontWeight: 600, fontSize: '1.25rem' }}>
                        {editingSystem ? t('editSystem') : t('addNew')}
                    </DialogTitle>
                    <DialogContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('name')}</Typography>
                                <TextField
                                    fullWidth
                                    value={formData.name}
                                    onChange={(e) => handleInputChange('name', e.target.value)}
                                    size="small"
                                    sx={{
                                        '& .MuiOutlinedInput-root': {
                                            '&:hover fieldset': {
                                                borderColor: 'primary.main',
                                                borderWidth: '2px'
                                            },
                                            '&.Mui-focused fieldset': {
                                                borderWidth: '2px'
                                            }
                                        }
                                    }}
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('shortCode')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    value={formData.shortCode}
                                    onChange={(e) => handleInputChange('shortCode', e.target.value)}
                                    size="small"
                                    sx={{
                                        '& .MuiOutlinedInput-root': {
                                            '&:hover fieldset': {
                                                borderColor: 'primary.main',
                                                borderWidth: '2px'
                                            },
                                            '&.Mui-focused fieldset': {
                                                borderWidth: '2px'
                                            }
                                        }
                                    }}
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('url')}</Typography>
                                <TextField
                                    fullWidth
                                    value={formData.url}
                                    onChange={(e) => handleInputChange('url', e.target.value)}
                                    size="small"
                                    sx={{
                                        '& .MuiOutlinedInput-root': {
                                            '&:hover fieldset': {
                                                borderColor: 'primary.main',
                                                borderWidth: '2px'
                                            },
                                            '&.Mui-focused fieldset': {
                                                borderWidth: '2px'
                                            }
                                        }
                                    }}
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('project')} *</Typography>
                                <FormControl fullWidth required size="small">
                                    <Select
                                        value={formData.projectId || ''}
                                        onChange={(e) => handleInputChange('projectId', e.target.value as number)}
                                        sx={{
                                            '&:hover .MuiOutlinedInput-notchedOutline': {
                                                borderColor: 'primary.main',
                                                borderWidth: '2px'
                                            },
                                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                                                borderWidth: '2px'
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
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('active')}</Typography>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={formData.actv}
                                            onChange={(e) => handleInputChange('actv', e.target.checked)}
                                        />
                                    }
                                    label={formData.actv ? t('active') : t('passive')}
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('tokenUrl')}</Typography>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={formData.isTokenUrl}
                                            onChange={(e) => handleInputChange('isTokenUrl', e.target.checked)}
                                        />
                                    }
                                    label={formData.isTokenUrl ? t('yes') : t('no')}
                                />
                            </Box>
                        </Box>
                    </DialogContent>
                    <DialogActions sx={{ px: 3, pb: 2 }}>
                        <Button 
                            onClick={handleCloseDialog}
                            variant="outlined"
                            sx={{
                                borderRadius: 1.5,
                                textTransform: 'none',
                                px: 3
                            }}
                        >
                            {t('cancel')}
                        </Button>
                        <Button 
                            onClick={handleSave} 
                            variant="contained"
                            sx={{
                                borderRadius: 1.5,
                                boxShadow: 1,
                                transition: 'all 200ms ease',
                                textTransform: 'none',
                                px: 3,
                                '&:hover': {
                                    boxShadow: 3,
                                }
                            }}
                        >
                            {t('save')}
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Delete Confirmation Dialog */}
                <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
                    <DialogTitle>Ortam Bağlantısı Sil</DialogTitle>
                    <DialogContent>
                        <Typography>Bu ortam bağlantısını silmek istediğinizden emin misiniz?</Typography>
                    </DialogContent>
                    <DialogActions sx={{ px: 3, pb: 2 }}>
                        <Button 
                            onClick={() => setDeleteDialogOpen(false)}
                            variant="outlined"
                            sx={{
                                borderRadius: 1.5,
                                textTransform: 'none',
                                px: 3
                            }}
                        >
                            İptal
                        </Button>
                        <Button 
                            onClick={handleDeleteConfirm} 
                            color="error" 
                            variant="contained"
                            sx={{
                                borderRadius: 1.5,
                                boxShadow: 1,
                                transition: 'all 200ms ease',
                                textTransform: 'none',
                                px: 3,
                                '&:hover': {
                                    boxShadow: 3,
                                }
                            }}
                        >
                            Sil
                        </Button>
                    </DialogActions>
                </Dialog>
            </Box>
        </DashboardLayout>
    );
}
