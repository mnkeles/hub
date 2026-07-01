'use client';
import { useState } from 'react';
import {
    Card,
    CardContent,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TablePagination,
    Chip,
    IconButton,
    Collapse,
    Box,
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import { PerformanceResultDto } from '@/types/performance';

interface Props {
    projectShortCode: string;
    refreshKey: number;
}

function ResultRow({ result }: { result: PerformanceResultDto }) {
    const [open, setOpen] = useState(false);

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'RUNNING': return 'info';
            case 'SUCCESS': return 'success';
            case 'FAILED': return 'error';
            default: return 'default';
        }
    };

    return (
        <>
            <TableRow>
                <TableCell>
                    <IconButton size="small" onClick={() => setOpen(!open)}>
                        {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                    </IconButton>
                </TableCell>
                <TableCell>{result.performanceResultId}</TableCell>
                <TableCell>{result.processFlowId}</TableCell>
                <TableCell>{result.threadCount}</TableCell>
                <TableCell>{result.rampUpPeriod}s</TableCell>
                <TableCell>
                    <Chip
                        label={result.performanceStatus}
                        color={getStatusColor(result.performanceStatus)}
                        size="small"
                    />
                </TableCell>
                <TableCell>-</TableCell>
            </TableRow>
            <TableRow>
                <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={7}>
                    <Collapse in={open} timeout="auto" unmountOnExit>
                        <Box sx={{ margin: 2 }}>
                            <Typography variant="h6" gutterBottom>
                                Test Detayları
                            </Typography>
                            <Typography color="text.secondary">
                                Thread Count: {result.threadCount}, Ramp Up: {result.rampUpPeriod}s
                            </Typography>
                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
        </>
    );
}

export default function PerformanceResultsGrid({ projectShortCode, refreshKey }: Props) {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    // TODO: Implement getPerformanceResults method in performanceService
    const results: PerformanceResultDto[] = [];

    return (
        <Card>
            <CardContent>
                <Typography variant="h6" gutterBottom>
                    Performans Test Sonuçları
                </Typography>
                <TableContainer>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell />
                                <TableCell>ID</TableCell>
                                <TableCell>Process Flow</TableCell>
                                <TableCell>Thread Count</TableCell>
                                <TableCell>Ramp Up</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>Start Time</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {results && results.map((result: PerformanceResultDto) => (
                                <ResultRow key={result.performanceResultId} result={result} />
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
                <TablePagination
                    component="div"
                    count={100} // Backend'den total count alınmalı
                    page={page}
                    onPageChange={(_, newPage) => setPage(newPage)}
                    rowsPerPage={rowsPerPage}
                    onRowsPerPageChange={(e) => {
                        setRowsPerPage(parseInt(e.target.value, 10));
                        setPage(0);
                    }}
                />
            </CardContent>
        </Card>
    );
}