'use client';

import React from 'react';
import { useTranslations } from 'next-intl';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Autocomplete,
    Box,
    Button,
    Chip,
    Divider,
    Paper,
    Stack,
    Switch,
    TextField,
    Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CodeEditor from '@/components/CodeEditor';
import { ApiInformationDto } from '@/types/api';
import { HarApiInformationMatch, HarReviewedDraftStep } from '@/types/harAnalysis';

interface HarLogicalStepReviewCardProps {
    step: HarReviewedDraftStep;
    allSteps: HarReviewedDraftStep[];
    apiOptions: ApiInformationDto[];
    dependencyIssue?: string | null;
    duplicateShortCode?: boolean;
    onStepChange: (stepOrder: number, updates: Partial<HarReviewedDraftStep>) => void;
    onOpenCreateApi: (step: HarReviewedDraftStep) => void;
}

const getApiId = (api?: Partial<ApiInformationDto> | null): number | null => {
    if (!api) {
        return null;
    }

    if (typeof api.gnlApiInformationId === 'number' && Number.isFinite(api.gnlApiInformationId)) {
        return api.gnlApiInformationId;
    }

    if (typeof api.id === 'number' && Number.isFinite(api.id)) {
        return api.id;
    }

    return null;
};

const getOptionLabel = (api?: Partial<ApiInformationDto> | null, fallbackLabel = 'Unknown API'): string => {
    if (!api) {
        return fallbackLabel;
    }

    return api.name || api.apiShortCode || api.shortCode || api.srvcName || `API #${getApiId(api) || 'N/A'}`;
};

const getMatchScore = (match?: HarApiInformationMatch | null): number | null => {
    if (!match) {
        return null;
    }

    const candidates = [match.score, match.matchScore, match.confidence];

    for (const candidate of candidates) {
        if (typeof candidate === 'number' && Number.isFinite(candidate)) {
            return candidate;
        }
    }

    return null;
};

const renderReasonList = (reasons?: string[]) => {
    if (!reasons || reasons.length === 0) {
        return null;
    }

    return (
        <Box component="ul" sx={{ mt: 1, mb: 0, pl: 2.5 }}>
            {reasons.map((reason, index) => (
                <li key={`${reason}-${index}`}>
                    <Typography variant="body2" color="text.secondary">
                        {reason}
                    </Typography>
                </li>
            ))}
        </Box>
    );
};

