'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    TextField,
    CircularProgress,
    Alert,
    Select,
    MenuItem,
    Radio,
    RadioGroup,
    FormControlLabel,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SaveIcon from '@mui/icons-material/Save';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { apiInformationService } from '@/services/apiInformationService';
import { ApiInformationDto } from '@/types/api';
import { useRouter, useParams } from 'next/navigation';
import { useProject } from '@/contexts/ProjectContext';
import { getErrorMessage } from '@/lib/errorUtils';

export default function APIDetailPage() {
    const router = useRouter();
    const params = useParams();
    const t = useTranslations('apiInfo');
    const apiId = parseInt(params.id as string);
    const { projects } = useProject();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

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
        sqlQuery:false,
    });

    useEffect(() => {
        fetchApiDetail();
    }, [apiId]);

    const fetchApiDetail = async () => {
        try {
            setLoading(true);
            setError(null);
            const apiData = await apiInformationService.getById(apiId);
            setFormData({
                ...apiData,
                mediaType: apiData.mediaType || 'application/json',
                active: apiData.active || 'Aktif',
            });
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to fetch API details'));
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        try {
            setError(null);
            await apiInformationService.update(formData);
            setSuccess('API updated successfully');
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to save API'));
        }
    };

    const handleInputChange = <K extends keyof ApiInformationDto>(field: K, value: ApiInformationDto[K]) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    if (loading) {
        return (
            <DashboardLayout>
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
                    <CircularProgress size={60} />
                </Box>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ mb: 3 }}>
                    <Button
                        startIcon={<ArrowBackIcon />}
                        onClick={() => router.push('/dashboard/api-information')}
                        sx={{ mb: 2 }}
                    >
                        {t('backToList')}
                    </Button>
                </Box>

                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                        {t('apiDetail')}
                    </Typography>
                    <Button
                        variant="contained"
                        startIcon={<SaveIcon />}
                        onClick={handleSave}
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

                <Paper sx={{ p: 3 }}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        {/* İstek Alanı - Tam Genişlik */}
                        <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'start' }}>
                            <Typography variant="body2" sx={{ mt: 1 }}>{t('name')} *</Typography>
                            <TextField
                                fullWidth
                                required
                                multiline
                                minRows={4}
                                maxRows={20}
                                value={formData.plIn || ''}
                                onChange={(e) => handleInputChange('plIn', e.target.value)}
                                size="small"
                            />
                        </Box>

                        {/* 2 Kolonlu Alan */}
                        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 3 }}>
                            {/* Sol Kolon */}
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                                <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                    <Typography variant="body2">{t('name')} *</Typography>
                                    <TextField
                                        fullWidth
                                        required
                                        value={formData.name || ''}
                                        onChange={(e) => handleInputChange('name', e.target.value)}
                                        size="small"
                                    />
                                </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'start' }}>
                                <Typography variant="body2" sx={{ mt: 1 }}>{t('apiUrl')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    multiline
                                    rows={2}
                                    value={formData.srvcName || ''}
                                    onChange={(e) => handleInputChange('srvcName', e.target.value)}
                                    size="small"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('apiShortCode')} *</Typography>
                                <TextField
                                    fullWidth
                                    required
                                    value={formData.apiShortCode || formData.shortCode}
                                    onChange={(e) => {
                                        handleInputChange('apiShortCode', e.target.value);
                                        handleInputChange('shortCode', e.target.value);
                                    }}
                                    size="small"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('headerParam')}</Typography>
                                <TextField
                                    fullWidth
                                    value={formData.headerParameters || ''}
                                    onChange={(e) => handleInputChange('headerParameters', e.target.value)}
                                    size="small"
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('responseCode')}</Typography>
                                <TextField
                                    fullWidth
                                    type="number"
                                    value={formData.statusCode || 200}
                                    onChange={(e) => handleInputChange('statusCode', parseInt(e.target.value))}
                                    size="small"
                                />
                            </Box>
                        </Box>

                        {/* Sağ Kolon */}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('httpMethod')} *</Typography>
                                <Select
                                    fullWidth
                                    value={formData.httpMethod || 'POST'}
                                    onChange={(e) => handleInputChange('httpMethod', e.target.value)}
                                    size="small"
                                >
                                    <MenuItem value="GET">GET</MenuItem>
                                    <MenuItem value="POST">POST</MenuItem>
                                    <MenuItem value="PUT">PUT</MenuItem>
                                    <MenuItem value="DELETE">DELETE</MenuItem>
                                    <MenuItem value="PATCH">PATCH</MenuItem>
                                </Select>
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('activeStatus')} *</Typography>
                                <RadioGroup
                                    row
                                    value={formData.active || 'Aktif'}
                                    onChange={(e) => handleInputChange('active', e.target.value)}
                                >
                                    <FormControlLabel value="Aktif" control={<Radio />} label={t('active')} />
                                    <FormControlLabel value="Pasif" control={<Radio />} label={t('inactive')} />
                                </RadioGroup>
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('mediaType')} *</Typography>
                                <Select
                                    fullWidth
                                    value={formData.mediaType || 'application/json'}
                                    onChange={(e) => handleInputChange('mediaType', e.target.value)}
                                    size="small"
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
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('externalApi')}</Typography>
                                <input
                                    type="checkbox"
                                    checked={formData.externalApi || false}
                                    onChange={(e) => handleInputChange('externalApi', e.target.checked)}
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('sqlQuery')}</Typography>
                                <input
                                    type="checkbox"
                                    checked={formData.sqlQuery || false}
                                    onChange={(e) => handleInputChange('sqlQuery', e.target.checked)}
                                />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 2, alignItems: 'center' }}>
                                <Typography variant="body2">{t('project')}</Typography>
                                <Select
                                    fullWidth
                                    value={formData.projectId || ''}
                                    onChange={(e) => handleInputChange('projectId', e.target.value as number)}
                                    size="small"
                                >
                                    {projects.map((project) => (
                                        <MenuItem key={project.projectId || 0} value={project.projectId || 0}>
                                            {project.shortCode} - {project.name}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </Box>
                        </Box>
                    </Box>
                    </Box>
                </Paper>
            </Box>
        </DashboardLayout>
    );
}
