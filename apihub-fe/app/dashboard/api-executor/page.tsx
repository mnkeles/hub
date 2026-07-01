'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    TextField,
    Tabs,
    Tab,
    Alert,
    CircularProgress,
    Chip,
    IconButton,
    Divider,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Checkbox,
    FormControlLabel,
    Autocomplete,
    Accordion,
    AccordionSummary,
    AccordionDetails,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import CodeIcon from '@mui/icons-material/Code';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { apiCallService } from '@/services/apiCallService';
import { generalWebSystemService } from '@/services/generalWebSystemService';
import { processFlowService } from '@/services/processFlowService';
import { projectService } from '@/services/projectService';
import { ParameterRequestDto, GeneralWebSystemDto, ProcessFlowDto, ProjectDto, ProcessFlowStepParmDto } from '@/types/api';
import { getApiErrorStatus, getErrorMessage } from '@/lib/errorUtils';

interface KeyValuePair {
    id: string;
    key: string;
    value: string;
    enabled: boolean;
}

type RequestType = 'single' | 'flow' | 'flowV2' | 'flowV2ContinueOnError';

type ApiExecutorResponse = Record<string, unknown> & {
    _xml?: string;
    _type?: 'xml' | string;
    _hasXmlValues?: boolean;
    result?: unknown;
    Result?: unknown;
    parameterContext?: Record<string, unknown>;
    Parametreler?: unknown;
    parametreler?: unknown;
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null;

const isApiExecutorResponse = (value: unknown): value is ApiExecutorResponse =>
    isRecord(value);

const getRecordField = (source: unknown, field: string): Record<string, unknown> | undefined => {
    if (!isRecord(source)) {
        return undefined;
    }

    const value = source[field];
    return isRecord(value) ? value : undefined;
};

export default function ApiExecutorPage() {
    const t = useTranslations('apiExecutor');
    // Request Configuration
    const [requestType, setRequestType] = useState<RequestType>('flow');
    const [selectedProject, setSelectedProject] = useState<ProjectDto | null>(null);
    const [selectedSystem, setSelectedSystem] = useState<GeneralWebSystemDto | null>(null);
    const [selectedFlow, setSelectedFlow] = useState<ProcessFlowDto | null>(null);
    const [stepCode, setStepCode] = useState('');
    
    // Parameters and Headers
    const [parameters, setParameters] = useState<KeyValuePair[]>([
        { id: '1', key: '', value: '', enabled: true }
    ]);
    const [headers, setHeaders] = useState<KeyValuePair[]>([
        { id: '1', key: '', value: '', enabled: true }
    ]);
    const [combination, setCombination] = useState(false);
    
    // Response State
    const [loading, setLoading] = useState(false);
    const [response, setResponse] = useState<ApiExecutorResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [responseTab, setResponseTab] = useState(0);
    const [responseTime, setResponseTime] = useState<number>(0);
    const [statusCode, setStatusCode] = useState<number | null>(null);
    const [expandedAccordions, setExpandedAccordions] = useState<{[key: string]: boolean}>({});
    
    // Data from API
    const [projects, setProjects] = useState<ProjectDto[]>([]);
    const [systems, setSystems] = useState<GeneralWebSystemDto[]>([]);
    const [flows, setFlows] = useState<ProcessFlowDto[]>([]);
    const [flowParameters, setFlowParameters] = useState<ProcessFlowStepParmDto[]>([]);

    // Filter systems by selected project
    const filteredSystems = React.useMemo(() => {
        if (!selectedProject) return systems;
        return systems.filter(sys => sys.projectId === selectedProject.projectId);
    }, [systems, selectedProject]);

    useEffect(() => {
        loadData();
    }, []);

    // Clear flow and system selection when project changes
    useEffect(() => {
        if (selectedProject) {
            // Clear flow if it doesn't belong to selected project
            if (selectedFlow && selectedFlow.projectId !== selectedProject.projectId) {
                setSelectedFlow(null);
            }
            // Clear system if it doesn't belong to selected project
            if (selectedSystem && selectedSystem.projectId !== selectedProject.projectId) {
                setSelectedSystem(null);
            }
        }
    }, [selectedProject]);

    // Load flow parameters when flow is selected
    useEffect(() => {
        if (selectedFlow) {
            loadFlowParameters();
        } else {
            setFlowParameters([]);
        }
    }, [selectedFlow]);

    const loadFlowParameters = async () => {
        if (!selectedFlow?.processFlowId) return;
        
        try {
            // Get flow with relations (includes steps and parameters)
            const flowWithRelations = await processFlowService.getWithRelations(selectedFlow.processFlowId);
            
            console.log('Flow with relations:', flowWithRelations);
            console.log('Flow keys:', Object.keys(flowWithRelations));
            
            // Collect all unique parameters from all steps
            const allParams: ProcessFlowStepParmDto[] = [];
            
            // Get steps from the correct property
            const steps = flowWithRelations.processFlowStepList || [];
            
            console.log('Steps found:', steps);
            console.log('Steps type:', typeof steps, 'isArray:', Array.isArray(steps));
            
            if (Array.isArray(steps)) {
                steps.forEach((step) => {
                    console.log('Processing step:', step);
                    
                    // Get parameters from the correct property
                    const params = step.processFlowStepParmList || [];
                    
                    console.log('Step parameters:', params);
                    
                    if (Array.isArray(params)) {
                        params.forEach((param) => {
                            console.log('Processing param:', param);
                            // Only add if not already in list (by shortCode)
                            if (param.shortCode && !allParams.find(p => p.shortCode === param.shortCode)) {
                                allParams.push(param);
                            }
                        });
                    }
                });
            }
            
            console.log('Flow parameters loaded:', allParams);
            setFlowParameters(allParams);
        } catch (err) {
            console.error('Failed to load flow parameters:', err);
            setFlowParameters([]);
        }
    };

    const loadData = async () => {
        try {
            const [projectsData, systemsData, flowsData] = await Promise.all([
                projectService.getAll(),
                generalWebSystemService.getAll(),
                processFlowService.getAll(),
            ]);
            setProjects(projectsData);
            setSystems(systemsData);
            setFlows(flowsData);
        } catch (err) {
            console.error('Failed to load data:', err);
        }
    };

    // Parameter Handlers
    const addParameter = () => {
        setParameters([...parameters, { id: `${Date.now()}-${Math.random()}`, key: '', value: '', enabled: true }]);
    };

    const updateParameter = (id: string, field: 'key' | 'value' | 'enabled', value: string | boolean) => {
        setParameters(parameters.map(p => p.id === id ? { ...p, [field]: value } : p));
    };

    const removeParameter = (id: string) => {
        setParameters(parameters.filter(p => p.id !== id));
    };

    // Header Handlers
    const addHeader = () => {
        setHeaders([...headers, { id: `${Date.now()}-${Math.random()}`, key: '', value: '', enabled: true }]);
    };

    const updateHeader = (id: string, field: 'key' | 'value' | 'enabled', value: string | boolean) => {
        setHeaders(headers.map(h => h.id === id ? { ...h, [field]: value } : h));
    };

    const removeHeader = (id: string) => {
        setHeaders(headers.filter(h => h.id !== id));
    };

    // Execute Request
    const handleExecute = async () => {
        try {
            setLoading(true);
            setError(null);
            const startTime = Date.now();

            let result;
            
            if (requestType === 'single') {
                result = await apiCallService.callStep(
                    selectedProject?.shortCode || '',
                    selectedSystem?.shortCode || '',
                    stepCode
                );
            } else if (requestType === 'flow') {
                result = await apiCallService.callProcess(
                    selectedProject?.shortCode || '',
                    selectedSystem?.shortCode || '',
                    selectedFlow?.shortCode || ''
                );
            } else if (requestType === 'flowV2') {
                // FlowV2 with parameters and headers
                const enabledParams = parameters
                    .filter(p => p.enabled && p.key)
                    .reduce((acc, p) => ({ ...acc, [p.key]: p.value }), {});
                
                const enabledHeaders = headers
                    .filter(h => h.enabled && h.key)
                    .reduce((acc, h) => ({ ...acc, [h.key]: h.value }), {});

                const params: ParameterRequestDto = {
                    parameterContext: enabledParams,
                    globalHeaders: enabledHeaders,
                    combination: combination,
                };

                result = await apiCallService.callProcessV2(
                    selectedProject?.shortCode || '',
                    selectedSystem?.shortCode || '',
                    selectedFlow?.shortCode || '',
                    params
                );
            } else {
                // FlowV2 with continueOnError=true
                const enabledParams = parameters
                    .filter(p => p.enabled && p.key)
                    .reduce((acc, p) => ({ ...acc, [p.key]: p.value }), {});
                
                const enabledHeaders = headers
                    .filter(h => h.enabled && h.key)
                    .reduce((acc, h) => ({ ...acc, [h.key]: h.value }), {});

                const params: ParameterRequestDto = {
                    parameterContext: enabledParams,
                    globalHeaders: enabledHeaders,
                    combination: combination,
                };

                result = await apiCallService.callProcessV2WithContinueOnError(
                    selectedProject?.shortCode || '',
                    selectedSystem?.shortCode || '',
                    selectedFlow?.shortCode || '',
                    params
                );
            }

            const endTime = Date.now();
            setResponseTime(endTime - startTime);
            
            // Parse response if it's a string
            let parsedResult = result;
            if (typeof result === 'string') {
                try {
                    // Try JSON first
                    parsedResult = JSON.parse(result);
                } catch (e) {
                    // If JSON fails, check if it's XML
                    if (result.trim().startsWith('<')) {
                        parsedResult = { 
                            _xml: result,
                            _type: 'xml'
                        };
                    } else {
                        console.error('Failed to parse response:', e);
                    }
                }
            }
            
            // Check if result object contains XML strings and parse them
            if (isApiExecutorResponse(parsedResult) && isRecord(parsedResult.result)) {
                const resultObj = parsedResult.result;
                let hasXml = false;
                const extractedParams: Record<string, string> = {};
                
                // Check if any value looks like XML and extract parameters
                for (const key in resultObj) {
                    if (typeof resultObj[key] === 'string') {
                        const value = resultObj[key];
                        // If it contains XML declaration fragments or looks like XML
                        if (value.includes("encoding='UTF-8'") || value.includes('<?xml') || value.includes('rO0AB') || value.includes('<S:Envelope')) {
                            hasXml = true;
                            
                            // Try to extract values from XML
                            try {
                                // Extract TransactionId
                                const transactionIdMatch = value.match(/<ns0:TransactionId>([^<]+)<\/ns0:TransactionId>/);
                                if (transactionIdMatch) {
                                    extractedParams[`${key}_TransactionId`] = transactionIdMatch[1];
                                }
                                
                                // Extract ExternalTransactionId
                                const externalTransactionIdMatch = value.match(/<ns0:ExternalTransactionId>([^<]+)<\/ns0:ExternalTransactionId>/);
                                if (externalTransactionIdMatch) {
                                    extractedParams[`${key}_ExternalTransactionId`] = externalTransactionIdMatch[1];
                                }
                                
                                // Extract ReturnCode
                                const returnCodeMatch = value.match(/<ns0:ReturnCode>([^<]+)<\/ns0:ReturnCode>/);
                                if (returnCodeMatch) {
                                    extractedParams[`${key}_ReturnCode`] = returnCodeMatch[1];
                                }
                                
                                // Extract ReturnMessage (if error)
                                const returnMessageMatch = value.match(/<ns0:ReturnMessage>([^<]+)<\/ns0:ReturnMessage>/);
                                if (returnMessageMatch) {
                                    extractedParams[`${key}_ReturnMessage`] = returnMessageMatch[1];
                                }
                            } catch (e) {
                                console.error('Failed to parse XML values:', e);
                            }
                        }
                    }
                }
                
                if (hasXml) {
                    parsedResult._hasXmlValues = true;
                    // Merge extracted parameters into parameterContext
                    if (Object.keys(extractedParams).length > 0) {
                        parsedResult.parameterContext = {
                            ...(parsedResult.parameterContext || {}),
                            ...extractedParams
                        };
                    }
                }
            }
            
            setResponse(isApiExecutorResponse(parsedResult) ? parsedResult : { value: parsedResult });
            setStatusCode(200); // Assuming success
        } catch (err) {
            setError(getErrorMessage(err, 'Request failed'));
            setStatusCode(getApiErrorStatus(err) || 500);
        } finally {
            setLoading(false);
        }
    };

    const filterResponse = (data: unknown): unknown => {
        if (!isRecord(data)) return data;
        
        // Response'un bir kopyasını oluştur
        const filtered: Record<string, unknown> = { ...data };
        
        // Always remove parameterContext from display
        if (filtered.parameterContext) {
            delete filtered.parameterContext;
        }
        
        // Remove internal flags
        if (filtered._hasXmlValues !== undefined) {
            delete filtered._hasXmlValues;
        }
        if (filtered._type !== undefined) {
            delete filtered._type;
        }
        if (filtered._xml !== undefined) {
            delete filtered._xml;
        }
        
        // Parse JSON strings in the response recursively
        const parseJsonStrings = (obj: unknown): unknown => {
            if (typeof obj === 'string') {
                // Try to parse if it looks like JSON
                if ((obj.startsWith('{') && obj.endsWith('}')) || (obj.startsWith('[') && obj.endsWith(']'))) {
                    try {
                        return parseJsonStrings(JSON.parse(obj));
                    } catch {
                        return obj;
                    }
                }
                return obj;
            } else if (Array.isArray(obj)) {
                return obj.map(item => parseJsonStrings(item));
            } else if (isRecord(obj)) {
                const parsed: Record<string, unknown> = {};
                for (const [key, value] of Object.entries(obj)) {
                    parsed[key] = parseJsonStrings(value);
                }
                return parsed;
            }
            return obj;
        };
        
        return parseJsonStrings(filtered);
    };

    const copyResponse = () => {
        if (response) {
            const filtered = filterResponse(response);
            navigator.clipboard.writeText(JSON.stringify(filtered, null, 2));
        }
    };

    const formatResponse = (data: ApiExecutorResponse) => {
        try {
            // Check if this is XML response
            if (data && data._type === 'xml' && data._xml) {
                // Format XML with indentation
                const xmlString = data._xml;
                return `<pre style="color: #4caf50;">${xmlString.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>`;
            }
            
            // Check if response contains XML - show formatted XML with red service names
            if (data._hasXmlValues && isRecord(data.result)) {
                const resultObj = data.result;
                let output = '';
                
                // Format each XML service
                for (const serviceName in resultObj) {
                    if (typeof resultObj[serviceName] === 'string') {
                        const xmlContent = resultObj[serviceName];
                        
                        // Just escape HTML, no formatting
                        const escapedXml = xmlContent
                            .replace(/&/g, '&amp;')
                            .replace(/</g, '&lt;')
                            .replace(/>/g, '&gt;')
                            .replace(/"/g, '&quot;');
                        
                        output += `<div style="margin-bottom: 2px; text-align: left !important; display: block;">
                            <span style="color: #d32f2f; font-weight: 700; display: inline-block;">${serviceName}: </span><span style="color: #333;">${escapedXml}</span>
                        </div>`;
                    }
                }
                
                return `<div style="text-align: left; width: 100%;">${output}</div>`;
            }
            
            const filtered = filterResponse(data);
            const jsonString = JSON.stringify(filtered, null, 2);
            // Add syntax highlighting
            return jsonString
                .replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?)/g, (match) => {
                    let cls = 'json-value';
                    if (/:$/.test(match)) {
                        cls = 'json-key';
                        // Highlight service/flow keys (odf-, edf-, businessFlow-, inquire, update, etc.) differently
                        if (match.match(/"(odf-|edf-|api-|businessFlow-|inquire|update|submit|complete|validate)[^"]*":/i)) {
                            cls = 'json-api-key';
                        }
                    }
                    return `<span class="${cls}">${match}</span>`;
                })
                .replace(/\b(true|false|null)\b/g, '<span class="json-boolean">$1</span>')
                .replace(/\b(-?\d+(\.\d+)?([eE][+-]?\d+)?)\b/g, '<span class="json-number">$1</span>');
        } catch {
            return String(data);
        }
    };

    const canExecute = () => {
        if (!selectedProject || !selectedSystem) return false;
        if (requestType === 'single') return !!stepCode;
        return !!selectedFlow;
    };

    return (
        <DashboardLayout>
            <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
                {/* Header */}
                <Box sx={{ 
                    px: 3, 
                    py: 2, 
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    backgroundColor: 'background.paper',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2
                }}>
                    <Box sx={{
                        width: 48,
                        height: 48,
                        borderRadius: '12px',
                        backgroundColor: 'primary.main',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}>
                        <PlayArrowIcon sx={{ color: 'white', fontSize: 28 }} />
                    </Box>
                    <Box>
                        <Typography variant="h6" sx={{ fontWeight: 700, color: 'text.primary', lineHeight: 1.2 }}>
                            {t('title')}
                        </Typography>
                        <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: '0.875rem' }}>
                            {t('subtitle')}
                        </Typography>
                    </Box>
                </Box>

                {/* Main Content */}
                <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden', justifyContent: 'center' }}>
                    {/* Left Panel - Request Configuration */}
                    <Box sx={{ 
                        width: '480px',
                        minWidth: '480px',
                        borderRight: '1px solid',
                        borderColor: 'divider',
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden'
                    }}>
                        {/* Request Type Selector */}
                        <Box sx={{ p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
                            <FormControl fullWidth size="small">
                                <InputLabel>{t('requestType')}</InputLabel>
                                <Select
                                    value={requestType}
                                    label={t('requestType')}
                                    onChange={(e) => setRequestType(e.target.value as RequestType)}
                                >
                                    <MenuItem value="single">Tekli Adım</MenuItem>
                                    <MenuItem value="flow">Akış</MenuItem>
                                    <MenuItem value="flowV2">Akış V2 (Parametreli)</MenuItem>
                                    <MenuItem value="flowV2ContinueOnError">Api Kontrol</MenuItem>
                                </Select>
                            </FormControl>
                        </Box>

                        {/* Request Configuration */}
                        <Box sx={{ p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
                            <Box sx={{ display: 'flex', gap: 1.5, mb: 1.5 }}>
                                <Autocomplete
                                    fullWidth
                                    options={projects}
                                    getOptionLabel={(option) => option.shortCode}
                                    value={selectedProject}
                                    onChange={(_, newValue) => setSelectedProject(newValue)}
                                    renderInput={(params) => (
                                        <TextField 
                                            {...params} 
                                            label={t('project')} 
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
                                />
                                <Autocomplete
                                    fullWidth
                                    options={filteredSystems}
                                    getOptionLabel={(option) => option.shortCode}
                                    value={selectedSystem}
                                    onChange={(_, newValue) => setSelectedSystem(newValue)}
                                    renderInput={(params) => (
                                        <TextField 
                                            {...params} 
                                            label={t('environment')} 
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
                                />
                            </Box>

                            {requestType === 'single' ? (
                                <TextField
                                    fullWidth
                                    label={t('stepCode')}
                                    size="small"
                                    value={stepCode}
                                    onChange={(e) => setStepCode(e.target.value)}
                                    placeholder="LOGIN_STEP"
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
                            ) : (
                                <Autocomplete
                                    fullWidth
                                    options={flows.filter(flow => 
                                        !selectedProject || flow.projectId === selectedProject.projectId
                                    )}
                                    getOptionLabel={(option) => option.shortCode}
                                    value={selectedFlow}
                                    onChange={(_, newValue) => setSelectedFlow(newValue)}
                                    renderInput={(params) => (
                                        <TextField 
                                            {...params} 
                                            label={t('selectFlow')} 
                                            size="small"
                                            placeholder={selectedProject ? "Akış seçin..." : "Önce proje seçin"}
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
                                    disabled={!selectedProject}
                                    noOptionsText={selectedProject ? "Akış bulunamadı" : "Önce proje seçin"}
                                />
                            )}
                        </Box>

                        {/* Parameters and Headers (only for flowV2 and flowV2ContinueOnError) */}
                        {(requestType === 'flowV2' || requestType === 'flowV2ContinueOnError') && (
                            <Box sx={{ flex: 1, overflow: 'auto' }}>
                                <Tabs value={0} sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
                                    <Tab label={`Parametreler (${parameters.filter(p => p.enabled && p.key).length})`} />
                                    <Tab label={`Headers (${headers.filter(h => h.enabled && h.key).length})`} />
                                </Tabs>

                                {/* Parameters Table */}
                                <Box sx={{ p: 2 }}>
                                    <TableContainer>
                                        <Table size="small">
                                            <TableHead>
                                                <TableRow>
                                                    <TableCell width="40px"></TableCell>
                                                    <TableCell>Key</TableCell>
                                                    <TableCell>Value</TableCell>
                                                    <TableCell width="40px"></TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {parameters.map((param) => (
                                                    <TableRow key={param.id}>
                                                        <TableCell>
                                                            <Checkbox
                                                                checked={param.enabled}
                                                                onChange={(e) => updateParameter(param.id, 'enabled', e.target.checked)}
                                                                size="small"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <TextField
                                                                fullWidth
                                                                size="small"
                                                                value={param.key}
                                                                onChange={(e) => updateParameter(param.id, 'key', e.target.value)}
                                                                placeholder="key"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <TextField
                                                                fullWidth
                                                                size="small"
                                                                value={param.value}
                                                                onChange={(e) => updateParameter(param.id, 'value', e.target.value)}
                                                                placeholder="value"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <IconButton size="small" onClick={() => removeParameter(param.id)}>
                                                                <DeleteIcon fontSize="small" />
                                                            </IconButton>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                    <Button
                                        startIcon={<AddIcon />}
                                        onClick={addParameter}
                                        size="small"
                                        sx={{ mt: 1 }}
                                    >
                                        Parametre Ekle
                                    </Button>

                                    <Divider sx={{ my: 2 }} />

                                    {/* Headers Table */}
                                    <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                                        Headers
                                    </Typography>
                                    <TableContainer>
                                        <Table size="small">
                                            <TableHead>
                                                <TableRow>
                                                    <TableCell width="40px"></TableCell>
                                                    <TableCell>Key</TableCell>
                                                    <TableCell>Value</TableCell>
                                                    <TableCell width="40px"></TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {headers.map((header) => (
                                                    <TableRow key={header.id}>
                                                        <TableCell>
                                                            <Checkbox
                                                                checked={header.enabled}
                                                                onChange={(e) => updateHeader(header.id, 'enabled', e.target.checked)}
                                                                size="small"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <TextField
                                                                fullWidth
                                                                size="small"
                                                                value={header.key}
                                                                onChange={(e) => updateHeader(header.id, 'key', e.target.value)}
                                                                placeholder="key"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <TextField
                                                                fullWidth
                                                                size="small"
                                                                value={header.value}
                                                                onChange={(e) => updateHeader(header.id, 'value', e.target.value)}
                                                                placeholder="value"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <IconButton size="small" onClick={() => removeHeader(header.id)}>
                                                                <DeleteIcon fontSize="small" />
                                                            </IconButton>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                    <Button
                                        startIcon={<AddIcon />}
                                        onClick={addHeader}
                                        size="small"
                                        sx={{ mt: 1 }}
                                    >
                                        Header Ekle
                                    </Button>

                                    <Divider sx={{ my: 2 }} />

                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={combination}
                                                onChange={(e) => setCombination(e.target.checked)}
                                            />
                                        }
                                        label="Kombinasyon Modu"
                                    />
                                </Box>
                            </Box>
                        )}

                        {/* Execute Button */}
                        <Box sx={{ p: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
                            <Button
                                fullWidth
                                variant="contained"
                                size="medium"
                                startIcon={loading ? <CircularProgress size={18} color="inherit" /> : <SendIcon />}
                                onClick={handleExecute}
                                disabled={loading || !canExecute()}
                                sx={{
                                    py: 1,
                                    fontSize: '0.95rem',
                                    fontWeight: 600,
                                    textTransform: 'none'
                                }}
                            >
                                {loading ? t('executing') : t('execute')}
                            </Button>
                        </Box>
                    </Box>

                    {/* Right Panel - Response */}
                    <Box sx={{ 
                        flex: 1,
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                        backgroundColor: 'action.hover'
                    }}>
                        {/* Response Header */}
                        <Box sx={{ 
                            px: 3,
                            py: 1.5, 
                            borderBottom: '1px solid',
                            borderColor: 'divider',
                            backgroundColor: 'background.paper',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between'
                        }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                <Typography variant="subtitle1" sx={{ fontWeight: 600, color: 'text.primary' }}>
                                    {t('response')}
                                </Typography>
                                {statusCode && (
                                    <Chip
                                        icon={statusCode < 400 ? <CheckCircleIcon /> : <ErrorIcon />}
                                        label={statusCode}
                                        color={statusCode < 400 ? 'success' : 'error'}
                                        size="small"
                                        sx={{ 
                                            fontWeight: 600,
                                            fontSize: '0.875rem',
                                            height: '28px'
                                        }}
                                    />
                                )}
                                {responseTime > 0 && (
                                    <Chip
                                        icon={<AccessTimeIcon fontSize="small" />}
                                        label={`${responseTime}ms`}
                                        size="small"
                                        sx={{ 
                                            backgroundColor: 'action.hover',
                                            color: 'text.primary',
                                            fontWeight: 500,
                                            fontSize: '0.875rem',
                                            height: '28px',
                                            '& .MuiChip-icon': {
                                                color: 'text.secondary'
                                            }
                                        }}
                                    />
                                )}
                            </Box>
                            {response && (
                                <IconButton 
                                    onClick={copyResponse} 
                                    size="small" 
                                    title="Kopyala"
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'action.hover'
                                        }
                                    }}
                                >
                                    <ContentCopyIcon fontSize="small" />
                                </IconButton>
                            )}
                        </Box>

                        {/* Response Body */}
                        <Box sx={{ flex: 1, overflow: 'auto', p: 2, display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                            {loading && (
                                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', width: '100%' }}>
                                    <CircularProgress />
                                </Box>
                            )}

                            {error && !loading && (
                                <Alert severity="error" sx={{ mb: 2 }}>
                                    {error}
                                </Alert>
                            )}

                            {/* XML Response Info - removed as per user request */}

                            {/* Flow Parameters - Combined view */}
                            {selectedFlow && response && !response._type && (response.parameterContext || flowParameters.length > 0) && (
                                <Box sx={{ mb: 1.5, width: '100%' }}>
                                    <Paper sx={{ p: 1.5, backgroundColor: 'success.lighter', border: '1px solid', borderColor: 'success.main' }}>
                                        <Typography variant="body2" sx={{ fontWeight: 600, mb: 1, color: 'text.primary', fontSize: '0.875rem' }}>
                                            {t('generatedParameters')}
                                        </Typography>
                                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                                            {(() => {
                                                // If flowParameters is empty, extract from response.parameterContext
                                                let paramsToShow = flowParameters;
                                                
                                                if (flowParameters.length === 0 && response?.parameterContext) {
                                                    // Create pseudo-parameters from parameterContext keys
                                                    paramsToShow = Object.keys(response.parameterContext).map((key, idx) => ({
                                                        shortCode: key,
                                                        processFlowStepParmId: idx, // Use index as unique ID
                                                        processFlowStepId: idx,
                                                        value: String(response.parameterContext?.[key] ?? ''),
                                                        paramOrder: idx,
                                                        useContext: true,
                                                    }));
                                                }
                                                
                                                // Add unique index to each param BEFORE filtering
                                                const paramsWithUniqueId = paramsToShow.map((param, originalIndex) => ({
                                                    ...param,
                                                    _uniqueIndex: originalIndex
                                                }));
                                                
                                                return paramsWithUniqueId.filter((param) => {
                                                    // Sadece response'ta değeri olan parametreleri göster
                                                    if (!response || typeof response !== 'object') return false;
                                                    
                                                    // Temiz parametre adını çıkar
                                                    let cleanParamName = param.shortCode;
                                                    if (cleanParamName.startsWith('${') && cleanParamName.endsWith('}')) {
                                                        cleanParamName = cleanParamName.slice(2, -1);
                                                    }
                                                    
                                                    // Whitelist: Sadece önemli parametreleri göster
                                                    const importantParams = [
                                                        'username', 'password', 'firstname', 'lastname',
                                                        'tckno', 'telno', 'email', 'msisdn', 'smsval',
                                                        'customerorderid', 'vercode', 'customerid',
                                                        'accountid', 'orderid', 'userid', 'transactionid',
                                                        'externaltransactionid', 'returncode', 'returnmessage'
                                                    ];
                                                    
                                                    // Skip XML-extracted parameters (contains _TransactionId, _ReturnCode, etc.)
                                                    const isXmlParam = cleanParamName.includes('_TransactionId') || 
                                                                      cleanParamName.includes('_ExternalTransactionId') ||
                                                                      cleanParamName.includes('_ReturnCode') ||
                                                                      cleanParamName.includes('_ReturnMessage');
                                                    
                                                    // Don't show XML-extracted parameters
                                                    if (isXmlParam) {
                                                        return false;
                                                    }
                                                    
                                                    // Parametre whitelist'te değilse gösterme
                                                    if (!importantParams.includes(cleanParamName.toLowerCase())) {
                                                        return false;
                                                    }
                                                    
                                                    const paramContext = response.parameterContext || response;
                                                    const value = paramContext[cleanParamName] || 
                                                                 paramContext[param.shortCode] || 
                                                                 paramContext[`\${${cleanParamName}}`];
                                                    
                                                    // Değer varsa ve boş string değilse göster
                                                    return value !== undefined && value !== null && value !== '';
                                                })
                                                .map((param) => {
                                                // Try to find the value in response (what was actually sent)
                                                let requestValue = null;
                                                
                                                // Check if response has this parameter
                                                if (response && typeof response === 'object') {
                                                    // Extract clean parameter name (remove ${} wrapper if present)
                                                    let cleanParamName = param.shortCode;
                                                    if (cleanParamName.startsWith('${') && cleanParamName.endsWith('}')) {
                                                        cleanParamName = cleanParamName.slice(2, -1);
                                                    }
                                                    
                                                    // Also try with ${} wrapper in case it's needed
                                                    const withBraces = `\${${cleanParamName}}`;
                                                    
                                                    // Get the parameter context object
                                                    const paramContext = response.parameterContext || response;
                                                    
                                                    // Try to find value in parameter context (try clean name first, then original, then with braces)
                                                    requestValue = paramContext[cleanParamName] || paramContext[param.shortCode] || paramContext[withBraces];
                                                    
                                                    // If not found, try other locations
                                                    if (requestValue === undefined || requestValue === null) {
                                                        const context = getRecordField(response, 'context');
                                                        const parameters = getRecordField(response, 'parameters');
                                                        const dataParameterContext = getRecordField(getRecordField(response, 'data'), 'parameterContext');
                                                        const resultParameterContext = getRecordField(response.result, 'parameterContext');

                                                        requestValue = context?.[cleanParamName] ||
                                                                      context?.[param.shortCode] ||
                                                                      context?.[withBraces] ||
                                                                      parameters?.[cleanParamName] ||
                                                                      parameters?.[param.shortCode] ||
                                                                      parameters?.[withBraces] ||
                                                                      dataParameterContext?.[cleanParamName] ||
                                                                      dataParameterContext?.[param.shortCode] ||
                                                                      dataParameterContext?.[withBraces] ||
                                                                      resultParameterContext?.[cleanParamName] ||
                                                                      resultParameterContext?.[param.shortCode] ||
                                                                      resultParameterContext?.[withBraces];
                                                    }
                                                }
                                                
                                                // Format value for display
                                                let displayValue = '(henüz çalıştırılmadı)';
                                                let hasValue = false;
                                                
                                                if (requestValue !== null && requestValue !== undefined) {
                                                    hasValue = true;
                                                    let valueStr = String(requestValue);
                                                    
                                                    // Remove extra quotes if present
                                                    if (valueStr.startsWith('"') && valueStr.endsWith('"')) {
                                                        valueStr = valueStr.slice(1, -1);
                                                    }
                                                    
                                                    // Show empty string as "(boş)"
                                                    if (valueStr === '') {
                                                        displayValue = '(boş)';
                                                    } else {
                                                        // Truncate if too long (max 50 chars for better display)
                                                        const maxLength = 50;
                                                        if (valueStr.length > maxLength) {
                                                            valueStr = valueStr.substring(0, maxLength) + '...';
                                                        }
                                                        displayValue = `"${valueStr}"`;
                                                    }
                                                }
                                                
                                                return (
                                                    <Box key={`param-${param._uniqueIndex}-${param.shortCode}`} sx={{ 
                                                        display: 'flex', 
                                                        alignItems: 'center', 
                                                        gap: 0.75,
                                                        p: 0.75,
                                                        backgroundColor: 'background.paper',
                                                        borderRadius: 0.75,
                                                        border: '1px solid',
                                                        borderColor: 'divider'
                                                    }}>
                                                        <Chip 
                                                            label={`\${${param.shortCode}}`}
                                                            size="small"
                                                            onClick={() => {
                                                                navigator.clipboard.writeText(`\${${param.shortCode}}`);
                                                            }}
                                                            sx={{ 
                                                                backgroundColor: 'success.light',
                                                                color: 'success.dark',
                                                                fontWeight: 600,
                                                                fontSize: '0.7rem',
                                                                fontFamily: 'monospace',
                                                                minWidth: '110px',
                                                                height: '24px',
                                                                cursor: 'pointer',
                                                                '&:hover': {
                                                                    backgroundColor: 'success.main',
                                                                    color: 'white',
                                                                    boxShadow: 1
                                                                }
                                                            }} 
                                                        />
                                                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>→</Typography>
                                                        <Typography variant="body2" sx={{ 
                                                            fontFamily: 'monospace', 
                                                            color: hasValue ? 'info.dark' : 'text.disabled',
                                                            fontWeight: 600,
                                                            backgroundColor: hasValue ? 'info.lighter' : 'transparent',
                                                            px: 1,
                                                            py: 0.4,
                                                            borderRadius: 0.5,
                                                            flex: 1,
                                                            overflow: 'hidden',
                                                            textOverflow: 'ellipsis',
                                                            whiteSpace: 'nowrap',
                                                            fontSize: '0.8rem'
                                                        }}>
                                                            {displayValue}
                                                        </Typography>
                                                    </Box>
                                                );
                                            })})()}
                                        </Box>
                                    </Paper>
                                </Box>
                            )}

                            {response && !loading && (
                                <Box sx={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 1 }}>
                                    {/* Result Section - Show each API result as accordion */}
                                    {(() => {
                                        const resultData = response.Result || response.result;
                                        if (resultData && typeof resultData === 'object') {
                                            return (
                                                <Box sx={{ width: '100%' }}>
                                                    {Object.entries(resultData).map(([key, value]) => (
                                                        <Accordion
                                                            key={key}
                                                            expanded={expandedAccordions[`result_${key}`] || false}
                                                            onChange={(_, isExpanded) => 
                                                                setExpandedAccordions(prev => ({ ...prev, [`result_${key}`]: isExpanded }))
                                                            }
                                                            sx={{
                                                                mb: 1,
                                                                border: '1px solid',
                                                                borderColor: 'divider',
                                                                '&:before': { display: 'none' }
                                                            }}
                                                        >
                                                            <AccordionSummary
                                                                expandIcon={<ExpandMoreIcon />}
                                                                sx={{
                                                                    backgroundColor: 'action.hover',
                                                                    '&:hover': {
                                                                        backgroundColor: 'action.selected'
                                                                    }
                                                                }}
                                                            >
                                                                <Typography
                                                                    variant="subtitle2"
                                                                    sx={{
                                                                        fontWeight: 700,
                                                                        color: 'text.primary',
                                                                        fontFamily: 'monospace'
                                                                    }}
                                                                >
                                                                    {key}
                                                                </Typography>
                                                            </AccordionSummary>
                                                            <AccordionDetails sx={{ p: 2 }}>
                                                                <Box
                                                                    sx={{
                                                                        p: 1.5,
                                                                        backgroundColor: 'action.hover',
                                                                        borderRadius: 1,
                                                                        fontFamily: 'monospace',
                                                                        fontSize: '12px',
                                                                        wordBreak: 'break-all',
                                                                        whiteSpace: 'pre-wrap',
                                                                        maxHeight: '400px',
                                                                        overflow: 'auto'
                                                                    }}
                                                                >
                                                                    {typeof value === 'string' ? value : JSON.stringify(value, null, 2)}
                                                                </Box>
                                                            </AccordionDetails>
                                                        </Accordion>
                                                    ))}
                                                </Box>
                                            );
                                        }
                                        return null;
                                    })()}

                                    {/* Parametreler Section - Show as accordion */}
                                    {(() => {
                                        const parametreler = response.Parametreler || response.parametreler;
                                        if (parametreler && typeof parametreler === 'object') {
                                            return (
                                                <Accordion
                                                    expanded={expandedAccordions['parametreler'] || false}
                                                    onChange={(_, isExpanded) => 
                                                        setExpandedAccordions(prev => ({ ...prev, parametreler: isExpanded }))
                                                    }
                                                    sx={{
                                                        mb: 1,
                                                        border: '1px solid',
                                                        borderColor: 'success.main',
                                                        '&:before': { display: 'none' }
                                                    }}
                                                >
                                                    <AccordionSummary
                                                        expandIcon={<ExpandMoreIcon />}
                                                        sx={{
                                                            backgroundColor: 'success.lighter',
                                                            '&:hover': {
                                                                backgroundColor: 'success.light'
                                                            }
                                                        }}
                                                    >
                                                        <Typography
                                                            variant="subtitle2"
                                                            sx={{
                                                                fontWeight: 700,
                                                                color: 'success.dark',
                                                                fontFamily: 'monospace'
                                                            }}
                                                        >
                                                            Parametreler ({Object.keys(parametreler).length} items)
                                                        </Typography>
                                                    </AccordionSummary>
                                                    <AccordionDetails sx={{ p: 2 }}>
                                                        <Box
                                                            sx={{
                                                                p: 1.5,
                                                                backgroundColor: 'action.hover',
                                                                borderRadius: 1,
                                                                fontFamily: 'monospace',
                                                                fontSize: '12px',
                                                                wordBreak: 'break-all',
                                                                whiteSpace: 'pre-wrap',
                                                                maxHeight: '400px',
                                                                overflow: 'auto'
                                                            }}
                                                        >
                                                            {JSON.stringify(parametreler, null, 2)}
                                                        </Box>
                                                    </AccordionDetails>
                                                </Accordion>
                                            );
                                        }
                                        return null;
                                    })()}

                                    {/* ParameterContext - Collapsible */}
                                    {response.parameterContext && typeof response.parameterContext === 'object' && (
                                        <Accordion
                                            expanded={expandedAccordions['parameterContext'] || false}
                                            onChange={(_, isExpanded) => 
                                                setExpandedAccordions(prev => ({ ...prev, parameterContext: isExpanded }))
                                            }
                                            sx={{
                                                mb: 1,
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                '&:before': { display: 'none' }
                                            }}
                                        >
                                            <AccordionSummary
                                                expandIcon={<ExpandMoreIcon />}
                                                sx={{
                                                    backgroundColor: 'action.hover',
                                                    '&:hover': {
                                                        backgroundColor: 'action.selected'
                                                    }
                                                }}
                                            >
                                                <Typography
                                                    variant="subtitle2"
                                                    sx={{
                                                        fontWeight: 700,
                                                        fontFamily: 'monospace'
                                                    }}
                                                >
                                                    parameterContext ({Object.keys(response.parameterContext).length} items)
                                                </Typography>
                                            </AccordionSummary>
                                            <AccordionDetails sx={{ p: 2 }}>
                                                <Box
                                                    sx={{
                                                        fontFamily: 'monospace',
                                                        fontSize: '12px',
                                                        whiteSpace: 'pre-wrap',
                                                        wordBreak: 'break-word'
                                                    }}
                                                >
                                                    {JSON.stringify(response.parameterContext, null, 2)}
                                                </Box>
                                            </AccordionDetails>
                                        </Accordion>
                                    )}

                                    {/* Other Response Data - if no result structure */}
                                    {!response.Result && !response.result && !response.Parametreler && !response.parametreler && (
                                        <Box
                                            sx={{
                                                backgroundColor: 'background.paper',
                                                borderRadius: 1,
                                                overflow: 'auto',
                                                width: '100%',
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                textAlign: 'left'
                                            }}
                                        >
                                            <Box 
                                                sx={{
                                                    margin: 0, 
                                                    padding: '16px',
                                                    whiteSpace: 'pre-wrap', 
                                                    wordBreak: 'break-word',
                                                    color: 'text.primary',
                                                    fontFamily: '"Consolas", "Monaco", "Courier New", monospace',
                                                    fontSize: '13px',
                                                    lineHeight: '1.6',
                                                    backgroundColor: 'transparent',
                                                    textAlign: 'left',
                                                    width: '100%',
                                                    '& .json-key': { 
                                                        color: 'text.primary', 
                                                        fontWeight: 600 
                                                    },
                                                    '& .json-api-key': { 
                                                        color: 'error.main', 
                                                        fontWeight: 700 
                                                    },
                                                    '& .json-value': { 
                                                        color: 'info.main' 
                                                    },
                                                    '& .json-boolean': { 
                                                        color: 'primary.main', 
                                                        fontWeight: 600 
                                                    },
                                                    '& .json-number': { 
                                                        color: 'primary.main' 
                                                    }
                                                }}
                                                dangerouslySetInnerHTML={{ __html: formatResponse(response) }}
                                            />
                                        </Box>
                                    )}
                                </Box>
                            )}

                            {!response && !loading && !error && (
                                <Box
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        height: '100%',
                                        color: 'text.secondary'
                                    }}
                                >
                                    <Typography>{t('noRequest')}</Typography>
                                </Box>
                            )}
                        </Box>
                    </Box>
                </Box>
            </Box>
        </DashboardLayout>
    );
}
