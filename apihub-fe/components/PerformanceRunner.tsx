'use client';
import { useState, useEffect } from 'react';
import {
    Card,
    CardContent,
    TextField,
    Button,
    MenuItem,
    Alert,
    CircularProgress,
    Box,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { performanceService } from '@/services/performanceService';
import { processFlowService } from '@/services/processFlowService';
import { ProcessFlowDto } from '@/types/api';
import { getApiErrorMessage } from '@/lib/errorUtils';

interface Props {
    projectShortCode: string;
    onTestComplete?: () => void;
}

export default function PerformanceRunner({ projectShortCode, onTestComplete }: Props) {
    const [processFlows, setProcessFlows] = useState<ProcessFlowDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);

    const [formData, setFormData] = useState({
        projectId: 1, // Bu değeri dinamik yapabilirsiniz
        processFlowId: 0,
        threadCount: 10,
        rampUpPeriod: 5,
        environment: 'TEST',
    });

    useEffect(() => {
        loadProcessFlows();
    }, []);

    const loadProcessFlows = async () => {
        try {
            const flows = await processFlowService.getAll();
            // Filter by projectId if needed
            const filteredFlows = flows.filter(f => f.projectId === formData.projectId);
            setProcessFlows(filteredFlows);
        } catch (err) {
            console.error('Process flows yüklenemedi:', err);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(false);

        try {
            await performanceService.runPerformanceTest(formData);
            setSuccess(true);
            onTestComplete?.();

            setTimeout(() => setSuccess(false), 3000);
        } catch (err) {
            setError(getApiErrorMessage(err, 'Test başlatılamadı'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <Card>
            <CardContent>
                <form onSubmit={handleSubmit}>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
                        <TextField
                            fullWidth
                            select
                            label="Process Flow"
                            value={formData.processFlowId}
                            onChange={(e) => setFormData({ ...formData, processFlowId: Number(e.target.value) })}
                            required
                        >
                            <MenuItem value={0}>Seçiniz</MenuItem>
                            {processFlows.map((flow) => (
                                <MenuItem key={flow.processFlowId || 0} value={flow.processFlowId || 0}>
                                    {flow.shortCode}
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            fullWidth
                            select
                            label="Environment"
                            value={formData.environment}
                            onChange={(e) => setFormData({ ...formData, environment: e.target.value })}
                            required
                        >
                            <MenuItem value="TEST">TEST</MenuItem>
                            <MenuItem value="PROD">PROD</MenuItem>
                            <MenuItem value="DEV">DEV</MenuItem>
                        </TextField>

                        <TextField
                            fullWidth
                            type="number"
                            label="Thread Count"
                            value={formData.threadCount}
                            onChange={(e) => setFormData({ ...formData, threadCount: Number(e.target.value) })}
                            required
                            inputProps={{ min: 1, max: 1000 }}
                        />

                        <TextField
                            fullWidth
                            type="number"
                            label="Ramp Up Period (s)"
                            value={formData.rampUpPeriod}
                            onChange={(e) => setFormData({ ...formData, rampUpPeriod: Number(e.target.value) })}
                            required
                            inputProps={{ min: 1, max: 300 }}
                        />

                        <Box sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}>
                            <Button
                                fullWidth
                                type="submit"
                                variant="contained"
                                size="large"
                                disabled={loading || formData.processFlowId === 0}
                                startIcon={loading ? <CircularProgress size={20} /> : <PlayArrowIcon />}
                                sx={{ height: '56px' }}
                            >
                                {loading ? 'Test Başlatılıyor...' : 'Performans Testi Başlat'}
                            </Button>
                        </Box>
                    </Box>

                    {error && (
                        <Box sx={{ mt: 2 }}>
                            <Alert severity="error">{error}</Alert>
                        </Box>
                    )}

                    {success && (
                        <Box sx={{ mt: 2 }}>
                            <Alert severity="success">Performans testi başarıyla başlatıldı!</Alert>
                        </Box>
                    )}
                </form>
            </CardContent>
        </Card>
    );
}
