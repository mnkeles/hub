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
    CircularProgress,
    Alert,
    TablePagination,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import SearchIcon from '@mui/icons-material/Search';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import FloatingChat from '@/components/FloatingChat';
import { processFlowService } from '@/services/processFlowService';
import { ProcessFlowDto } from '@/types/api';
import { useRouter } from 'next/navigation';
import { useProject } from '@/contexts/ProjectContext';
import { getApiErrorStatus, getErrorMessage } from '@/lib/errorUtils';

export default function ProcessFlowsPage() {
    const router = useRouter();
    const t = useTranslations('flows');
    const { projects, selectedProject } = useProject();
    const [flows, setFlows] = useState<ProcessFlowDto[]>([]);
    const [filteredFlows, setFilteredFlows] = useState<ProcessFlowDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingFlow, setEditingFlow] = useState<ProcessFlowDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
    const [flowToDelete, setFlowToDelete] = useState<number | null>(null);
    const [copyConfirmOpen, setCopyConfirmOpen] = useState(false);
    const [flowToCopy, setFlowToCopy] = useState<ProcessFlowDto | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    const [formData, setFormData] = useState<ProcessFlowDto>({
        processFlowId: null,
        shortCode: '',
        isActive: 'true',
        projectId: 1,
        systemShortCode: '',
    });

    const fetchFlows = async () => {
        try {
            setLoading(true);
            setError(null);
            
            // Seçili projeye göre akışları getir
            if (!selectedProject?.projectId) {
                setFlows([]);
                return;
            }

            let flowsData: ProcessFlowDto[] = [];
            try {
                flowsData = await processFlowService.getByProject(selectedProject.projectId);
            } catch (error) {
                console.error('Failed to fetch flows:', error);
                flowsData = [];
            }
            
            // ID'ye göre sırala (küçükten büyüğe)
            const sortedFlows = flowsData.sort((a, b) => {
                const idA = a.processFlowId || 0;
                const idB = b.processFlowId || 0;
                return idA - idB;
            });
            setFlows(sortedFlows);
        } catch (err) {
            console.error('Error fetching flows:', err);
            if (getApiErrorStatus(err) === 404) {
                setFlows([]);
            } else {
                setError(getErrorMessage(err, 'Failed to fetch process flows'));
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFlows();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedProject]);


    useEffect(() => {
        let filtered = flows;
        
        // Sadece arama filtreleme (proje filtresi backend'de yapılıyor)
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(flow => 
                flow.shortCode?.toLowerCase().includes(query) ||
                flow.systemShortCode?.toLowerCase().includes(query)
            );
        }
        
        setFilteredFlows(filtered);
        setPage(0);
    }, [flows, searchQuery]);

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    // Sayfalanmış verileri hesapla
    const paginatedFlows = filteredFlows.slice(
        page * rowsPerPage,
        page * rowsPerPage + rowsPerPage
    );

    const handleOpenDialog = (flow?: ProcessFlowDto) => {
        if (flow) {
            setEditingFlow(flow);
            // isActive değerini "Aktif"/"Pasif" string'inden 'true'/'false'ye dönüştür
            const isActiveValue = flow.isActive === 'Aktif' ? 'true' : 'false';
            
            setFormData({
                ...flow,
                isActive: isActiveValue
            });
        } else {
            setEditingFlow(null);
            const defaultProjectId = selectedProject?.projectId || 1;
            setFormData({
                processFlowId: null,
                shortCode: '',
                isActive: 'true',
                projectId: defaultProjectId,
                systemShortCode: '',
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingFlow(null);
        setError(null);
    };

    const handleCopyClick = (flow: ProcessFlowDto) => {
        setFlowToCopy(flow);
        setCopyConfirmOpen(true);
    };

    const handleCopyConfirm = async () => {
        if (!flowToCopy) return;

        try {
            setLoading(true);
            setError(null);
            setCopyConfirmOpen(false);
            
            // Backend'deki copy endpoint'ini kullan (tüm adımlar ve parametrelerle birlikte)
            const copyResult = await processFlowService.copy(flowToCopy.processFlowId!);
            
            if (!copyResult.success) {
                throw new Error(copyResult.message || 'Akış kopyalanamadı');
            }
            
            // Backend'den dönen ID'yi al
            const newFlowId = copyResult.data;
            
            setSuccess('Akış başarıyla kopyalandı! Tüm adımlar ve parametreler kopyalandı.');
            
            // Listeyi yenile
            await fetchFlows();
            
            // Yeni akışın adımlarına git
            if (newFlowId) {
                setTimeout(() => {
                    router.push(`/dashboard/process-flows/${newFlowId}/steps`);
                }, 1500);
            }
            
        } catch (err) {
            console.error('Copy error:', err);
            setError(getErrorMessage(err, 'Akış kopyalanırken hata oluştu'));
        } finally {
            setLoading(false);
            setFlowToCopy(null);
        }
    };

    const handleSave = async () => {
        try {
            setError(null);
            
            // isActive değerini 'true'/'false'den 'Aktif'/'Pasif'e dönüştür
            const dataToSave = {
                ...formData,
                isActive: formData.isActive === 'true' ? 'Aktif' : 'Pasif',
                projectId: selectedProject?.projectId || formData.projectId
            };
            
            let result;
            if (editingFlow) {
                result = await processFlowService.update(dataToSave);
                setSuccess('Akış başarıyla güncellendi');
            } else {
                result = await processFlowService.save(dataToSave);
                setSuccess('Akış başarıyla oluşturuldu');
            }
            
            handleCloseDialog();
            
            // Tüm akışları yeniden yükle (getAll kullanıyor, cache sorunu yok)
            await fetchFlows();
            
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            console.error('Save error:', err);
            setError(getErrorMessage(err, 'Akış kaydedilemedi'));
        }
    };

    const handleDeleteClick = (flowId: number) => {
        setFlowToDelete(flowId);
        setDeleteConfirmOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (flowToDelete) {
            try {
                const deleteResult = await processFlowService.delete(flowToDelete);
                if (!deleteResult.success) {
                    throw new Error(deleteResult.message || 'Akış silinemedi');
                }
                
                // Silinen flow'u state'den direkt kaldır (anında güncelleme)
                setFlows(prevFlows => prevFlows.filter(f => f.processFlowId !== flowToDelete));
                
                setDeleteConfirmOpen(false);
                setFlowToDelete(null);
                
                setSuccess('Akış başarıyla silindi');
                setTimeout(() => setSuccess(null), 3000);
            } catch (err) {
                console.error('Delete error:', err);
                setError(getErrorMessage(err, 'Akış silinirken hata oluştu'));
                setDeleteConfirmOpen(false);
                setFlowToDelete(null);
            }
        }
    };

    const handleInputChange = <K extends keyof ProcessFlowDto>(field: K, value: ProcessFlowDto[K]) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    const handleViewSteps = (flowId: number) => {
        router.push(`/dashboard/process-flows/${flowId}/steps`);
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
                            <AccountTreeIcon sx={{ fontSize: 32, color: 'white' }} />
                        </Box>
                        <Box>
                            <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
                                {t('title')}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {t('count', { count: filteredFlows.length })}
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
                            variant="outlined"
                            startIcon={<UploadFileIcon />}
                            onClick={() => router.push('/dashboard/process-flows/import-har')}
                            sx={{
                                px: 2.5,
                                py: 1.5,
                                borderRadius: '12px',
                                textTransform: 'none',
                                fontSize: '0.95rem',
                                fontWeight: 600,
                            }}
                        >
                            {t('importHar')}
                        </Button>
                        <Button
                            variant="contained"
                            startIcon={<AddIcon />}
                            onClick={() => handleOpenDialog()}
                            data-testid="add-flow-button"
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
                ) : filteredFlows.length === 0 ? (
                    <Paper sx={{ p: 8, textAlign: 'center' }}>
                        <AccountTreeIcon sx={{ fontSize: 80, color: 'text.disabled', mb: 2 }} />
                        <Typography variant="h6" color="text.secondary">
                            {t('noFlows')}
                        </Typography>
                    </Paper>
                ) : (
                    <Paper variant="outlined" sx={{ overflow: 'hidden', borderRadius: 2, boxShadow: 2 }}>
                        <Box sx={{ overflow: 'auto', width: '100%' }}>
                            {/* Header Row */}
                            <Box sx={{ 
                                display: 'grid',
                                gridTemplateColumns: '100px 1fr 180px 200px',
                                alignItems: 'center', 
                                px: 3, 
                                py: 2,
                                backgroundColor: 'action.hover',
                                borderBottom: '1px solid',
                                borderColor: 'divider',
                                minWidth: '800px'
                            }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>
                                    {t('flowId')} ↕
                                </Typography>
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
                            {paginatedFlows.map((flow, index) => (
                                <Box 
                                    key={flow.processFlowId}
                                    sx={{ 
                                        display: 'grid',
                                        gridTemplateColumns: '100px 1fr 180px 200px',
                                        alignItems: 'center',
                                        px: 3,
                                        py: 2,
                                        backgroundColor: index % 2 === 0 ? 'background.paper' : 'action.hover',
                                        borderBottom: index < paginatedFlows.length - 1 ? '1px solid' : 'none',
                                        borderColor: 'divider',
                                        transition: 'all 150ms ease',
                                        minWidth: '800px',
                                        '&:hover': {
                                            backgroundColor: 'action.selected',
                                        }
                                    }}
                                >
                                    {/* Flow ID */}
                                    <Typography variant="body2" sx={{ color: 'text.primary' }}>
                                        {flow.processFlowId}
                                    </Typography>

                                    {/* Short Code */}
                                    <Typography variant="body2" sx={{ fontWeight: 500, color: 'primary.main', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                        {flow.shortCode}
                                    </Typography>

                                    {/* Active Status */}
                                    <Typography variant="body2" sx={{ textAlign: 'center' }}>
                                        {flow.isActive}
                                    </Typography>

                                    {/* Actions */}
                                    <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                                    <Button
                                        size="small"
                                        variant="contained"
                                        onClick={() => handleViewSteps(flow.processFlowId!)}
                                        sx={{ 
                                            minWidth: 'auto',
                                            px: 2,
                                            py: 0.5,
                                            fontSize: '0.75rem',
                                            textTransform: 'none',
                                            borderRadius: 1.5,
                                            boxShadow: 1,
                                            transition: 'all 200ms ease',
                                            '&:hover': {
                                                boxShadow: 3,
                                            }
                                        }}
                                    >
                                        {t('detail')}
                                    </Button>
                                    <IconButton
                                        size="small"
                                        onClick={() => handleOpenDialog(flow)}
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
                                        onClick={() => handleCopyClick(flow)}
                                        disabled={loading}
                                        title="Kopyala (Tüm adımlar ve parametreler)"
                                        sx={{ 
                                            transition: 'all 150ms ease',
                                            '&:hover': { color: 'success.main' }
                                        }}
                                    >
                                        <ContentCopyIcon fontSize="small" />
                                    </IconButton>
                                    <IconButton
                                        size="small"
                                        onClick={() => handleDeleteClick(flow.processFlowId!)}
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
                            count={filteredFlows.length}
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
                <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                    <DialogTitle sx={{ borderBottom: 1, borderColor: 'divider', pb: 2, pt: 2 }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 600, fontSize: '1.1rem' }}>
                            {editingFlow ? 'Akış Düzenle' : 'Yeni Akış Ekle'}
                        </Typography>
                    </DialogTitle>
                    <DialogContent sx={{ mt: 2, pt: 2 }}>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>Kısa Kod * <Typography component="span" variant="caption" color="text.secondary">(zorunlu alan)</Typography></Typography>
                                <TextField
                                    fullWidth
                                    required
                                    value={formData.shortCode}
                                    onChange={(e) => handleInputChange('shortCode', e.target.value)}
                                    size="small"
                                    placeholder="PrePaidData"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>Aktif / Pasif *</Typography>
                                <Box sx={{ display: 'flex', gap: 2 }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                        <input 
                                            type="radio" 
                                            checked={formData.isActive === 'true'} 
                                            onChange={() => handleInputChange('isActive', 'true')}
                                        />
                                        <Typography variant="body2">Aktif</Typography>
                                    </Box>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                        <input 
                                            type="radio" 
                                            checked={formData.isActive === 'false'} 
                                            onChange={() => handleInputChange('isActive', 'false')}
                                        />
                                        <Typography variant="body2">Pasif</Typography>
                                    </Box>
                                </Box>
                            </Box>
                        </Box>
                    </DialogContent>
                    <DialogActions sx={{ px: 2, py: 1.5, justifyContent: editingFlow ? 'space-between' : 'flex-end' }}>
                        {editingFlow && (
                            <Button 
                                onClick={() => {
                                    handleCloseDialog();
                                    handleDeleteClick(editingFlow.processFlowId!);
                                }}
                                color="error"
                                variant="contained"
                                sx={{
                                    textTransform: 'none',
                                    px: 2,
                                    py: 0.75
                                }}
                            >
                                Sil
                            </Button>
                        )}
                        <Box sx={{ display: 'flex', gap: 1 }}>
                            <Button 
                                onClick={handleCloseDialog}
                                variant="outlined"
                                sx={{
                                    textTransform: 'none',
                                    px: 2,
                                    py: 0.75
                                }}
                            >
                                İptal
                            </Button>
                            <Button 
                                onClick={handleSave} 
                                variant="contained"
                                sx={{
                                    textTransform: 'none',
                                    px: 2,
                                    py: 0.75
                                }}
                            >
                                Kaydet
                            </Button>
                        </Box>
                    </DialogActions>
                </Dialog>

                {/* Copy Confirmation Dialog */}
                <Dialog open={copyConfirmOpen} onClose={() => setCopyConfirmOpen(false)}>
                    <DialogTitle>Akış Kopyala</DialogTitle>
                    <DialogContent>
                        <Typography>
                            <strong>{flowToCopy?.shortCode}</strong> akışını kopyalamak istediğinizden emin misiniz?
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                            Tüm adımlar ve parametreler kopyalanacaktır.
                        </Typography>
                    </DialogContent>
                    <DialogActions sx={{ px: 3, pb: 2 }}>
                        <Button 
                            onClick={() => setCopyConfirmOpen(false)}
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
                            onClick={handleCopyConfirm} 
                            color="success" 
                            variant="contained"
                            sx={{
                                borderRadius: 1.5,
                                textTransform: 'none',
                                px: 3
                            }}
                        >
                            Evet, Kopyala
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Delete Confirmation Dialog */}
                <Dialog open={deleteConfirmOpen} onClose={() => setDeleteConfirmOpen(false)}>
                    <DialogTitle>Akış Sil</DialogTitle>
                    <DialogContent>
                        <Typography>Bu akışı silmek istediğinizden emin misiniz?</Typography>
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

                {/* Floating Chat Component */}
                <FloatingChat
                    title={t('aiAssistantTitle')}
                    subtitle={t('aiAssistantSubtitle')}
                    infoMessage={t('aiAssistantInfo')}
                    suggestions={[
                        t('aiSuggestionCopy'),
                        t('aiSuggestionEdit'),
                        t('aiSuggestionCreate'),
                    ]}
                    position="bottom-right"
                    bottomOffset={96}
                    clearOnUnmount
                    projectShortCode={selectedProject?.shortCode}
                />
            </Box>
        </DashboardLayout>
    );
}
