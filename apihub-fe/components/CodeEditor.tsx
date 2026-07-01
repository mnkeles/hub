'use client';

import React from 'react';
import { Box, Typography, TextField } from '@mui/material';

interface CodeEditorProps {
    value: string;
    onChange: (value: string) => void;
    language: 'sql' | 'json' | 'javascript';
    label?: string;
    height?: string;
    readOnly?: boolean;
}

export default function CodeEditor({ 
    value, 
    onChange, 
    language, 
    label,
    height = '200px',
    readOnly = false 
}: CodeEditorProps) {
    return (
        <Box>
            {label && (
                <Typography variant="body2" sx={{ mb: 1, fontWeight: 500 }}>
                    {label}
                </Typography>
            )}
            <TextField
                multiline
                fullWidth
                value={value || ''}
                onChange={(e) => onChange(e.target.value)}
                disabled={readOnly}
                placeholder={`Enter ${language} code...`}
                sx={{
                    '& .MuiInputBase-root': {
                        fontFamily: '"Consolas", "Monaco", "Courier New", monospace',
                        fontSize: '13px',
                        lineHeight: '1.6',
                        padding: 0,
                    },
                    '& .MuiInputBase-input': {
                        height: height,
                        overflow: 'auto !important',
                        padding: '12px',
                    },
                    '& textarea': {
                        height: `${height} !important`,
                    }
                }}
            />
        </Box>
    );
}
