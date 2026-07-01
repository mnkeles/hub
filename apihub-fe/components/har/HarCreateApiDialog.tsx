'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { useTranslations } from 'next-intl';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    TextField,
    Typography,
} from '@mui/material';
import CodeEditor from '@/components/CodeEditor';
import { apiInformationService } from '@/services/apiInformationService';
import { ApiInformationDto } from '@/types/api';
import { HarApiInformationDraft } from '@/types/harAnalysis';

interface HarCreateApiDialogProps {
    open: boolean;
    draft: HarApiInformationDraft | null;
    projectId: number | null;
    projectShortCode: string;
    defaultMethod?: string;
    defaultPath?: string;
    stepShortCode: string;
    onClose: () => void;
    onCreated: (api: ApiInformationDto) => void;
}

const createBaseFormData = (projectId: number | null): ApiInformationDto => ({
    name: '',
    shortCode: '',
    apiShortCode: '',
    srvcName: '',
    httpMethod: 'GET',
    active: 'Aktif',
    mediaType: 'application/json',
    projectId: projectId || 1,
    statusCode: 200,
    plIn: '',
    headerParameters: '',
    grpc: false,
    isTokenApi: false,
    externalApi: false,
    sqlQuery: false,
});

const extractNumericId = (value: unknown): number | null => {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
    }

    if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) {
        return Number(value);
    }

    if (!value || typeof value !== 'object') {
        return null;
    }

    const candidates = [
        (value as Record<string, unknown>).gnlApiInformationId,
        (value as Record<string, unknown>).id,
        (value as Record<string, unknown>).apiId,
    ];

    for (const candidate of candidates) {
        const parsed = extractNumericId(candidate);
        if (parsed !== null) {
            return parsed;
        }
    }

    return null;
};

const getErrorMessage = (error: unknown, fallbackMessage: string): string => {
    const maybeError = error as {
        response?: {
            data?: {
                message?: string;
                error?: string;
            } | string;
            statusText?: string;
        };
        message?: string;
    };

    const responseData = maybeError?.response?.data;

    if (typeof responseData === 'string' && responseData.trim()) {
        return responseData;
    }

    if (responseData && typeof responseData === 'object') {
        if (typeof responseData.message === 'string' && responseData.message.trim()) {
            return responseData.message;
        }

        if (typeof responseData.error === 'string' && responseData.error.trim()) {
            return responseData.error;
        }
    }

    if (typeof maybeError?.response?.statusText === 'string' && maybeError.response.statusText.trim()) {
        return maybeError.response.statusText;
    }

    if (typeof maybeError?.message === 'string' && maybeError.message.trim()) {
        return maybeError.message;
    }

    return fallbackMessage;
};

