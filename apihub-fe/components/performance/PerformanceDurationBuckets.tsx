'use client';

import { Box, LinearProgress, Typography } from '@mui/material';
import { PerformanceResponseTimeBuckets } from '@/types/performance';

interface PerformanceDurationBucketsProps {
    buckets?: PerformanceResponseTimeBuckets | null;
}

export default function PerformanceDurationBuckets({ buckets }: PerformanceDurationBucketsProps) {
    if (!buckets) {
        return <Typography variant="body2">-</Typography>;
    }

    const rows = [
        { label: '<500ms', value: buckets.under500ms ?? 0, color: 'success' as const },
        { label: '500ms-1s', value: buckets.from500msTo1s ?? 0, color: 'info' as const },
        { label: '1s-3s', value: buckets.from1sTo3s ?? 0, color: 'warning' as const },
        { label: '>3s', value: buckets.over3s ?? 0, color: 'error' as const },
    ];
    const total = rows.reduce((sum, row) => sum + row.value, 0);

    if (total === 0) {
        return <Typography variant="body2">-</Typography>;
    }

    return (
        <Box sx={{ minWidth: 160 }}>
            {rows.map((row) => {
                const percent = (row.value * 100) / total;
                const color = row.label === '>3s' && percent > 30 ? 'error' : row.color;
                return (
                    <Box key={row.label} sx={{ display: 'grid', gridTemplateColumns: '60px 1fr 44px', gap: 1, alignItems: 'center', mb: 0.5 }}>
                        <Typography variant="caption">{row.label}</Typography>
                        <LinearProgress variant="determinate" value={percent} color={color} sx={{ height: 6, borderRadius: 1 }} />
                        <Typography variant="caption" sx={{ textAlign: 'right' }}>{percent.toFixed(0)}%</Typography>
                    </Box>
                );
            })}
        </Box>
    );
}
