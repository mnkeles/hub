'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    IconButton,
    Chip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    CircularProgress,
    Alert,
    Breadcrumbs,
    Link,
    Tabs,
    Tab,
    Select,
    MenuItem,
    Autocomplete,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SearchIcon from '@mui/icons-material/Search';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import {
    DndContext,
    closestCenter,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    DragEndEvent,
} from '@dnd-kit/core';
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    useSortable,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import CodeEditor from '@/components/CodeEditor';
import { processFlowStepService } from '@/services/processFlowStepService';
import { processFlowService } from '@/services/processFlowService';
import { apiInformationService } from '@/services/apiInformationService';
import { ProcessFlowStepDto, ProcessFlowDto, ApiInformationDto, ProcessFlowStepParmDto, ProcessFlowStepRelationDto } from '@/types/api';
import { useRouter, useParams } from 'next/navigation';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import { useProject } from '@/contexts/ProjectContext';
import { getErrorMessage } from '@/lib/errorUtils';

const getResolvedApiId = (api?: ApiInformationDto | ProcessFlowStepDto | null) => {
    if (!api) {
        return 0;
    }

    if (typeof api.gnlApiInformationId === 'number' && Number.isFinite(api.gnlApiInformationId)) {
        return api.gnlApiInformationId;
    }

    if ('id' in api && typeof api.id === 'number' && Number.isFinite(api.id)) {
        return api.id;
    }

    return 0;
};

