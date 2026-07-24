'use client';

import { Alert, Box, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import {
    Bar,
    BarChart,
    CartesianGrid,
    Legend,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from 'recharts';
import { PerformanceDetailResponse, PerformanceStepSummary } from '@/types/performance';

interface PerformanceChartsPanelProps {
    detail: PerformanceDetailResponse | null;
    summaries: PerformanceStepSummary[];
}

interface TimeSeriesRow {
    time: string;
    elapsedTime: number;
    errorRate: number;
    throughput: number;
}

function buildTimeSeries(detail: PerformanceDetailResponse | null): TimeSeriesRow[] {
    const rows = detail?.groups.flatMap((group) => group.steps.map((step) => ({
        finishedAt: step.finishedAt,
        elapsedTime: step.elapsedTime,
        failed: step.performanceItemStatus === 'FAILED',
    }))) ?? [];
    const timestamped = rows
        .filter((row) => row.finishedAt)
        .sort((a, b) => new Date(a.finishedAt || 0).getTime() - new Date(b.finishedAt || 0).getTime());

    let completed = 0;
    let failed = 0;
    const firstTime = timestamped[0]?.finishedAt ? new Date(timestamped[0].finishedAt).getTime() : 0;

    return timestamped.map((row) => {
        completed += 1;
        failed += row.failed ? 1 : 0;
        const currentTime = new Date(row.finishedAt || 0).getTime();
        const elapsedSeconds = Math.max(1, (currentTime - firstTime) / 1000);
        return {
            time: new Date(row.finishedAt || '').toLocaleTimeString('tr-TR'),
            elapsedTime: row.elapsedTime,
            errorRate: (failed * 100) / completed,
            throughput: completed / elapsedSeconds,
        };
    });
}

export default function PerformanceChartsPanel({ detail, summaries }: PerformanceChartsPanelProps) {
    const t = useTranslations('performance');
    const timeSeries = buildTimeSeries(detail);
    const stepChartData = summaries.map((summary) => ({
        step: summary.stepName,
        average: summary.averageElapsedTime,
        p95: summary.p95ElapsedTime,
        p99: summary.p99ElapsedTime,
    }));

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            {timeSeries.length === 0 ? (
                <Alert severity="info">{t('timeSeriesUnavailable')}</Alert>
            ) : (
                <>
                    <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>Response Time Over Time</Typography>
                        <ResponsiveContainer width="100%" height={260}>
                            <LineChart data={timeSeries}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="time" />
                                <YAxis />
                                <Tooltip />
                                <Legend />
                                <Line type="monotone" dataKey="elapsedTime" stroke="#1976d2" dot={false} />
                            </LineChart>
                        </ResponsiveContainer>
                    </Box>
                    <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>Throughput / Error Rate Over Time</Typography>
                        <ResponsiveContainer width="100%" height={260}>
                            <LineChart data={timeSeries}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="time" />
                                <YAxis />
                                <Tooltip />
                                <Legend />
                                <Line type="monotone" dataKey="throughput" stroke="#2e7d32" dot={false} />
                                <Line type="monotone" dataKey="errorRate" stroke="#d32f2f" dot={false} />
                            </LineChart>
                        </ResponsiveContainer>
                    </Box>
                </>
            )}

            {stepChartData.length > 0 && (
                <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>Step P95 / P99</Typography>
                    <ResponsiveContainer width="100%" height={320}>
                        <BarChart data={stepChartData}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="step" interval={0} angle={-30} textAnchor="end" height={90} />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Bar dataKey="average" fill="#1976d2" />
                            <Bar dataKey="p95" fill="#ed6c02" />
                            <Bar dataKey="p99" fill="#d32f2f" />
                        </BarChart>
                    </ResponsiveContainer>
                </Box>
            )}
        </Box>
    );
}
