'use client';

import { Alert, Box, Tooltip, Typography } from '@mui/material';

interface PerformanceFailureReasonsProps {
    reasons?: string[] | null;
    maxVisible?: number;
}

export default function PerformanceFailureReasons({ reasons, maxVisible = 2 }: PerformanceFailureReasonsProps) {
    if (!reasons || reasons.length === 0) {
        return <Typography variant="body2">-</Typography>;
    }

    const visibleReasons = reasons.slice(0, maxVisible);
    const hiddenReasons = reasons.slice(maxVisible);

    return (
        <Alert severity="warning" sx={{ py: 0.75 }}>
            <Box component="ul" sx={{ m: 0, pl: 2 }}>
                {visibleReasons.map((reason, index) => (
                    <Typography key={`${reason}-${index}`} component="li" variant="body2">
                        {reason}
                    </Typography>
                ))}
            </Box>
            {hiddenReasons.length > 0 && (
                <Tooltip title={hiddenReasons.join('\n')}>
                    <Typography variant="caption" sx={{ display: 'inline-block', mt: 0.5, cursor: 'help' }}>
                        +{hiddenReasons.length}
                    </Typography>
                </Tooltip>
            )}
        </Alert>
    );
}