export default function ProcessFlowStepsPage() {
    const router = useRouter();
    const params = useParams();
    const t = useTranslations('flows');
    const flowId = parseInt(params.flowId as string);
    const { selectedProject } = useProject();

    const [steps, setSteps] = useState<ProcessFlowStepDto[]>([]);
    const [filteredSteps, setFilteredSteps] = useState<ProcessFlowStepDto[]>([]);
    const [flow, setFlow] = useState<ProcessFlowDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingStep, setEditingStep] = useState<ProcessFlowStepDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
    const [stepToDelete, setStepToDelete] = useState<number | null>(null);
    const [activeTab, setActiveTab] = useState(0);
    const [apiList, setApiList] = useState<ApiInformationDto[]>([]);
    const [filteredApiList, setFilteredApiList] = useState<ApiInformationDto[]>([]);
    const [expandedParams, setExpandedParams] = useState<{ [key: string]: boolean }>({});
    const [paramDialogOpen, setParamDialogOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [newParam, setNewParam] = useState<ProcessFlowStepParmDto>({
        processFlowStepParmId: null,
        shortCode: '',
        value: '',
        valExpression: '',
        sql: '',
        code: '',
        useContext: true,
        paramOrder: 1,
    });

    const [formData, setFormData] = useState<ProcessFlowStepDto>({
        processFlowStepId: null,
        gnlApiInformationId: 0,
        processFlowId: flowId,
        stepOrder: 1,
        stepShortCode: '',
        plIn: '',
        headerExtractor: '',
        parameterExtractor: '',
        preHeader: '',
    });

    useEffect(() => {
        fetchFlow();
        fetchSteps();
        fetchApiList();
    }, [flowId]);

    // Redirect to process flows page when project changes
    useEffect(() => {
        if (selectedProject && flow && flow.projectId !== selectedProject.projectId) {
            router.push('/dashboard/process-flows');
        }
    }, [selectedProject, flow, router]);

    // Filter steps by search query
    useEffect(() => {
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            const filtered = steps.filter(step => 
                step.stepShortCode?.toLowerCase().includes(query) ||
                getResolvedApiId(step).toString().includes(query)
            );
            setFilteredSteps(filtered);
        } else {
            setFilteredSteps(steps);
        }
    }, [steps, searchQuery]);

    const fetchFlow = async () => {
        try {
            const flowData = await processFlowService.getById(flowId);
            setFlow(flowData);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to fetch process flow'));
        }
    };

    const fetchApiList = async () => {
        try {
            const apis = await apiInformationService.getAll();
            setApiList(apis);
        } catch (err) {
            console.error('Failed to fetch API list:', err);
        }
    };

    // Filter API list by selected project
    useEffect(() => {
        if (selectedProject && apiList.length > 0) {
            const filtered = apiList.filter(api => api.projectId === selectedProject.projectId);
            setFilteredApiList(filtered);
        } else {
            setFilteredApiList(apiList);
        }
    }, [selectedProject, apiList]);

    const fetchSteps = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await processFlowStepService.list({
                offset: 0,
                limit: 100,
                filterList: [
                    {
                        criteria: 'PROCESS_FLOW_ID',
                        numberValue: flowId,
                    },
                ],
            });
            const sortedSteps = response.data.sort((a, b) => a.stepOrder - b.stepOrder);
            setSteps(sortedSteps);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to fetch steps'));
        } finally {
            setLoading(false);
        }
    };

    const handleOpenDialog = (step?: ProcessFlowStepDto) => {
        if (step && step.processFlowStepId) {
            setEditingStep(step);
            setFormData({
                ...step,
                gnlApiInformationId: getResolvedApiId(step) || getResolvedApiId(step.apiInformation),
                processFlowStepParmList: step.processFlowStepParmList || []
            });
        } else {
            setEditingStep(null);
            const maxOrder = steps.length > 0 ? Math.max(...steps.map(s => s.stepOrder)) : 0;
            setFormData({
                processFlowStepId: null,
                gnlApiInformationId: 0,
                processFlowId: flowId,
                stepOrder: maxOrder + 1,
                stepShortCode: '',
                plIn: '',
                headerExtractor: '',
                parameterExtractor: '',
                preHeader: '',
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingStep(null);
        setError(null);
    };

    const handleSave = async () => {
        // Prevent multiple saves
        if (saving) return;
        
        try {
            setError(null);
            
            // Validation
            if (!formData.gnlApiInformationId || formData.gnlApiInformationId === 0) {
                setError(t('apiInfoRequired') || 'API Bilgisi seçilmelidir');
                return;
            }
            
            if (!formData.stepShortCode || formData.stepShortCode.trim() === '') {
                setError(t('stepShortCodeRequired') || 'Adım Kısa Kodu zorunludur');
                return;
            }
            
            setSaving(true);
            
            if (editingStep) {
                await processFlowStepService.update(formData);
                setSuccess('Step updated successfully');
            } else {
                await processFlowStepService.save(formData);
                setSuccess('Step created successfully');
            }
            
            handleCloseDialog();
            await fetchSteps();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            console.error('Save error:', err);
            setError(getErrorMessage(err, 'Failed to save step'));
        } finally {
            setSaving(false);
        }
    };

    const handleOpenParamDialog = () => {
        setNewParam({
            processFlowStepParmId: null,
            shortCode: '',
            value: '',
            valExpression: '',
            sql: '',
            code: '',
            useContext: true,
            paramOrder: 1,
        });
        setParamDialogOpen(true);
    };

    const handleCloseParamDialog = () => {
        setParamDialogOpen(false);
    };

    const handleParamInputChange = <K extends keyof ProcessFlowStepParmDto>(field: K, value: ProcessFlowStepParmDto[K]) => {
        setNewParam(prev => ({ ...prev, [field]: value }));
    };

    const handleSaveParam = () => {
        if (!newParam.shortCode) {
            setError('Kısa Kod zorunludur');
            return;
        }

        const updatedParams = [...(formData.processFlowStepParmList || []), newParam];
        setFormData(prev => ({ ...prev, processFlowStepParmList: updatedParams }));
        handleCloseParamDialog();
        setSuccess('Parametre eklendi. Değişiklikleri kaydetmeyi unutmayın!');
        setTimeout(() => setSuccess(null), 3000);
    };

    const handleUpdateParam = <K extends keyof ProcessFlowStepParmDto>(index: number, field: K, value: ProcessFlowStepParmDto[K]) => {
        const updatedParams = [...(formData.processFlowStepParmList || [])];
        updatedParams[index] = { ...updatedParams[index], [field]: value };
        setFormData(prev => ({ ...prev, processFlowStepParmList: updatedParams }));
    };

    const handleDeleteClick = (stepId: number) => {
        setStepToDelete(stepId);
        setDeleteConfirmOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (stepToDelete) {
            try {
                await processFlowStepService.delete(stepToDelete);
                setSuccess('Step deleted successfully');
                setDeleteConfirmOpen(false);
                setStepToDelete(null);
                fetchSteps();
                setTimeout(() => setSuccess(null), 3000);
            } catch (err) {
                setError(getErrorMessage(err, 'Failed to delete step'));
            }
        }
    };

    // Drag and drop sensors
    const sensors = useSensors(
        useSensor(PointerSensor),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    );

    // Handle drag end
    const handleDragEnd = async (event: DragEndEvent) => {
        const { active, over } = event;

        if (over && active.id !== over.id) {
            const oldIndex = filteredSteps.findIndex((step) => step.processFlowStepId === active.id);
            const newIndex = filteredSteps.findIndex((step) => step.processFlowStepId === over.id);

            const newSteps = arrayMove(filteredSteps, oldIndex, newIndex);
            
            // Update stepOrder for each step
            const updatedSteps = newSteps.map((step, index) => ({
                ...step,
                stepOrder: index + 1,
            }));

            // Update local state immediately for better UX
            setFilteredSteps(updatedSteps);
            setSteps(updatedSteps);

            try {
                // Send only processFlowStepId and stepOrder to backend
                const orderUpdates = updatedSteps.map((step) => ({
                    processFlowStepId: step.processFlowStepId!,
                    stepOrder: step.stepOrder,
                }));
                
                await processFlowStepService.updateStepOrders(orderUpdates);
                setSuccess('Sıralama güncellendi');
                setTimeout(() => setSuccess(null), 3000);
            } catch (err) {
                setError(getErrorMessage(err, 'Sıralama güncellenemedi'));
                // Revert on error
                fetchSteps();
            }
        }
    };

    const handleInputChange = <K extends keyof ProcessFlowStepDto>(field: K, value: ProcessFlowStepDto[K]) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    // Sortable Row Component
    function SortableRow({ step }: { step: ProcessFlowStepDto }) {
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ id: step.processFlowStepId! });

        const style = {
            transform: CSS.Transform.toString(transform),
            transition,
            opacity: isDragging ? 0.5 : 1,
            backgroundColor: isDragging ? 'rgba(99, 102, 241, 0.05)' : 'transparent',
        };

        return (
            <TableRow ref={setNodeRef} style={style} key={step.processFlowStepId}>
                <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <IconButton
                            size="small"
                            {...attributes}
                            {...listeners}
                            sx={{
                                cursor: 'grab',
                                '&:active': { cursor: 'grabbing' },
                                color: 'text.secondary',
                            }}
                        >
                            <DragIndicatorIcon />
                        </IconButton>
                        <Chip label={step.stepOrder} color="primary" size="small" />
                    </Box>
                </TableCell>
                <TableCell>
                    <Typography variant="body2" fontWeight={600}>
                        {step.stepShortCode}
                    </Typography>
                </TableCell>
                <TableCell>{getResolvedApiId(step) || getResolvedApiId(step.apiInformation) || '-'}</TableCell>
                <TableCell>
                    {step.headerExtractor && (
                        <Chip label={step.headerExtractor} size="small" />
                    )}
                </TableCell>
                <TableCell>
                    {step.parameterExtractor && (
                        <Typography variant="caption" sx={{ maxWidth: 200, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {step.parameterExtractor}
                        </Typography>
                    )}
                </TableCell>
                <TableCell align="right">
                    <IconButton
                        size="small"
                        onClick={() => handleOpenDialog(step)}
                    >
                        <EditIcon />
                    </IconButton>
                    <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDeleteClick(step.processFlowStepId!)}
                    >
                        <DeleteIcon />
                    </IconButton>
                </TableCell>
            </TableRow>
        );
    }

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ mb: 2 }}>
                    <Button
                        startIcon={<ArrowBackIcon />}
                        onClick={() => router.push('/dashboard/process-flows')}
                        sx={{ 
                            mb: 2,
                            textTransform: 'none',
                            color: 'primary.main',
                            '&:hover': {
                                backgroundColor: 'rgba(99, 102, 241, 0.08)'
                            }
                        }}
                    >
                        Akışlara Dön
                    </Button>
                    <Breadcrumbs sx={{ mb: 3 }}>
                        <Link
                            underline="hover"
                            color="inherit"
                            onClick={() => router.push('/dashboard/process-flows')}
                            sx={{ cursor: 'pointer', fontSize: '0.875rem' }}
                        >
                            Akışlar
                        </Link>
                        <Typography color="text.primary" sx={{ fontSize: '0.875rem', fontWeight: 500 }}>
                            {flow?.shortCode}
                        </Typography>
                    </Breadcrumbs>
                </Box>

                <Box sx={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center', 
                    mb: 4,
                    pb: 2,
                    borderBottom: '2px solid',
                    borderColor: 'divider'
                }}>
                    <Box>
                        <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
                            {t('steps')}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {flow?.shortCode}
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
                        {t('addStep')}
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

                <Paper>
                    <TableContainer>
                        <DndContext
                            sensors={sensors}
                            collisionDetection={closestCenter}
                            onDragEnd={handleDragEnd}
                        >
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>{t('order')}</TableCell>
                                        <TableCell>{t('stepShortCode')}</TableCell>
                                        <TableCell>{t('apiInfoId')}</TableCell>
                                        <TableCell>{t('headerExtractor')}</TableCell>
                                        <TableCell>{t('paramExtractor')}</TableCell>
                                        <TableCell align="right">{t('actions')}</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {loading ? (
                                        <TableRow>
                                            <TableCell colSpan={6} align="center">
                                                <CircularProgress />
                                            </TableCell>
                                        </TableRow>
                                    ) : filteredSteps.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={6} align="center">
                                                {searchQuery ? t('noResults') : t('noFlows')}
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        <SortableContext
                                            items={filteredSteps.map((step) => step.processFlowStepId!)}
                                            strategy={verticalListSortingStrategy}
                                        >
                                            {filteredSteps.map((step) => (
                                                <SortableRow key={step.processFlowStepId} step={step} />
                                            ))}
                                        </SortableContext>
                                    )}
                                </TableBody>
                            </Table>
                        </DndContext>
                    </TableContainer>
                </Paper>

                {/* Add/Edit Dialog */}
                <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="lg" fullWidth>
                    <DialogTitle sx={{ borderBottom: 1, borderColor: 'divider', pb: 0, pt: 2, fontWeight: 600, mb: 1.5, fontSize: '1.1rem' }}>
                        {editingStep ? t('editStep') : t('addStep')}
                        {error && (
                            <Alert severity="error" sx={{ mt: 2, mb: 1 }} onClose={() => setError(null)}>
                                {error}
                            </Alert>
                        )}
                        <Tabs 
                            value={activeTab} 
                            onChange={(e, newValue) => setActiveTab(newValue)}
                            sx={{
                                '& .MuiTab-root': {
                                    textTransform: 'none',
                                    fontSize: '0.9rem',
                                    fontWeight: 500,
                                    minHeight: 48
                                }
                            }}
                        >
                            <Tab label={t('stepDetail')} />
                            <Tab label={t('apiInfo')} />
                            <Tab label={t('parameters')} />
                        </Tabs>
                    </DialogTitle>
                    <DialogContent sx={{ mt: 2, pt: 2 }}>

                        {/* Tab 1: Adım Detayı */}
                        {activeTab === 0 && (
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('stepShortCode')} *</Typography>
                                    <TextField
                                        fullWidth
                                        required
                                        value={formData.stepShortCode}
                                        onChange={(e) => handleInputChange('stepShortCode', e.target.value)}
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
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary', mt: 1 }}>İstek</Typography>
                                    {formData.plIn && formData.plIn.trim() ? (
                                        <CodeEditor
                                            value={formData.plIn}
                                            onChange={(val) => handleInputChange('plIn', val)}
                                            language="json"
                                            height="200px"
                                        />
                                    ) : (
                                        <TextField
                                            fullWidth
                                            multiline
                                            minRows={4}
                                            maxRows={20}
                                            value={formData.plIn || ''}
                                            onChange={(e) => handleInputChange('plIn', e.target.value)}
                                            size="small"
                                            placeholder="JSON formatında istek girin..."
                                            sx={{
                                                '& .MuiOutlinedInput-root': {
                                                    backgroundColor: 'background.paper',
                                                    fontFamily: 'monospace',
                                                    fontSize: '0.875rem',
                                                }
                                            }}
                                        />
                                    )}
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary', mt: 1 }}>{t('headerExtractor')}</Typography>
                                    <TextField
                                        fullWidth
                                        multiline
                                        minRows={2}
                                        maxRows={15}
                                        value={formData.headerExtractor || ''}
                                        onChange={(e) => handleInputChange('headerExtractor', e.target.value)}
                                        size="small"
                                        placeholder="Örn: Authorization=Bearer ${token}"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'rgba(99, 102, 241, 0.03)',
                                                fontFamily: 'monospace',
                                                fontSize: '0.875rem',
                                                '&:hover': {
                                                    backgroundColor: 'rgba(99, 102, 241, 0.05)',
                                                },
                                                '&:hover fieldset': {
                                                    borderColor: 'primary.main',
                                                    borderWidth: '2px'
                                                },
                                                '&.Mui-focused': {
                                                    backgroundColor: 'background.paper',
                                                },
                                                '&.Mui-focused fieldset': {
                                                    borderWidth: '2px',
                                                    borderColor: 'primary.main'
                                                }
                                            }
                                        }}
                                    />
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary', mt: 1 }}>{t('paramExtractor')}</Typography>
                                    <TextField
                                        fullWidth
                                        multiline
                                        minRows={2}
                                        maxRows={15}
                                        value={formData.parameterExtractor || ''}
                                        onChange={(e) => handleInputChange('parameterExtractor', e.target.value)}
                                        size="small"
                                        placeholder="Örn: OrderId&/[local-name()='Envelope']/[local-name()='Body']"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'rgba(16, 185, 129, 0.03)',
                                                fontFamily: 'monospace',
                                                fontSize: '0.875rem',
                                                '&:hover': {
                                                    backgroundColor: 'rgba(16, 185, 129, 0.05)',
                                                },
                                                '&:hover fieldset': {
                                                    borderColor: 'success.main',
                                                    borderWidth: '2px'
                                                },
                                                '&.Mui-focused': {
                                                    backgroundColor: 'background.paper',
                                                },
                                                '&.Mui-focused fieldset': {
                                                    borderWidth: '2px',
                                                    borderColor: 'success.main'
                                                }
                                            }
                                        }}
                                    />
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary', mt: 1 }}>{t('preHeader')}</Typography>
                                    <TextField
                                        fullWidth
                                        multiline
                                        minRows={2}
                                        maxRows={10}
                                        value={formData.preHeader || ''}
                                        onChange={(e) => handleInputChange('preHeader', e.target.value)}
                                        size="small"
                                        placeholder="Örn: Content-Type=application/json"
                                        sx={{
                                            '& .MuiOutlinedInput-root': {
                                                backgroundColor: 'rgba(245, 158, 11, 0.03)',
                                                fontFamily: 'monospace',
                                                fontSize: '0.875rem',
                                                '&:hover': {
                                                    backgroundColor: 'rgba(245, 158, 11, 0.05)',
                                                },
                                                '&:hover fieldset': {
                                                    borderColor: 'warning.main',
                                                    borderWidth: '2px'
                                                },
                                                '&.Mui-focused': {
                                                    backgroundColor: 'background.paper',
                                                },
                                                '&.Mui-focused fieldset': {
                                                    borderWidth: '2px',
                                                    borderColor: 'warning.main'
                                                }
                                            }
                                        }}
                                    />
                                </Box>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('stepOrder')} *</Typography>
                                    <TextField
                                        type="number"
                                        fullWidth
                                        required
                                        value={formData.stepOrder}
                                        onChange={(e) => handleInputChange('stepOrder', parseInt(e.target.value))}
                                        size="small"
                                        sx={{ 
                                            maxWidth: 200,
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
                        )}

                        {/* Tab 2: API Bilgileri */}
                        {activeTab === 1 && (
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('apiInfoLabel')} *</Typography>
                                    <Autocomplete
                                        fullWidth
                                        options={filteredApiList}
                                        value={filteredApiList.find(a => getResolvedApiId(a) === formData.gnlApiInformationId) || null}
                                        onChange={(event, newValue) => {
                                            const apiId = newValue ? getResolvedApiId(newValue) : 0;
                                            
                                            // Update both gnlApiInformationId and apiInformation object
                                            setFormData(prev => ({
                                                ...prev,
                                                gnlApiInformationId: apiId,
                                                apiInformation: newValue || undefined
                                            }));
                                        }}
                                        getOptionLabel={(option) => option.name || option.apiShortCode || option.shortCode || option.srvcName || `API #${option.id}`}
                                        filterOptions={(options, { inputValue }) => {
                                            if (!inputValue) return options;
                                            const searchTerm = inputValue.toLowerCase();
                                            return options.filter(option => {
                                                const name = (option.name || '').toLowerCase();
                                                const apiShortCode = (option.apiShortCode || '').toLowerCase();
                                                const shortCode = (option.shortCode || '').toLowerCase();
                                                const srvcName = (option.srvcName || '').toLowerCase();
                                                return name.includes(searchTerm) || 
                                                       apiShortCode.includes(searchTerm) || 
                                                       shortCode.includes(searchTerm) || 
                                                       srvcName.includes(searchTerm);
                                            });
                                        }}
                                        renderInput={(params) => (
                                            <TextField
                                                {...params}
                                                placeholder={t('searchApi')}
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
                                        )}
                                        noOptionsText={t('noApiFound')}
                                        isOptionEqualToValue={(option, value) => getResolvedApiId(option) === getResolvedApiId(value)}
                                    />
                                </Box>
                                {formData.gnlApiInformationId > 0 && (() => {
                                    const selectedApi = filteredApiList.find(a => getResolvedApiId(a) === formData.gnlApiInformationId);
                                    return selectedApi && (
                                        <>
                                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('name')}</Typography>
                                                <TextField
                                                    fullWidth
                                                    value={selectedApi.name || selectedApi.srvcName || 'Belirtilmemiş'}
                                                    size="small"
                                                    disabled
                                                    sx={{
                                                        '& .MuiOutlinedInput-root': {
                                                            backgroundColor: 'action.hover'
                                                        }
                                                    }}
                                                />
                                            </Box>
                                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary', mt: 1 }}>{t('request')}</Typography>
                                                <TextField
                                                    fullWidth
                                                    multiline
                                                    minRows={2}
                                                    maxRows={10}
                                                    value={selectedApi.plIn || ''}
                                                    size="small"
                                                    disabled
                                                    sx={{
                                                        '& .MuiOutlinedInput-root': {
                                                            backgroundColor: 'action.hover',
                                                            fontFamily: 'monospace',
                                                            fontSize: '0.875rem'
                                                        }
                                                    }}
                                                />
                                            </Box>
                                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('apiUrl')}</Typography>
                                                <TextField
                                                    fullWidth
                                                    value={selectedApi.srvcName || selectedApi.url || ''}
                                                    size="small"
                                                    disabled
                                                    sx={{
                                                        '& .MuiOutlinedInput-root': {
                                                            backgroundColor: 'action.hover',
                                                            fontFamily: 'monospace',
                                                            fontSize: '0.875rem'
                                                        }
                                                    }}
                                                />
                                            </Box>
                                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('shortCode')}</Typography>
                                                <TextField
                                                    fullWidth
                                                    value={selectedApi.shortCode || selectedApi.srvcName || 'Belirtilmemiş'}
                                                    size="small"
                                                    disabled
                                                    sx={{
                                                        '& .MuiOutlinedInput-root': {
                                                            backgroundColor: 'action.hover'
                                                        }
                                                    }}
                                                />
                                            </Box>
                                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>{t('httpMethod')}</Typography>
                                                <TextField
                                                    fullWidth
                                                    value={selectedApi.httpMethod || selectedApi.method || ''}
                                                    size="small"
                                                    disabled
                                                    sx={{
                                                        '& .MuiOutlinedInput-root': {
                                                            backgroundColor: 'action.hover'
                                                        }
                                                    }}
                                                />
                                            </Box>
                                            <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>Durumu</Typography>
                                                <TextField
                                                    fullWidth
                                                    value={selectedApi.statusCode || 200}
                                                    size="small"
                                                    disabled
                                                    sx={{
                                                        '& .MuiOutlinedInput-root': {
                                                            backgroundColor: 'action.hover'
                                                        }
                                                    }}
                                                />
                                            </Box>
                                        </>
                                    );
                                })()}
                            </Box>
                        )}

                        {/* Tab 3: Adım Parametreleri */}
                        {activeTab === 2 && (
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                {formData.processFlowStepParmList && formData.processFlowStepParmList.length > 0 ? (
                                    formData.processFlowStepParmList.map((param, index) => (
                                        <Box key={index} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                                            {/* Parametre Başlığı */}
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    p: 2,
                                                    backgroundColor: 'action.hover',
                                                    cursor: 'pointer',
                                                }}
                                                onClick={() => setExpandedParams(prev => ({
                                                    ...prev,
                                                    [param.shortCode]: !prev[param.shortCode]
                                                }))}
                                            >
                                                <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                    {expandedParams[param.shortCode] ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                                                    {param.shortCode}
                                                </Typography>
                                            </Box>

                                            {/* Parametre Detayları */}
                                            {expandedParams[param.shortCode] && (
                                                <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                        <Typography variant="body2">Kısa Kod *</Typography>
                                                        <TextField
                                                            fullWidth
                                                            value={param.shortCode}
                                                            onChange={(e) => handleUpdateParam(index, 'shortCode', e.target.value)}
                                                            size="small"
                                                        />
                                                    </Box>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                        <Typography variant="body2">Değer</Typography>
                                                        <TextField
                                                            fullWidth
                                                            value={param.value || ''}
                                                            onChange={(e) => handleUpdateParam(index, 'value', e.target.value)}
                                                            size="small"
                                                        />
                                                    </Box>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                        <Typography variant="body2">Değer Sorgusu</Typography>
                                                        <TextField
                                                            fullWidth
                                                            value={param.valExpression || ''}
                                                            onChange={(e) => handleUpdateParam(index, 'valExpression', e.target.value)}
                                                            size="small"
                                                        />
                                                    </Box>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                                        <Typography variant="body2" sx={{ mt: 1 }}>SQL Sorgusu</Typography>
                                                        {param.sql && param.sql.trim() ? (
                                                            <CodeEditor
                                                                value={param.sql}
                                                                onChange={(val) => handleUpdateParam(index, 'sql', val)}
                                                                language="sql"
                                                                height="150px"
                                                            />
                                                        ) : (
                                                            <TextField
                                                                fullWidth
                                                                multiline
                                                                minRows={3}
                                                                maxRows={10}
                                                                value={param.sql || ''}
                                                                onChange={(e) => handleUpdateParam(index, 'sql', e.target.value)}
                                                                size="small"
                                                                placeholder="SQL sorgusu girin..."
                                                            />
                                                        )}
                                                    </Box>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'start' }}>
                                                        <Typography variant="body2" sx={{ mt: 1 }}>Dinamik Sorgu</Typography>
                                                        {param.code && param.code.trim() ? (
                                                            <CodeEditor
                                                                value={param.code}
                                                                onChange={(val) => handleUpdateParam(index, 'code', val)}
                                                                language="javascript"
                                                                height="150px"
                                                            />
                                                        ) : (
                                                            <TextField
                                                                fullWidth
                                                                multiline
                                                                minRows={3}
                                                                maxRows={10}
                                                                value={param.code || ''}
                                                                onChange={(e) => handleUpdateParam(index, 'code', e.target.value)}
                                                                size="small"
                                                                placeholder="Dinamik sorgu girin..."
                                                            />
                                                        )}
                                                    </Box>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                        <Typography variant="body2">Kontekst kullan *</Typography>
                                                        <Box sx={{ display: 'flex', gap: 2 }}>
                                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                                <input 
                                                                    type="radio" 
                                                                    checked={param.useContext} 
                                                                    onChange={() => handleUpdateParam(index, 'useContext', true)}
                                                                />
                                                                <Typography variant="body2">Aktif</Typography>
                                                            </Box>
                                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                                <input 
                                                                    type="radio" 
                                                                    checked={!param.useContext} 
                                                                    onChange={() => handleUpdateParam(index, 'useContext', false)}
                                                                />
                                                                <Typography variant="body2">Pasif</Typography>
                                                            </Box>
                                                        </Box>
                                                    </Box>
                                                    <Box sx={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: 2, alignItems: 'center' }}>
                                                        <Typography variant="body2">Parametre Sırası</Typography>
                                                        <TextField
                                                            fullWidth
                                                            type="number"
                                                            value={param.paramOrder}
                                                            onChange={(e) => handleUpdateParam(index, 'paramOrder', parseInt(e.target.value))}
                                                            size="small"
                                                            sx={{ maxWidth: 200 }}
                                                        />
                                                    </Box>
                                                    <Box sx={{ mt: 2, p: 1.5, backgroundColor: 'info.lighter', borderRadius: 1, border: '1px solid', borderColor: 'info.light' }}>
                                                        <Typography variant="caption" color="info.dark" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                                            ℹ️ {t('parameterAutoSaveNote')}
                                                        </Typography>
                                                    </Box>
                                                </Box>
                                            )}
                                        </Box>
                                    ))
                                ) : null}
                                
                                {/* Yeni Parametre Ekle Butonu - Her zaman göster */}
                                {formData.processFlowStepParmList && formData.processFlowStepParmList.length > 0 && (
                                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                                        <Button
                                            variant="outlined"
                                            startIcon={<AddIcon />}
                                            onClick={handleOpenParamDialog}
                                            sx={{ textTransform: 'none' }}
                                        >
                                            Yeni Parametre Ekle
                                        </Button>
                                    </Box>
                                )}
                                
                                {!formData.processFlowStepParmList || formData.processFlowStepParmList.length === 0 ? (
                                    <Box sx={{ p: 3, textAlign: 'center' }}>
                                        <Typography variant="body2" color="text.secondary">
                                            Henüz parametre eklenmemiş
                                        </Typography>
                                        <Button
                                            variant="text"
                                            startIcon={<AddIcon />}
                                            sx={{ mt: 2 }}
                                            onClick={handleOpenParamDialog}
                                        >
                                            Yeni Parametre Ekle
                                        </Button>
                                    </Box>
                                ) : null}
                            </Box>
                        )}
                    </DialogContent>
                    <DialogActions sx={{ px: 2, py: 1.5 }}>
                        <Button onClick={handleCloseDialog} variant="outlined" sx={{ textTransform: 'none', px: 2, py: 0.75 }} disabled={saving}>
                            {t('cancel')}
                        </Button>
                        <Button onClick={handleSave} variant="contained" sx={{ textTransform: 'none', px: 2, py: 0.75 }} disabled={saving}>
                            {saving ? 'Kaydediliyor...' : t('save')}
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Yeni Parametre Ekle Dialog */}
                <Dialog open={paramDialogOpen} onClose={handleCloseParamDialog} maxWidth="md" fullWidth>
                    <DialogTitle sx={{ borderBottom: 1, borderColor: 'divider', pb: 2, pt: 2, fontWeight: 600, fontSize: '1.1rem' }}>
                        {t('newParameter')}
                    </DialogTitle>
                    <DialogContent sx={{ mt: 2, pt: 2 }}>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 700 }}>{t('shortCodeRequired')}</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    value={newParam.shortCode}
                                    onChange={(e) => handleParamInputChange('shortCode', e.target.value)}
                                    size="small"
                                    placeholder="${username}"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>{t('value')}</Typography>
                                <TextField
                                    fullWidth
                                    value={newParam.value || ''}
                                    onChange={(e) => handleParamInputChange('value', e.target.value)}
                                    size="small"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>{t('valueQuery')}</Typography>
                                <TextField
                                    fullWidth
                                    value={newParam.valExpression || ''}
                                    onChange={(e) => handleParamInputChange('valExpression', e.target.value)}
                                    size="small"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'start' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, mt: 1 }}>{t('sqlQuery')}</Typography>
                                {newParam.sql && newParam.sql.trim() ? (
                                    <CodeEditor
                                        value={newParam.sql}
                                        onChange={(val) => handleParamInputChange('sql', val)}
                                        language="sql"
                                        height="150px"
                                    />
                                ) : (
                                    <TextField
                                        fullWidth
                                        multiline
                                        minRows={3}
                                        maxRows={10}
                                        value={newParam.sql || ''}
                                        onChange={(e) => handleParamInputChange('sql', e.target.value)}
                                        size="small"
                                        placeholder="SQL sorgusu girin..."
                                    />
                                )}
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'start' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600, mt: 1 }}>{t('dynamicQuery')}</Typography>
                                {newParam.code && newParam.code.trim() ? (
                                    <CodeEditor
                                        value={newParam.code}
                                        onChange={(val) => handleParamInputChange('code', val)}
                                        language="javascript"
                                        height="150px"
                                    />
                                ) : (
                                    <TextField
                                        fullWidth
                                        multiline
                                        minRows={3}
                                        maxRows={10}
                                        value={newParam.code || ''}
                                        onChange={(e) => handleParamInputChange('code', e.target.value)}
                                        size="small"
                                        placeholder="Dinamik sorgu girin..."
                                    />
                                )}
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 700 }}>{t('useContext')} *</Typography>
                                <Box sx={{ display: 'flex', gap: 2 }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                        <input 
                                            type="radio" 
                                            checked={newParam.useContext === true} 
                                            onChange={() => handleParamInputChange('useContext', true)}
                                        />
                                        <Typography variant="body2">{t('active')}</Typography>
                                    </Box>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                        <input 
                                            type="radio" 
                                            checked={newParam.useContext === false} 
                                            onChange={() => handleParamInputChange('useContext', false)}
                                        />
                                        <Typography variant="body2">{t('passive')}</Typography>
                                    </Box>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>{t('parameterOrder')}</Typography>
                                <TextField
                                    fullWidth
                                    type="number"
                                    value={newParam.paramOrder}
                                    onChange={(e) => handleParamInputChange('paramOrder', parseInt(e.target.value))}
                                    size="small"
                                    sx={{ maxWidth: 250 }}
                                />
                            </Box>
                        </Box>
                    </DialogContent>
                    <DialogActions sx={{ px: 2, py: 1.5 }}>
                        <Button onClick={handleCloseParamDialog} variant="outlined" sx={{ textTransform: 'none', px: 2, py: 0.75 }}>
                            {t('cancel')}
                        </Button>
                        <Button onClick={handleSaveParam} variant="contained" sx={{ textTransform: 'none', px: 2, py: 0.75 }}>
                            {t('save')}
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Delete Confirmation Dialog */}
                <Dialog open={deleteConfirmOpen} onClose={() => setDeleteConfirmOpen(false)}>
                    <DialogTitle>Confirm Delete</DialogTitle>
                    <DialogContent>
                        Are you sure you want to delete this step? This action cannot be undone.
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setDeleteConfirmOpen(false)}>Cancel</Button>
                        <Button onClick={handleDeleteConfirm} color="error" variant="contained">
                            Delete
                        </Button>
                    </DialogActions>
                </Dialog>
            </Box>
        </DashboardLayout>
    );
}
