'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    IconButton,
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
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    InputAdornment,
    Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SearchIcon from '@mui/icons-material/Search';
import StorageIcon from '@mui/icons-material/Storage';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { databaseConfigService, DatabaseConfigDto } from '@/services/databaseConfigService';
import { useProject } from '@/contexts/ProjectContext';
import { getErrorMessage } from '@/lib/errorUtils';

export default function EnvironmentsPage() {
    const t = useTranslations('environments');
    const { projects, selectedProject } = useProject();
    const [databases, setDatabases] = useState<DatabaseConfigDto[]>([]);
    const [filteredDatabases, setFilteredDatabases] = useState<DatabaseConfigDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingDatabase, setEditingDatabase] = useState<DatabaseConfigDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
    const [envToDelete, setEnvToDelete] = useState<number | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [copySuccess, setCopySuccess] = useState(false);

    const [formData, setFormData] = useState<DatabaseConfigDto>({
        shortCode: '',
        url: '',
        username: '',
        password: '',
        isActv: true,
        schema: 'public',
        driver: 'org.postgresql.Driver',
        projectId: 1,
    });

    useEffect(() => {
        loadDatabases();
    }, []);

    useEffect(() => {
        filterDatabasesByProject();
    }, [selectedProject, databases, searchQuery]);

    const loadDatabases = async () => {
        try {
            setLoading(true);
            setError(null);
            const databasesData = await databaseConfigService.getAll();
            
            // Backend'den actv geliyor, isActv'ye map et
            const normalizedData = databasesData.map(db => ({
                ...db,
                isActv: db.actv === true || db.isActv === true
            }));
            
            setDatabases(normalizedData);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to load data'));
        } finally {
            setLoading(false);
        }
    };

    const filterDatabasesByProject = () => {
        let filtered = databases;
        
        if (selectedProject) {
            filtered = filtered.filter(db => db.projectId === selectedProject.projectId);
        }
        
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(db => 
                db.shortCode?.toLowerCase().includes(query) ||
                db.url?.toLowerCase().includes(query) ||
                db.username?.toLowerCase().includes(query)
            );
        }
        
        setFilteredDatabases(filtered);
    };

    const handleOpenDialog = (database?: DatabaseConfigDto) => {
        if (database) {
            setEditingDatabase(database);
            setFormData({
                ...database,
                shortCode: database.shortCode || '',
                url: database.url || '',
                username: database.username || '',
                password: database.password || '',
                schema: database.schema || 'public',
                driver: database.driver || 'org.postgresql.Driver',
                isActv: database.isActv !== undefined ? database.isActv : true,
                projectId: database.projectId || selectedProject?.projectId || 1,
            });
        } else {
            setEditingDatabase(null);
            setFormData({
                shortCode: '',
                url: '',
                username: '',
                password: '',
                isActv: true,
                schema: 'public',
                driver: 'org.postgresql.Driver',
                projectId: selectedProject?.projectId || 1,
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingDatabase(null);
        setError(null);
        setShowPassword(false);
        setCopySuccess(false);
    };

    const handleSave = async () => {
        try {
            setError(null);
            
            // Backend'e actv alanı olarak gönder
            const dataToSave: DatabaseConfigDto = {
                ...formData,
                actv: formData.isActv
            };
            
            if (editingDatabase) {
                await databaseConfigService.update(dataToSave);
                setSuccess('Database updated successfully');
            } else {
                await databaseConfigService.save(dataToSave);
                setSuccess('Database created successfully');
            }
            handleCloseDialog();
            await loadDatabases();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to save database'));
        }
    };

    const handleInputChange = <K extends keyof DatabaseConfigDto>(field: K, value: DatabaseConfigDto[K]) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    const handleViewDetail = (dbId: number) => {
        const db = databases.find(d => d.dbConfigId === dbId);
        if (db) handleOpenDialog(db);
    };

    const handleDeleteClick = (databaseId: number) => {
        setEnvToDelete(databaseId);
        setDeleteConfirmOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (envToDelete) {
            try {
                await databaseConfigService.delete(envToDelete);
                setSuccess('Veritabanı bağlantısı başarıyla silindi');
                setDeleteConfirmOpen(false);
                setEnvToDelete(null);
                await loadDatabases();
                setTimeout(() => setSuccess(null), 3000);
            } catch (err) {
                setError(getErrorMessage(err, 'Veritabanı bağlantısı silinemedi'));
            }
        }
    };

    const handleCopyPassword = async () => {
        try {
            await navigator.clipboard.writeText(formData.password || '');
            setCopySuccess(true);
            setTimeout(() => setCopySuccess(false), 2000);
        } catch (err) {
            console.error('Failed to copy password:', err);
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

                {/* Databases List */}
                {loading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
                        <CircularProgress size={60} />
                    </Box>
                ) : filteredDatabases.length === 0 ? (
                    <Paper sx={{ p: 8, textAlign: 'center' }}>
                        <StorageIcon sx={{ fontSize: 80, color: 'text.disabled', mb: 2 }} />
                        <Typography variant="h6" color="text.secondary">
                            {t('noDatabases')}
                        </Typography>
                    </Paper>
                ) : (
                    <Paper variant="outlined" sx={{ overflow: 'hidden', borderRadius: 2, boxShadow: 2 }}>
                        <Box sx={{ overflow: 'auto', width: '100%' }}>
                            {/* Header Row */}
                            <Box sx={{ 
                                display: 'grid',
                                gridTemplateColumns: '80px 180px 1fr 150px 100px 150px',
                                alignItems: 'center', 
                                px: 3, 
                                py: 2,
                                backgroundColor: 'action.hover',
                                borderBottom: '1px solid',
                                borderColor: 'divider',
                                minWidth: '1000px'
                            }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('dbId')}
                                </Typography>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('shortCode')}
                                </Typography>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('jdbcUrl')}
                                </Typography>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('schema')}
                                </Typography>
                                <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('active')}
                                </Typography>
                                <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('actions')}
                                </Typography>
                            </Box>

                            {/* Data Rows */}
                            {filteredDatabases.map((database, index) => (
                                <Box 
                                    key={database.dbConfigId}
                                    sx={{ 
                                        display: 'grid',
                                        gridTemplateColumns: '80px 180px 1fr 150px 100px 150px',
                                        alignItems: 'center',
                                        px: 3,
                                        py: 2,
                                        backgroundColor: index % 2 === 0 ? 'background.paper' : 'action.hover',
                                        borderBottom: index < filteredDatabases.length - 1 ? '1px solid' : 'none',
                                        borderColor: 'divider',
                                        transition: 'all 150ms ease',
                                        minWidth: '1000px',
                                        '&:hover': {
                                            backgroundColor: 'action.selected',
                                        }
                                    }}
                                >
                                    <Typography variant="body2" sx={{ color: 'text.primary' }}>
                                        {database.dbConfigId}
                                    </Typography>

                                    <Typography variant="body2" sx={{ fontWeight: 500, color: 'primary.main' }}>
                                        {database.shortCode}
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
                                        title={database.url}
                                    >
                                        {database.url}
                                    </Typography>

                                    <Typography variant="body2">
                                        {database.schema}
                                    </Typography>

                                    <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                                        <Box
                                            sx={{ 
                                                display: 'inline-flex',
                                                alignItems: 'center',
                                                gap: 0.5,
                                                px: 1.5,
                                                py: 0.5,
                                                borderRadius: '16px',
                                                backgroundColor: database.isActv ? 'rgba(46, 125, 50, 0.1)' : 'rgba(211, 47, 47, 0.1)',
                                                border: '1px solid',
                                                borderColor: database.isActv ? 'success.main' : 'error.main',
                                            }}
                                        >
                                            <Box
                                                sx={{
                                                    width: 8,
                                                    height: 8,
                                                    borderRadius: '50%',
                                                    backgroundColor: database.isActv ? 'success.main' : 'error.main',
                                                }}
                                            />
                                            <Typography 
                                                variant="body2" 
                                                sx={{ 
                                                    fontWeight: 600,
                                                    fontSize: '0.8rem',
                                                    color: database.isActv ? 'success.main' : 'error.main'
                                                }}
                                            >
                                                {database.isActv ? 'Aktif' : 'Pasif'}
                                            </Typography>
                                        </Box>
                                    </Box>

                                    <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                                        <IconButton
                                            size="small"
                                            onClick={() => handleOpenDialog(database)}
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
                                            onClick={() => handleDeleteClick(database.dbConfigId!)}
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
                        {editingDatabase ? t('editDatabase') : t('addNew')}
                    </DialogTitle>
                    <DialogContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
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
                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary', mt: 1 }}>{t('jdbcUrl')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    multiline
                                    rows={2}
                                    value={formData.url}
                                    onChange={(e) => handleInputChange('url', e.target.value)}
                                    placeholder="jdbc:postgresql://localhost:5432/mydb"
                                    size="small"
                                    sx={{
                                        '& .MuiOutlinedInput-root': {
                                            fontFamily: 'monospace',
                                            fontSize: '0.875rem',
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
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('username')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    value={formData.username}
                                    onChange={(e) => handleInputChange('username', e.target.value)}
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
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('password')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    type={showPassword ? 'text' : 'password'}
                                    value={formData.password}
                                    onChange={(e) => handleInputChange('password', e.target.value)}
                                    size="small"
                                    InputProps={{
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <Tooltip title={showPassword ? "Şifreyi Gizle" : "Şifreyi Göster"}>
                                                    <IconButton
                                                        onClick={() => setShowPassword(!showPassword)}
                                                        edge="end"
                                                        size="small"
                                                    >
                                                        {showPassword ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title={copySuccess ? "Kopyalandı!" : "Şifreyi Kopyala"}>
                                                    <IconButton
                                                        onClick={handleCopyPassword}
                                                        edge="end"
                                                        size="small"
                                                        color={copySuccess ? "success" : "default"}
                                                    >
                                                        <ContentCopyIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                            </InputAdornment>
                                        ),
                                    }}
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
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('schema')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    value={formData.schema}
                                    onChange={(e) => handleInputChange('schema', e.target.value)}
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
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('driver')} *</Typography>
                                <FormControl fullWidth required size="small">
                                    <Select
                                        value={formData.driver}
                                        onChange={(e) => handleInputChange('driver', e.target.value)}
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
                                        <MenuItem value="org.postgresql.Driver">PostgreSQL</MenuItem>
                                        <MenuItem value="com.mysql.cj.jdbc.Driver">MySQL</MenuItem>
                                        <MenuItem value="oracle.jdbc.driver.OracleDriver">Oracle</MenuItem>
                                        <MenuItem value="com.microsoft.sqlserver.jdbc.SQLServerDriver">SQL Server</MenuItem>
                                    </Select>
                                </FormControl>
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
                                            checked={formData.isActv}
                                            onChange={(e) => handleInputChange('isActv', e.target.checked)}
                                        />
                                    }
                                    label={formData.isActv ? t('active') : t('passive')}
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
                <Dialog open={deleteConfirmOpen} onClose={() => setDeleteConfirmOpen(false)}>
                    <DialogTitle>Veritabanı Bağlantısı Sil</DialogTitle>
                    <DialogContent>
                        <Typography>Bu veritabanı bağlantısını silmek istediğinizden emin misiniz?</Typography>
                    </DialogContent>
                    <DialogActions sx={{ px: 3, pb: 2 }}>
                        <Button 
                            onClick={() => setDeleteConfirmOpen(false)}
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