export default function HarLogicalStepReviewCard({
    step,
    allSteps,
    apiOptions,
    dependencyIssue,
    duplicateShortCode,
    onStepChange,
    onOpenCreateApi,
}: HarLogicalStepReviewCardProps) {
    const t = useTranslations('harImport.reviewCard');
    const localizedOptionLabel = (api?: Partial<ApiInformationDto> | null) => getOptionLabel(api, t('unknownApi'));
    const stepsByOrder = React.useMemo(
        () => new Map(allSteps.map((reviewStep) => [reviewStep.analysisStepOrder, reviewStep])),
        [allSteps]
    );
    const selectedApi = apiOptions.find(
        (api) => (api.gnlApiInformationId || api.id) === step.selectedApiInformationId
    ) || null;

    const uniqueCandidateMatches = step.candidateApiInformation.filter((candidate, index, source) => {
        const candidateId = getApiId(candidate);

        if (candidateId === null) {
            return index === source.findIndex((entry) => localizedOptionLabel(entry) === localizedOptionLabel(candidate));
        }

        return index === source.findIndex((entry) => getApiId(entry) === candidateId);
    });

    const unresolved = step.included && !step.selectedApiInformationId;
    const matchedScore = getMatchScore(step.matchedApiInformation);

    const resolveSelectedApiForStep = (targetStep?: HarReviewedDraftStep | null): ApiInformationDto | null => {
        if (!targetStep || targetStep.selectedApiInformationId === null) {
            return null;
        }

        return apiOptions.find(
            (api) => (api.gnlApiInformationId || api.id) === targetStep.selectedApiInformationId
        ) || null;
    };

    const resolveStepDisplayName = (targetStep?: HarReviewedDraftStep | null): string => {
        if (!targetStep) {
            return t('unknownApi');
        }

        return getOptionLabel(
            resolveSelectedApiForStep(targetStep) || targetStep.matchedApiInformation || targetStep.apiInformationDraft || null,
            targetStep.stepShortCode || `STEP_${targetStep.analysisStepOrder}`
        );
    };

    const resolveStepDisplayUrl = (targetStep?: HarReviewedDraftStep | null): string => {
        if (!targetStep) {
            return t('noPathAvailable');
        }

        return (
            resolveSelectedApiForStep(targetStep)?.srvcName ||
            resolveSelectedApiForStep(targetStep)?.url ||
            targetStep.matchedApiInformation?.srvcName ||
            targetStep.matchedApiInformation?.url ||
            targetStep.apiInformationDraft?.srvcName ||
            targetStep.apiInformationDraft?.url ||
            targetStep.path ||
            targetStep.originalAnalysisReference.normalizedPath ||
            targetStep.originalAnalysisReference.url ||
            t('noPathAvailable')
        );
    };

    return (
        <Accordion
            disableGutters
            sx={{
                border: '1px solid',
                borderColor: unresolved ? 'warning.main' : 'divider',
                borderRadius: '16px !important',
                overflow: 'hidden',
                boxShadow: 1,
                '&:before': {
                    display: 'none',
                },
            }}
        >
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1.5, width: '100%', pr: 2 }}>
                    <Chip label={`#${step.analysisStepOrder}`} color="primary" size="small" />
                    <Chip
                        label={step.stepType || t('unknownStepType')}
                        color={step.stepType === 'PRIMARY' ? 'secondary' : 'default'}
                        size="small"
                        variant="outlined"
                    />
                    {step.method && <Chip label={step.method} size="small" color="info" variant="outlined" />}
                    <Chip
                        icon={step.selectedApiInformationId ? <CheckCircleIcon /> : <WarningAmberIcon />}
                        label={step.selectedApiInformationId ? t('matched') : t('unmatched')}
                        color={step.selectedApiInformationId ? 'success' : 'warning'}
                        size="small"
                    />
                    <Chip
                        label={step.included ? t('included') : t('excluded')}
                        color={step.included ? 'success' : 'default'}
                        size="small"
                        variant={step.included ? 'filled' : 'outlined'}
                    />
                    {typeof step.statusCode === 'number' && (
                        <Chip label={t('statusChip', { status: step.statusCode })} size="small" variant="outlined" />
                    )}
                    {typeof step.responseTime === 'number' && (
                        <Chip label={t('responseTimeChip', { time: step.responseTime })} size="small" variant="outlined" />
                    )}
                    <Chip
                        label={t('dependenciesChip', { count: step.dependsOnStepOrders.length })}
                        size="small"
                        variant="outlined"
                    />
                    <Box sx={{ flex: 1, minWidth: 240 }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                            {step.stepShortCode || `STEP_${step.analysisStepOrder}`}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                            {step.path || step.originalAnalysisReference.normalizedPath || step.originalAnalysisReference.url || t('noPathAvailable')}
                        </Typography>
                    </Box>
                </Box>
            </AccordionSummary>
            <AccordionDetails>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, alignSelf: 'flex-start' }}>
                        <Switch
                            checked={step.included}
                            onClick={(event) => event.stopPropagation()}
                            onChange={(event) => onStepChange(step.analysisStepOrder, { included: event.target.checked })}
                        />
                        <Typography variant="body2">{t('includeToggle')}</Typography>
                    </Box>

                    {unresolved && (
                        <Alert severity="warning">
                            {t('alerts.unresolvedIncludedStep')}
                        </Alert>
                    )}

                    {!!dependencyIssue && (
                        <Alert severity="error">{dependencyIssue}</Alert>
                    )}

                    {duplicateShortCode && step.included && (
                        <Alert severity="error">
                            {t('alerts.duplicateStepShortCode')}
                        </Alert>
                    )}

                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }, gap: 2 }}>
                        <TextField
                            label={t('fields.stepShortCode')}
                            value={step.stepShortCode}
                            onChange={(event) => onStepChange(step.analysisStepOrder, { stepShortCode: event.target.value })}
                            fullWidth
                        />
                        <TextField
                            label={t('fields.preHeader')}
                            value={step.preHeader}
                            onChange={(event) => onStepChange(step.analysisStepOrder, { preHeader: event.target.value })}
                            fullWidth
                            multiline
                            minRows={2}
                        />
                        <TextField
                            label={t('fields.headerExtractor')}
                            value={step.headerExtractor}
                            onChange={(event) => onStepChange(step.analysisStepOrder, { headerExtractor: event.target.value })}
                            fullWidth
                            multiline
                            minRows={2}
                        />
                        <TextField
                            label={t('fields.parameterExtractor')}
                            value={step.parameterExtractor}
                            onChange={(event) => onStepChange(step.analysisStepOrder, { parameterExtractor: event.target.value })}
                            fullWidth
                            multiline
                            minRows={2}
                        />
                    </Box>

                    <Box>
                        <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                            {t('fields.payloadTemplate')}
                        </Typography>
                        <CodeEditor
                            value={step.plIn}
                            onChange={(value) => onStepChange(step.analysisStepOrder, { plIn: value })}
                            language="json"
                            height="180px"
                        />
                    </Box>

                    <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                        <Stack spacing={1.5}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                                {t('sections.apiMapping')}
                            </Typography>

                            {step.matchedApiInformation && (
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'success.light', borderColor: 'success.main' }}>
                                    <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 1.5 }}>
                                        <Box>
                                            <Typography variant="body1" sx={{ fontWeight: 700 }}>
                                                {t('sections.bestMatch')}: {localizedOptionLabel(step.matchedApiInformation)}
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary">
                                                {(step.matchedApiInformation.srvcName || step.matchedApiInformation.url || '').toString()}
                                            </Typography>
                                        </Box>
                                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
                                            {matchedScore !== null && (
                                                <Chip label={t('scoreChip', { score: matchedScore })} color="success" size="small" />
                                            )}
                                            {step.matchedApiShortCode && (
                                                <Chip label={t('draftMatchChip', { shortCode: step.matchedApiShortCode })} size="small" variant="outlined" />
                                            )}
                                            <Button
                                                variant="outlined"
                                                size="small"
                                                disabled={getApiId(step.matchedApiInformation) === null}
                                                onClick={() =>
                                                    onStepChange(step.analysisStepOrder, {
                                                        selectedApiInformationId: getApiId(step.matchedApiInformation),
                                                    })
                                                }
                                                sx={{ textTransform: 'none' }}
                                            >
                                                {t('actions.useBestMatch')}
                                            </Button>
                                        </Box>
                                    </Box>
                                    {renderReasonList(step.matchedApiInformation.reasons)}
                                </Paper>
                            )}

                            {uniqueCandidateMatches.length > 0 && (
                                <Box>
                                    <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                                        {t('sections.candidateMatches')}
                                    </Typography>
                                    <Stack spacing={1.25}>
                                        {uniqueCandidateMatches.map((candidate, index) => {
                                            const candidateId = getApiId(candidate);
                                            const candidateScore = getMatchScore(candidate);
                                            return (
                                                <Paper key={`${candidateId || localizedOptionLabel(candidate)}-${index}`} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                                                    <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 1.5 }}>
                                                        <Box>
                                                            <Typography variant="body2" sx={{ fontWeight: 700 }}>
                                                                {localizedOptionLabel(candidate)}
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary">
                                                                {candidate.srvcName || candidate.url || t('noServicePathAvailable')}
                                                            </Typography>
                                                        </Box>
                                                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
                                                            {candidateScore !== null && (
                                                                <Chip label={t('scoreChip', { score: candidateScore })} size="small" color="info" />
                                                            )}
                                                            <Button
                                                                variant="text"
                                                                size="small"
                                                                disabled={candidateId === null}
                                                                onClick={() =>
                                                                    onStepChange(step.analysisStepOrder, {
                                                                        selectedApiInformationId: candidateId,
                                                                    })
                                                                }
                                                                sx={{ textTransform: 'none' }}
                                                            >
                                                                {t('actions.useCandidate')}
                                                            </Button>
                                                        </Box>
                                                    </Box>
                                                    {renderReasonList(candidate.reasons)}
                                                </Paper>
                                            );
                                        })}
                                    </Stack>
                                </Box>
                            )}

                            <Autocomplete
                                fullWidth
                                options={apiOptions}
                                value={selectedApi}
                                onChange={(_, newValue) => {
                                    onStepChange(step.analysisStepOrder, {
                                        selectedApiInformationId: newValue ? (newValue.gnlApiInformationId || newValue.id || null) : null,
                                    });
                                }}
                                getOptionLabel={localizedOptionLabel}
                                filterOptions={(options, state) => {
                                    if (!state.inputValue) {
                                        return options;
                                    }

                                    const searchValue = state.inputValue.toLowerCase();
                                    return options.filter((option) => {
                                        return [option.name, option.apiShortCode, option.shortCode, option.srvcName]
                                            .filter((value): value is string => typeof value === 'string' && value.trim().length > 0)
                                            .some((value) => value.toLowerCase().includes(searchValue));
                                    });
                                }}
                                isOptionEqualToValue={(option, value) => {
                                    return (option.gnlApiInformationId || option.id) === (value.gnlApiInformationId || value.id);
                                }}
                                renderInput={(params) => (
                                    <TextField
                                        {...params}
                                        label={t('fields.selectExistingApi')}
                                        placeholder={t('fields.searchApiPlaceholder')}
                                    />
                                )}
                                noOptionsText={t('fields.noApiOptions')}
                            />

                            {step.apiInformationDraft && !step.selectedApiInformationId && (
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'warning.light' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {t('sections.suggestedApiDraft')}
                                    </Typography>
                                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 1.5 }}>
                                        <TextField label={t('fields.name')} value={step.apiInformationDraft.name || ''} size="small" disabled fullWidth />
                                        <TextField label={t('fields.shortCode')} value={step.apiInformationDraft.shortCode || step.apiInformationDraft.apiShortCode || ''} size="small" disabled fullWidth />
                                        <TextField label={t('fields.httpMethod')} value={step.apiInformationDraft.httpMethod || step.apiInformationDraft.method || step.method || ''} size="small" disabled fullWidth />
                                        <TextField label={t('fields.path')} value={step.apiInformationDraft.srvcName || step.apiInformationDraft.url || step.path || ''} size="small" disabled fullWidth />
                                    </Box>
                                    <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                        <Button variant="contained" onClick={() => onOpenCreateApi(step)} sx={{ textTransform: 'none' }}>
                                            {t('actions.createApiFromDraft')}
                                        </Button>
                                        <Button
                                            variant="outlined"
                                            color="inherit"
                                            onClick={() => onStepChange(step.analysisStepOrder, { included: false })}
                                            sx={{ textTransform: 'none' }}
                                        >
                                            {t('actions.excludeStepInstead')}
                                        </Button>
                                    </Box>
                                </Paper>
                            )}
                        </Stack>
                    </Paper>

                    <Divider />

                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }, gap: 2 }}>
                        <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                                {t('sections.requestSourceDetails')}
                            </Typography>
                            <Box sx={{ display: 'grid', gap: 1, mb: 1.5 }}>
                                <Box>
                                    <Typography variant="caption" color="text.secondary">
                                        {t('fields.name')}
                                    </Typography>
                                    <Typography variant="body2" sx={{ fontWeight: 700, wordBreak: 'break-word' }}>
                                        {resolveStepDisplayName(step)}
                                    </Typography>
                                </Box>
                                <Box>
                                    <Typography variant="caption" color="text.secondary">
                                        {t('fields.url')}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                                        {resolveStepDisplayUrl(step)}
                                    </Typography>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 1.5 }}>
                                {step.sourceRequestOrders.length > 0 ? (
                                    step.sourceRequestOrders.map((requestOrder) => (
                                        <Chip key={requestOrder} label={t('requestChip', { order: requestOrder })} size="small" variant="outlined" />
                                    ))
                                ) : (
                                    <Typography variant="body2" color="text.secondary">
                                        {t('noRequestOrderDetails')}
                                    </Typography>
                                )}
                            </Box>
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                {step.dependsOnStepOrders.length > 0 ? (
                                    step.dependsOnStepOrders.map((dependency) => (
                                        <Chip key={dependency} label={t('dependsOnChip', { dependency })} size="small" color="secondary" variant="outlined" />
                                    ))
                                ) : (
                                    <Typography variant="body2" color="text.secondary">
                                        {t('noExplicitDependencies')}
                                    </Typography>
                                )}
                            </Box>
                            {step.dependsOnStepOrders.length > 0 && (
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25, mt: 1.5 }}>
                                    {step.dependsOnStepOrders.map((dependency) => {
                                        const dependencyStep = stepsByOrder.get(dependency) || null;

                                        return (
                                            <Paper key={dependency} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                                                <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1, mb: 0.75 }}>
                                                    <Chip label={t('dependsOnChip', { dependency })} size="small" color="secondary" variant="outlined" />
                                                    {dependencyStep?.method && <Chip label={dependencyStep.method} size="small" variant="outlined" />}
                                                </Box>
                                                <Typography variant="body2" sx={{ fontWeight: 700, wordBreak: 'break-word' }}>
                                                    {resolveStepDisplayName(dependencyStep)}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                                                    {resolveStepDisplayUrl(dependencyStep)}
                                                </Typography>
                                            </Paper>
                                        );
                                    })}
                                </Box>
                            )}
                        </Paper>

                        <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                                {t('sections.rawStepDetails')}
                            </Typography>
                            <CodeEditor
                                value={JSON.stringify(step.originalAnalysisReference, null, 2)}
                                onChange={() => undefined}
                                language="json"
                                height="220px"
                                readOnly
                            />
                        </Paper>
                    </Box>
                </Box>
            </AccordionDetails>
        </Accordion>
    );
}