export default function HarCreateApiDialog({
    open,
    draft,
    projectId,
    projectShortCode,
    defaultMethod,
    defaultPath,
    stepShortCode,
    onClose,
    onCreated,
}: HarCreateApiDialogProps) {
    const t = useTranslations('harImport.createApiDialog');
    const commonT = useTranslations('common');
    const [formData, setFormData] = useState<ApiInformationDto>(createBaseFormData(projectId));
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    const derivedFormData = useMemo<ApiInformationDto>(() => {
        const shortCode = draft?.apiShortCode || draft?.shortCode || stepShortCode || '';
        const serviceName = draft?.srvcName || draft?.url || defaultPath || '';
        const httpMethod = draft?.httpMethod || draft?.method || defaultMethod || 'GET';

        return {
            ...createBaseFormData(projectId),
            name: draft?.name || shortCode || serviceName || t('defaultApiName'),
            shortCode,
            apiShortCode: draft?.apiShortCode || shortCode,
            srvcName: serviceName,
            httpMethod,
            active: 'Aktif',
            mediaType: draft?.mediaType || 'application/json',
            projectId: projectId || 1,
            statusCode: draft?.statusCode || 200,
            plIn: draft?.plIn || '',
            headerParameters: draft?.headerParameters || '',
            description: draft?.description || '',
            headerVal: draft?.headerVal || '',
            grpc: false,
            isTokenApi: false,
            externalApi: false,
            sqlQuery: false,
        };
    }, [defaultMethod, defaultPath, draft, projectId, stepShortCode, t]);

    useEffect(() => {
        if (open) {
            setFormData(derivedFormData);
            setError(null);
        }
    }, [derivedFormData, open]);

    const handleInputChange = (field: keyof ApiInformationDto, value: string | number) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
    };

    const handleSave = async () => {
        if (!projectId) {
            setError(t('errors.projectResolve'));
            return;
        }

        if (!formData.name?.trim()) {
            setError(t('errors.nameRequired'));
            return;
        }

        if (!formData.shortCode?.trim()) {
            setError(t('errors.shortCodeRequired'));
            return;
        }

        if (!formData.srvcName?.trim()) {
            setError(t('errors.servicePathRequired'));
            return;
        }

        try {
            setSaving(true);
            setError(null);

            const payload: ApiInformationDto = {
                ...formData,
                apiShortCode: formData.apiShortCode?.trim() || formData.shortCode.trim(),
                shortCode: formData.shortCode.trim(),
                name: formData.name.trim(),
                srvcName: formData.srvcName.trim(),
                projectId,
                httpMethod: formData.httpMethod || 'GET',
                mediaType: formData.mediaType || 'application/json',
                statusCode: formData.statusCode || 200,
                active: 'Aktif',
                grpc: false,
                isTokenApi: false,
                externalApi: false,
                sqlQuery: false,
            };

            const result = await apiInformationService.save(payload);

            if (result && result.success === false) {
                throw new Error(result.message || t('errors.failedToCreate'));
            }

            let createdApi: ApiInformationDto | null = null;
            const createdId = extractNumericId(result?.data);

            if (createdId !== null) {
                try {
                    createdApi = await apiInformationService.getById(createdId);
                } catch {
                    createdApi = null;
                }
            }

            if (!createdApi && projectShortCode.trim()) {
                const projectApis = await apiInformationService.getByProject(projectShortCode.trim());
                const matchingApis = projectApis
                    .filter((api) => api.projectId === projectId)
                    .filter((api) => {
                        const apiId = api.gnlApiInformationId || api.id || 0;
                        if (createdId !== null && apiId === createdId) {
                            return true;
                        }

                        const sameShortCode =
                            (api.apiShortCode || '').toLowerCase() === (payload.apiShortCode || '').toLowerCase() ||
                            (api.shortCode || '').toLowerCase() === payload.shortCode.toLowerCase();
                        const samePath = (api.srvcName || '').toLowerCase() === payload.srvcName.toLowerCase();
                        return sameShortCode && samePath;
                    })
                    .sort((a, b) => {
                        const left = b.gnlApiInformationId || b.id || 0;
                        const right = a.gnlApiInformationId || a.id || 0;
                        return left - right;
                    });

                createdApi = matchingApis[0] || null;
            }

            if (!createdApi) {
                const allApis = await apiInformationService.getAll();
                const matchingApis = allApis
                    .filter((api) => api.projectId === projectId)
                    .filter((api) => {
                        const apiId = api.gnlApiInformationId || api.id || 0;
                        if (createdId !== null && apiId === createdId) {
                            return true;
                        }

                        const sameShortCode =
                            (api.apiShortCode || '').toLowerCase() === (payload.apiShortCode || '').toLowerCase() ||
                            (api.shortCode || '').toLowerCase() === payload.shortCode.toLowerCase();
                        const samePath = (api.srvcName || '').toLowerCase() === payload.srvcName.toLowerCase();
                        return sameShortCode && samePath;
                    })
                    .sort((a, b) => {
                        const left = b.gnlApiInformationId || b.id || 0;
                        const right = a.gnlApiInformationId || a.id || 0;
                        return left - right;
                    });

                createdApi = matchingApis[0] || null;
            }

            if (!createdApi) {
                throw new Error(t('errors.createdButReloadFailed'));
            }

            onCreated(createdApi);
            onClose();
        } catch (saveError) {
            setError(getErrorMessage(saveError, t('errors.failedToCreate')));
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={saving ? undefined : onClose} maxWidth="md" fullWidth>
            <DialogTitle sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
                {t('title')}
            </DialogTitle>
            <DialogContent sx={{ pt: 3 }}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 1 }}>
                    {error && <Alert severity="error">{error}</Alert>}

                    <Alert severity="info">
                        {t('infoAlert')}
                    </Alert>

                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 2 }}>
                        <TextField
                            label={t('fields.name')}
                            value={formData.name || ''}
                            onChange={(event) => handleInputChange('name', event.target.value)}
                            fullWidth
                            required
                        />
                        <TextField
                            label={t('fields.shortCode')}
                            value={formData.shortCode || ''}
                            onChange={(event) => handleInputChange('shortCode', event.target.value)}
                            fullWidth
                            required
                        />
                        <TextField
                            label={t('fields.apiShortCode')}
                            value={formData.apiShortCode || ''}
                            onChange={(event) => handleInputChange('apiShortCode', event.target.value)}
                            fullWidth
                        />
                        <TextField
                            label={t('fields.httpMethod')}
                            value={formData.httpMethod || 'GET'}
                            onChange={(event) => handleInputChange('httpMethod', event.target.value)}
                            select
                            fullWidth
                        >
                            {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD'].map((method) => (
                                <MenuItem key={method} value={method}>
                                    {method}
                                </MenuItem>
                            ))}
                        </TextField>
                        <TextField
                            label={t('fields.servicePath')}
                            value={formData.srvcName || ''}
                            onChange={(event) => handleInputChange('srvcName', event.target.value)}
                            fullWidth
                            required
                        />
                        <TextField
                            label={t('fields.statusCode')}
                            type="number"
                            value={formData.statusCode || 200}
                            onChange={(event) => handleInputChange('statusCode', Number(event.target.value) || 200)}
                            fullWidth
                        />
                        <TextField
                            label={t('fields.mediaType')}
                            value={formData.mediaType || 'application/json'}
                            onChange={(event) => handleInputChange('mediaType', event.target.value)}
                            fullWidth
                        />
                        <TextField
                            label={t('fields.headerParameters')}
                            value={formData.headerParameters || ''}
                            onChange={(event) => handleInputChange('headerParameters', event.target.value)}
                            fullWidth
                        />
                    </Box>

                    <Box>
                        <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                            {t('fields.payloadTemplate')}
                        </Typography>
                        <CodeEditor
                            value={formData.plIn || ''}
                            onChange={(value) => handleInputChange('plIn', value)}
                            language="json"
                            height="220px"
                        />
                    </Box>
                </Box>
            </DialogContent>
            <DialogActions sx={{ px: 3, py: 2 }}>
                <Button onClick={onClose} variant="outlined" disabled={saving} sx={{ textTransform: 'none' }}>
                    {commonT('cancel')}
                </Button>
                <Button onClick={handleSave} variant="contained" disabled={saving} sx={{ textTransform: 'none', minWidth: 180 }}>
                    {saving ? (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <CircularProgress size={18} color="inherit" />
                            <span>{t('actions.creating')}</span>
                        </Box>
                    ) : (
                        t('actions.create')
                    )}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
