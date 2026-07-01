'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Typography,
    Paper,
    Button,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    CircularProgress,
    Alert,
    Select,
    MenuItem,FormControl,
    InputLabel,
    Radio,
    RadioGroup,
    FormControlLabel,
    TablePagination,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ApiIcon from '@mui/icons-material/Api';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { apiInformationService } from '@/services/apiInformationService';
import { ApiInformationDto, GeneralFilter } from '@/types/api';
import { useProject } from '@/contexts/ProjectContext';
import { useRouter } from 'next/navigation';
import { getErrorMessage } from '@/lib/errorUtils';

export default function APIInformationPage() {
    const router = useRouter();
    const t = useTranslations('apiInfo');
    const { projects, selectedProject } = useProject();
    const [apis, setApis] = useState<ApiInformationDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingApi, setEditingApi] = useState<ApiInformationDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [deletingApiId, setDeletingApiId] = useState<number | null>(null);
    const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [totalCount, setTotalCount] = useState(0);
    const [searchQuery, setSearchQuery] = useState('');
    const [filteredApis, setFilteredApis] = useState<ApiInformationDto[]>([]);

    const [formData, setFormData] = useState<ApiInformationDto>({
        name: '',
        shortCode: '',
        apiShortCode: '',
        srvcName: '',
        httpMethod: 'GET',
        active: 'Aktif',
        mediaType: 'application/json',
        projectId: 1,
        statusCode: 200,
        grpc: false,
        isTokenApi: false,
        externalApi: false,
        sqlQuery: false,
    });

    useEffect(() => {
        // Sadece selectedProject yüklendiyse API'ları çek
        if (selectedProject) {
            fetchApis();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedProject?.projectId]);

    useEffect(() => {
        // Arama ve sıralama filtreleme
        let filtered = [...apis];
        
        // Arama filtreleme
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(api => 
                api.apiShortCode?.toLowerCase().includes(query) ||
                api.shortCode?.toLowerCase().includes(query) ||
                api.name?.toLowerCase().includes(query) ||
                api.srvcName?.toLowerCase().includes(query)
            );
        }
        
        // Sıralama
        filtered = filtered.sort((a, b) => {
            const idA = a.gnlApiInformationId || a.id || 0;
            const idB = b.gnlApiInformationId || b.id || 0;
            return sortOrder === 'asc' ? idA - idB : idB - idA;
        });
        
        setFilteredApis(filtered);
        setPage(0); // Filtre değiştiğinde ilk sayfaya dön
    }, [apis, searchQuery, sortOrder]);

    const fetchApis = async () => {
        try {
            setLoading(true);
            setError(null);
            
            // Tüm veriyi çek (filtresiz)
            const response = await apiInformationService.list({
                offset: 0,
                limit: 10000,
                filterList: []
            });
            
            // Client-side'da proje bazlı filtreleme yap
            let filteredData = [...response.data];
            
            // Eğer bir proje seçiliyse ve projectId geçerliyse, sadece o projeye ait API'ları göster
            if (selectedProject && selectedProject.projectId !== null && selectedProject.projectId !== undefined && selectedProject.projectId > 0) {
                filteredData = filteredData.filter(api => api.projectId === selectedProject.projectId);
            }
            
            // Sıralama
            const sortedData = filteredData.sort((a, b) => {
                const idA = a.gnlApiInformationId || a.id || 0;
                const idB = b.gnlApiInformationId || b.id || 0;
                return sortOrder === 'asc' ? idA - idB : idB - idA;
            });
            
            setApis(sortedData);
            setFilteredApis(sortedData);
            setTotalCount(sortedData.length);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to fetch APIs'));
        } finally {
            setLoading(false);
        }
    };


    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const toggleSortOrder = () => {
        setSortOrder(prev => prev === 'asc' ? 'desc' : 'asc');
    };

    const handleOpenDialog = (api?: ApiInformationDto) => {
        if (api) {
            setEditingApi(api);
            setFormData({
                ...api,
                mediaType: api.mediaType || 'application/json',
                active: api.active || 'Aktif',
            });
        } else {
            setEditingApi(null);
            setFormData({
                name: '',
                shortCode: '',
                apiShortCode: '',
                srvcName: '',
                httpMethod: 'GET',
                active: 'Aktif',
                mediaType: 'application/json',
                projectId: selectedProject?.projectId || 1,
                statusCode: 200,
                grpc: false,
                isTokenApi: false,
                externalApi: false,
                sqlQuery: false,
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingApi(null);
        setError(null);
    };

    const handleSave = async () => {
        try {
            setError(null);
            if (editingApi) {
                await apiInformationService.update(formData);
                setSuccess('API updated successfully');
            } else {
                await apiInformationService.save(formData);
                setSuccess('API created successfully');
            }
            handleCloseDialog();
            await fetchApis();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to save API'));
        }
    };

    const handleInputChange = <K extends keyof ApiInformationDto>(field: K, value: ApiInformationDto[K]) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    const handleViewDetail = (apiId: number) => {
        router.push(`/dashboard/api-information/${apiId}`);
    };

    const handleDeleteClick = (apiId: number) => {
        setDeletingApiId(apiId);
        setDeleteDialogOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (deletingApiId) {
            try {
                await apiInformationService.delete(deletingApiId);
                setSuccess('API deleted successfully');
                setDeleteDialogOpen(false);
                setDeletingApiId(null);
                await fetchApis();
                setTimeout(() => setSuccess(null), 3000);
            } catch (err) {
                setError(getErrorMessage(err, 'Failed to delete API'));
            }
        }
    };
    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center', 
                    mb: 4,
                    pb: 2,
                    borderBottom: '2px solid',
                    borderColor: 'divider'
                }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Box sx={{ 
                            p: 1.5, 
                            borderRadius: 2, 
                            backgroundColor: 'primary.main',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
                            <ApiIcon sx={{ fontSize: 32, color: 'white' }} />
                        </Box>
                        <Box>
                            <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
                                {t('title')}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {t('count', { count: filteredApis.length })}
                            </Typography>
                        </Box>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                        <TextField
                            placeholder={t('search')}
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            size="small"
                            InputProps={{
                                startAdornment: (
                                    <SearchIcon sx={{ color: 'text.secondary', mr: 1, ml: 0.5 }} />
                                ),
                            }}
                            sx={{ 
                                minWidth: 300,
                                '& .MuiOutlinedInput-root': {
                                    backgroundColor: 'background.paper',
                                    borderRadius: '12px',
                                    fontSize: '0.95rem',
                                    paddingLeft: '8px'
                                },
                                '& .MuiOutlinedInput-input': {
                                    paddingLeft: '4px'
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

                {loading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
                        <CircularProgress size={60} />
                    </Box>
                ) : filteredApis.length === 0 ? (
                    <Paper sx={{ p: 8, textAlign: 'center' }}>
                        <ApiIcon sx={{ fontSize: 80, color: 'text.disabled', mb: 2 }} />
                        <Typography variant="h6" color="text.secondary">
                            {searchQuery ? t('noResults') : t('noApis')}
                        </Typography>
                    </Paper>
                ) : (
                    <Paper variant="outlined" sx={{ overflow: 'hidden', borderRadius: 2, boxShadow: 2 }}>
                        <Box sx={{ overflow: 'auto', width: '100%' }}>
                            {/* Header Row */}
                            <Box sx={{ 
                                display: 'grid',
                                gridTemplateColumns: '100px 1fr 180px 150px',
                                alignItems: 'center', 
                                px: 3, 
                                py: 2,
                                backgroundColor: 'action.hover',
                                borderBottom: '1px solid',
                                borderColor: 'divider',
                                minWidth: '800px'
                            }}>
                                <Box 
                                    onClick={toggleSortOrder}
                                    sx={{ 
                                        cursor: 'pointer',
                                        '&:hover': { color: 'primary.main' },
                                        transition: 'color 0.2s'
                                    }}
                                >
                                    <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                        {t('apiId')} {sortOrder === 'asc' ? '↑' : '↓'}
                                    </Typography>
                                </Box>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('shortCode')}
                                </Typography>
                                <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('activeStatus')}
                                </Typography>
                                <Typography variant="body2" sx={{ textAlign: 'center', fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('actions')}
                                </Typography>
                            </Box>

                            {/* Data Rows */}
                            {filteredApis.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((api, index) => (
                                <Box 
                                    key={api.id || api.gnlApiInformationId}
                                    sx={{ 
                                        display: 'grid',
                                        gridTemplateColumns: '100px 1fr 180px 150px',
                                        alignItems: 'center',
                                        px: 3,
                                        py: 2,
                                        backgroundColor: index % 2 === 0 ? 'background.paper' : 'action.hover',
                                        borderBottom: index < filteredApis.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).length - 1 ? '1px solid' : 'none',
                                        borderColor: 'divider',
                                        transition: 'all 150ms ease',
                                        minWidth: '800px',
                                        '&:hover': {
                                            backgroundColor: 'action.selected',
                                        }
                                    }}
                                >
                                    {/* API ID */}
                                    <Typography variant="body2" sx={{ color: 'text.primary' }}>
                                        {api.id || api.gnlApiInformationId}
                                    </Typography>

                                    {/* Short Code */}
                                    <Typography variant="body2" sx={{ fontWeight: 500, color: 'primary.main', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                        {api.apiShortCode || api.shortCode}
                                    </Typography>

                                    {/* Active Status */}
                                    <Typography variant="body2" sx={{ textAlign: 'center' }}>
                                        {api.active || 'Aktif'}
                                    </Typography>

                                    {/* Actions */}
                                    <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                                        <IconButton
                                            size="small"
                                            onClick={() => handleViewDetail(api.id || api.gnlApiInformationId || 0)}
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
                                            onClick={() => handleDeleteClick(api.id || api.gnlApiInformationId || 0)}
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
                        
                        {/* Pagination */}
                        <TablePagination
                            component="div"
                            count={filteredApis.length}
                            page={page}
                            onPageChange={handleChangePage}
                            rowsPerPage={rowsPerPage}
                            onRowsPerPageChange={handleChangeRowsPerPage}
                            rowsPerPageOptions={[5, 10, 25, 50, 100]}
                            labelRowsPerPage="Sayfa başına satır:"
                            labelDisplayedRows={({ from, to, count }) => `${from}-${to} / ${count !== -1 ? count : `${to}'den fazla`}`}
                            sx={{
                                borderTop: '1px solid',
                                borderColor: 'divider',
                                '.MuiTablePagination-toolbar': {
                                    minHeight: '52px'
                                }
                            }}
                        />
                    </Paper>
                )}

                {/* Add/Edit Dialog */}
                <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="lg" fullWidth>
                    <DialogTitle>
                        {editingApi ? 'API Detay' : 'API Detay'}
                    </DialogTitle>
                    <DialogContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
                            {/* İstek Alanı - Tam Genişlik */}
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'start' }}>
                                <Typography variant="body2" sx={{ mt: 1, fontWeight: 600, color: 'text.primary' }}>İstek *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    multiline
                                    minRows={4}
                                    maxRows={20}
                                    value={formData.plIn || ''}
                                    onChange={(e) => handleInputChange('plIn', e.target.value)}
                                    size="small"
                                    sx={{
                                        '& .MuiOutlinedInput-root': {
                                            backgroundColor: 'background.paper',
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

                            {/* 2 Kolonlu Alan */}
                            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 3 }}>
                                {/* Sol Kolon */}
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                                    <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                        <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Adı *</Typography>
                                        <TextField
                                            fullWidth
                                            required
                                            value={formData.name}
                                            onChange={(e) => handleInputChange('name', e.target.value)}
                                            size="small"
                                            sx={{
                                                '& .MuiOutlinedInput-root': {
                                                    backgroundColor: 'background.paper',
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
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'start' }}>
                                    <Typography variant="body2" sx={{ mt: 1, fontWeight: 600, color: 'text.primary' }}>API URL *</Typography>
                                    <TextField
                                        fullWidth
                                        required
                                        multiline
                                        rows={2}
                                        value={formData.srvcName}
                                        onChange={(e) => handleInputChange('srvcName', e.target.value)}
                                        size="small"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'background.paper',
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
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Kısa Kod *</Typography>
                                    <TextField
                                        fullWidth
                                        required
                                        value={formData.apiShortCode || formData.shortCode}
                                        onChange={(e) => {
                                            handleInputChange('apiShortCode', e.target.value);
                                            handleInputChange('shortCode', e.target.value);
                                        }}
                                        size="small"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'background.paper',
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
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Header Param.</Typography>
                                    <TextField
                                        fullWidth
                                        value={formData.headerParameters || ''}
                                        onChange={(e) => handleInputChange('headerParameters', e.target.value)}
                                        size="small"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'background.paper',
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
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Cevap Kodu</Typography>
                                    <TextField
                                        fullWidth
                                        type="number"
                                        value={formData.statusCode || 200}
                                        onChange={(e) => handleInputChange('statusCode', parseInt(e.target.value))}
                                        size="small"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'background.paper',
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
                            </Box>

                            {/* Sağ Kolon */}
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>HTTP Metodu *</Typography>
                                    <Select
                                        fullWidth
                                        value={formData.httpMethod}
                                        onChange={(e) => handleInputChange('httpMethod', e.target.value)}
                                        size="small"
                                        sx={{
                                            backgroundColor: 'background.paper',
                                            '&:hover .MuiOutlinedInput-notchedOutline': {
                                                borderColor: 'primary.main',
                                                borderWidth: '2px'
                                            },
                                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                                                borderWidth: '2px'
                                            }
                                        }}
                                    >
                                        <MenuItem value="GET">GET</MenuItem>
                                        <MenuItem value="POST">POST</MenuItem>
                                        <MenuItem value="PUT">PUT</MenuItem>
                                        <MenuItem value="DELETE">DELETE</MenuItem>
                                        <MenuItem value="PATCH">PATCH</MenuItem>
                                    </Select>
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Aktif / Pasif *</Typography>
                                    <RadioGroup
                                        row
                                        value={formData.active}
                                        onChange={(e) => handleInputChange('active', e.target.value)}
                                    >
                                        <FormControlLabel value="Aktif" control={<Radio />} label="Aktif" />
                                        <FormControlLabel value="Pasif" control={<Radio />} label="Pasif" />
                                    </RadioGroup>
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Media Type *</Typography>
                                    <Select
                                        fullWidth
                                        value={formData.mediaType || 'application/json'}
                                        onChange={(e) => handleInputChange('mediaType', e.target.value)}
                                        size="small"
                                        sx={{
                                            backgroundColor: 'background.paper',
                                            '&:hover .MuiOutlinedInput-notchedOutline': {
                                                borderColor: 'primary.main',
                                                borderWidth: '2px'
                                            },
                                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                                                borderWidth: '2px'
                                            }
                                        }}
                                    >
                                        {/* Mevcut değer listede yoksa ekle */}
                                        {formData.mediaType && !['application/json', 'APPLICATION/JSON', 'application/xml', 'APPLICATION/XML', 'text/plain', 'text/html', 'text/xml;charset=UTF-8', 'multipart/form-data'].includes(formData.mediaType) && (
                                            <MenuItem value={formData.mediaType}>{formData.mediaType}</MenuItem>
                                        )}
                                        <MenuItem value="application/json">application/json</MenuItem>
                                        <MenuItem value="APPLICATION/JSON">APPLICATION/JSON</MenuItem>
                                        <MenuItem value="application/xml">application/xml</MenuItem>
                                        <MenuItem value="APPLICATION/XML">APPLICATION/XML</MenuItem>
                                        <MenuItem value="text/plain">text/plain</MenuItem>
                                        <MenuItem value="text/html">text/html</MenuItem>
                                        <MenuItem value="text/xml;charset=UTF-8">text/xml;charset=UTF-8</MenuItem>
                                        <MenuItem value="multipart/form-data">multipart/form-data</MenuItem>
                                    </Select>
                                </Box>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                    <Box sx={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <input
                                                type="checkbox"
                                                checked={formData.externalApi || false}
                                                onChange={(e) => handleInputChange('externalApi', e.target.checked)}
                                                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                                            />
                                            <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>External API</Typography>
                                        </Box>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <input
                                                type="checkbox"
                                                checked={formData.sqlQuery || false}
                                                onChange={(e) => handleInputChange('sqlQuery', e.target.checked)}
                                                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                                            />
                                            <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>SQL Query</Typography>
                                        </Box>
                                    </Box>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <input
                                            type="checkbox"
                                            checked={formData.isTokenApi || false}
                                            onChange={(e) => handleInputChange('isTokenApi', e.target.checked)}
                                            style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                                        />
                                        <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Token Api</Typography>
                                    </Box>
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Proje</Typography>
                                    <Select
                                        fullWidth
                                        value={formData.projectId || ''}
                                        onChange={(e) => handleInputChange('projectId', e.target.value as number)}
                                        size="small"
                                        sx={{
                                            backgroundColor: 'background.paper',
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
                                </Box>
                            </Box>
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
                            İptal
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
                            Kaydet
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Delete Confirmation Dialog */}
                <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
                    <DialogTitle>API Sil</DialogTitle>
                    <DialogContent>
                        <Typography>Bu API&apos;yi silmek istediğinizden emin misiniz?</Typography>
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
