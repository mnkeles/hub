'use client';

import React, { useEffect, useState } from 'react';
import {
    Box,
    Paper,
    Typography,
    Card,
    CardContent,
    LinearProgress,
    CircularProgress,
} from '@mui/material';
import SpeedIcon from '@mui/icons-material/Speed';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import DashboardLayout from '@/components/DashboardLayout';
import { useProject } from '@/contexts/ProjectContext';
import { processFlowService } from '@/services/processFlowService';
import { performanceService } from '@/services/performanceService';

interface StatCardProps {
    title: string;
    value: string | number;
    icon: React.ReactNode;
    color: string;
    subtitle?: string;
    loading?: boolean;
}

function StatCard({ title, value, icon, color, subtitle, loading }: StatCardProps) {
    return (
        <Card
            sx={{
                height: '100%',
                background: `linear-gradient(135deg, ${color}15 0%, ${color}05 100%)`,
                border: `1px solid ${color}30`,
            }}
        >
            <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Box
                        sx={{
                            backgroundColor: color,
                            borderRadius: 2,
                            p: 1,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'white',
                            mr: 2,
                        }}
                    >
                        {icon}
                    </Box>
                    <Typography variant="h6" color="text.secondary">
                        {title}
                    </Typography>
                </Box>
                {loading ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', height: '52px' }}>
                        <CircularProgress size={24} />
                    </Box>
                ) : (
                    <>
                        <Typography variant="h3" sx={{ fontWeight: 700, mb: 1 }}>
                            {value}
                        </Typography>
                        {subtitle && (
                            <Typography variant="body2" color="text.secondary">
                                {subtitle}
                            </Typography>
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
}

interface RecentTest {
    name: string;
    status: 'success' | 'running' | 'failed';
    time: string;
    progress: number;
    timestamp?: number; // For sorting
}

interface FailedService {
    name: string;
    status: 'failed' | 'broken';
}

interface AllureTreeNode {
    name?: string;
    status?: 'passed' | 'failed' | 'broken' | string;
    statistic?: {
        passed?: number;
        failed?: number;
        broken?: number;
    };
    children?: AllureTreeNode[];
    error?: string;
}

interface AllureCategory {
    name?: string;
    total?: number;
    uid?: string;
}

export default function AdminDashboard() {
    const { selectedProject } = useProject();
    const [totalProcessFlows, setTotalProcessFlows] = useState<number>(0);
    const [performanceTests, setPerformanceTests] = useState<number>(0);
    const [passedTests, setPassedTests] = useState<number>(0);
    const [failedTests, setFailedTests] = useState<number>(0);
    const [loading, setLoading] = useState(true);
    const [recentTests, setRecentTests] = useState<RecentTest[]>([]);
    const [failedServices, setFailedServices] = useState<FailedService[]>([]);

    useEffect(() => {
        if (selectedProject) {
            console.log('🔄 Project changed to:', selectedProject.shortCode);
            // Reset states before fetching new data
            setLoading(true);
            setTotalProcessFlows(0);
            setPerformanceTests(0);
            setPassedTests(0);
            setFailedTests(0);
            setFailedServices([]);
            setRecentTests([]);
            
            fetchDashboardData();
        }
    }, [selectedProject?.projectId, selectedProject?.shortCode]); // Watch both projectId and shortCode

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            // Fetch all data in parallel for better performance
            await Promise.all([
                fetchTotalProcessFlows().catch(err => console.error('Failed to fetch Process Flows:', err)),
                fetchPerformanceTests().catch(err => console.error('Failed to fetch Performance Tests:', err)),
                fetchFailedTests().catch(err => console.error('Failed to fetch Failed Tests:', err)),
                fetchRecentTests().catch(err => console.error('Failed to fetch Recent Tests:', err)),
            ]);
            
            console.log('✅ Dashboard data loaded successfully');
        } catch (error) {
            console.error('❌ Error fetching dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchPerformanceTests = async () => {
        try {
            console.log('📊 Fetching Performance Tests for project:', selectedProject?.shortCode);
            if (!selectedProject || !selectedProject.projectId) {
                console.log('⚠️ No project selected, skipping performance tests');
                setPerformanceTests(0);
                return;
            }

            // Get all process flows for the selected project
            const flows = await processFlowService.getByProject(selectedProject.projectId);
            console.log('📋 Found', flows.length, 'process flows');
            
            // Count total performance tests from all flows
            let totalTests = 0;
            for (const flow of flows) {
                if (flow.processFlowId) {
                    try {
                        const history = await performanceService.getHistory(
                            selectedProject.projectId,
                            flow.processFlowId
                        );
                        
                        // Filter tests from last 30 days
                        const thirtyDaysAgo = new Date();
                        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
                        
                        const recentTests = history.filter(item => {
                            if (!item.createdAt) return false;
                            const date = new Date(item.createdAt);
                            return date >= thirtyDaysAgo && date.getFullYear() !== 1970;
                        });
                        
                        totalTests += recentTests.length;
                    } catch (err) {
                        console.error(`Error fetching history for flow ${flow.processFlowId}:`, err);
                    }
                }
            }
            
            console.log('✅ Total Performance Tests (last 30 days):', totalTests);
            setPerformanceTests(totalTests);
        } catch (error) {
            console.error('❌ Error fetching performance tests:', error);
            setPerformanceTests(0);
        }
    };

    const fetchTotalProcessFlows = async () => {
        try {
            console.log('📊 Fetching Process Flows for project:', selectedProject?.shortCode);
            const flows = selectedProject
                ? await processFlowService.getByProject(selectedProject.projectId!)
                : await processFlowService.getAll();
            
            console.log('✅ Total Process Flows:', flows.length);
            setTotalProcessFlows(flows.length);
        } catch (error) {
            console.error('❌ Error fetching process flows:', error);
            setTotalProcessFlows(0);
        }
    };

    const fetchFailedTests = async () => {
        try {
            console.log('Starting to fetch failed tests from Allure...');
            
            // Get project short code for Allure
            const projectShortCode = selectedProject?.shortCode || 'OMNI';
            console.log('Fetching Allure data for project:', projectShortCode);
            
            // Use Next.js API proxy to avoid CORS issues
            // First, try to get the summary data
            try {
                console.log('Trying suites.json...');
                const summaryResponse = await fetch(`/api/allure-proxy?endpoint=suites.json&project=${projectShortCode}`);
                console.log('Suites response status:', summaryResponse.status);
                
                if (summaryResponse.ok) {
                    const summaryData = await summaryResponse.json();
                    console.log('Suites data:', summaryData);
                    console.log('Children count:', summaryData.children?.length);
                    console.log('First child:', summaryData.children?.[0]);
                    
                    // Check if there's an error in the response
                    if (summaryData.error) {
                        console.error('Suites data error:', summaryData.error);
                        throw new Error(summaryData.error);
                    }
                    
                    // Recursive function to count tests and collect service names
                    const failedServicesList: FailedService[] = [];
                    let passedCount = 0;
                    let failedCount = 0;
                    
                    const countTests = (node: AllureTreeNode): void => {
                        // Check if current node has status (individual test)
                        if (node.status) {
                            if (node.status === 'passed') {
                                passedCount += 1;
                            } else if (node.status === 'failed' || node.status === 'broken') {
                                console.log('🔴 Found failed/broken test:', node.name, '| Status:', node.status);
                                
                                // Extract service name from test name
                                const serviceName = node.name?.split(':').pop()?.trim() || node.name || 'Unknown service';
                                failedServicesList.push({
                                    name: serviceName,
                                    status: node.status
                                });
                                
                                failedCount += 1;
                            }
                        }
                        
                        // Also check if current node has statistics (suite level)
                        if (node.statistic) {
                            console.log('📊 Found statistic:', node.name, node.statistic);
                            passedCount += node.statistic.passed || 0;
                            failedCount += (node.statistic.failed || 0) + (node.statistic.broken || 0);
                        }
                        
                        // Recursively check children
                        if (Array.isArray(node.children)) {
                            node.children.forEach((child) => {
                                countTests(child);
                            });
                        }
                    };
                    
                    countTests(summaryData);
                    
                    console.log('✅ Passed tests:', passedCount);
                    console.log('❌ Failed tests:', failedCount);
                    console.log('📋 Failed services list:', failedServicesList.length, 'services');
                    
                    setPassedTests(passedCount);
                    setFailedTests(failedCount);
                    setFailedServices(failedServicesList); // Show all failed services
                    return;
                }
            } catch (err) {
                console.error('Failed to fetch suites.json, trying alternative method:', err);
            }

            // Primary method: Try to fetch categories data (most reliable)
            try {
                console.log('Trying categories.json (primary method)...');
                const categoriesResponse = await fetch(`/api/allure-proxy?endpoint=categories.json&project=${projectShortCode}`);
                console.log('Categories response status:', categoriesResponse.status);
                
                if (categoriesResponse.ok) {
                    const categoriesData = await categoriesResponse.json();
                    console.log('Categories data:', categoriesData);
                    
                    // Check if there's an error in the response
                    if (categoriesData.error) {
                        console.error('Categories data error:', categoriesData.error);
                        throw new Error(categoriesData.error);
                    }
                    
                    let failedCount = 0;
                    
                    if (Array.isArray(categoriesData)) {
                        categoriesData.forEach((category: AllureCategory) => {
                            console.log('📊 Category:', category.name, '| Total:', category.total, '| UID:', category.uid);
                            // Sum up all categories (Product defects, Test defects, etc.)
                            // Categories with failures typically include all test failures
                            failedCount += category.total || 0;
                        });
                    }
                    
                    console.log('✅ Total failed tests from categories:', failedCount);
                    setFailedTests(failedCount);
                    return;
                }
            } catch (err) {
                console.error('❌ Failed to fetch categories.json:', err);
            }

            // Last resort: Try widgets summary
            try {
                console.log('Trying widgets/summary.json (last resort)...');
                const widgetsResponse = await fetch(`/api/allure-proxy?endpoint=widgets/summary.json&project=${projectShortCode}`);
                console.log('Widgets response status:', widgetsResponse.status);
                
                if (widgetsResponse.ok) {
                    const widgetsData = await widgetsResponse.json();
                    console.log('Widgets data:', widgetsData);
                    
                    if (widgetsData.statistic) {
                        const failedCount = (widgetsData.statistic.failed || 0) + (widgetsData.statistic.broken || 0);
                        console.log('✅ Total failed tests from widgets:', failedCount);
                        setFailedTests(failedCount);
                        return;
                    }
                }
            } catch (err) {
                console.error('❌ Failed to fetch widgets/summary.json:', err);
            }

            // If all methods fail, set to 0
            console.warn('⚠️ All methods failed, setting failed tests to 0');
            setFailedTests(0);
        } catch (error) {
            console.error('Error fetching failed tests from Allure:', error);
            setFailedTests(0); // Default value
        }
    };

    const fetchRecentTests = async () => {
        try {
            console.log('📊 Fetching Recent Tests for project:', selectedProject?.shortCode);
            if (!selectedProject || !selectedProject.projectId) {
                console.log('⚠️ No project selected, skipping recent tests');
                setRecentTests([]);
                return;
            }

            // Get all process flows for the selected project
            const flows = await processFlowService.getByProject(selectedProject.projectId);
            
            const allTests: RecentTest[] = [];
            
            // Get tests from all flows
            for (const flow of flows) {
                if (flow.processFlowId) {
                    try {
                        const history = await performanceService.getHistory(
                            selectedProject.projectId,
                            flow.processFlowId
                        );
                        
                        // Get recent tests (filter valid dates)
                        const validHistory = history.filter(item => {
                            if (!item.createdAt) return false;
                            const date = new Date(item.createdAt);
                            return !isNaN(date.getTime()) && date.getFullYear() !== 1970;
                        });
                        
                        // Sort by date and take most recent
                        const sortedHistory = validHistory.sort((a, b) => 
                            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
                        );
                        
                        // Add all tests from this flow
                        sortedHistory.forEach(item => {
                            const testDate = new Date(item.createdAt);
                            const now = new Date();
                            const diffMs = now.getTime() - testDate.getTime();
                            const diffMins = Math.floor(diffMs / 60000);
                            const diffHours = Math.floor(diffMs / 3600000);
                            
                            let timeStr = '';
                            if (diffMins < 60) {
                                timeStr = `${diffMins} mins ago`;
                            } else if (diffHours < 24) {
                                timeStr = `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
                            } else {
                                timeStr = testDate.toLocaleDateString();
                            }
                            
                            allTests.push({
                                name: flow.shortCode || 'Unknown Flow',
                                status: 'success',
                                time: timeStr,
                                progress: 100,
                                timestamp: testDate.getTime()
                            });
                        });
                    } catch (err) {
                        console.error(`Error fetching recent tests for flow ${flow.processFlowId}:`, err);
                    }
                }
            }
            
            // Sort all tests by timestamp (most recent first)
            const sortedTests = allTests.sort((a, b) => {
                const timeA = a.timestamp || 0;
                const timeB = b.timestamp || 0;
                return timeB - timeA; // Descending order (newest first)
            });
            
            console.log('✅ Total tests collected:', sortedTests.length);
            const recentTop5 = sortedTests.slice(0, 5);
            console.log('✅ Showing last 5 recent tests');
            setRecentTests(recentTop5);
        } catch (error) {
            console.error('❌ Error fetching recent tests:', error);
            setRecentTests([]);
        }
    };

    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
                    Dashboard
                </Typography>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr 1fr' }, gap: 3 }}>
                        <StatCard
                            title="Process Flows"
                            value={totalProcessFlows}
                            icon={<AccountTreeIcon />}
                            color="#1976d2"
                            subtitle="Total flows"
                            loading={loading}
                        />
                        <StatCard
                            title="Performance Tests"
                            value={performanceTests}
                            icon={<SpeedIcon />}
                            color="#9c27b0"
                            subtitle="Last 30 days"
                            loading={loading}
                        />
                        <StatCard
                            title="Success Rate"
                            value={`${passedTests}/${passedTests + failedTests}`}
                            icon={<CheckCircleIcon />}
                            color="#2e7d32"
                            subtitle="All tests"
                            loading={loading}
                        />
                        <StatCard
                            title="Failed Tests"
                            value={failedTests}
                            icon={<ErrorIcon />}
                            color="#d32f2f"
                            subtitle="Needs attention"
                            loading={loading}
                        />
                    </Box>

                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
                        <Paper sx={{ p: 3, height: '100%' }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                Recent Performance Tests
                            </Typography>
                            {loading ? (
                                <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                                    <CircularProgress />
                                </Box>
                            ) : recentTests.length === 0 ? (
                                <Box sx={{ textAlign: 'center', p: 4 }}>
                                    <Typography variant="body2" color="text.secondary">
                                        No recent performance tests
                                    </Typography>
                                </Box>
                            ) : (
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                    {recentTests.map((test, index) => (
                                        <Box key={index}>
                                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                                <Typography variant="body1" sx={{ fontWeight: 500, fontSize: '0.9rem' }}>
                                                    {test.name}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem' }}>
                                                    {test.time}
                                                </Typography>
                                            </Box>
                                            <LinearProgress
                                                variant="determinate"
                                                value={test.progress}
                                                sx={{
                                                    height: 8,
                                                    borderRadius: 1,
                                                    backgroundColor: '#e0e0e0',
                                                    '& .MuiLinearProgress-bar': {
                                                        backgroundColor: test.status === 'success' ? '#2e7d32' : test.status === 'failed' ? '#d32f2f' : '#1976d2',
                                                    },
                                                }}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            )}
                        </Paper>
                        <Paper sx={{ p: 2.5, height: '100%', display: 'flex', flexDirection: 'column' }}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                                <Typography variant="h6" sx={{ fontWeight: 600, color: '#d32f2f', fontSize: '1rem' }}>
                                    Failed Services
                                </Typography>
                                {!loading && failedServices.length > 0 && (
                                    <Typography variant="caption" sx={{ 
                                        color: '#d32f2f', 
                                        backgroundColor: '#ffebee', 
                                        px: 1, 
                                        py: 0.3, 
                                        borderRadius: 0.5,
                                        fontSize: '0.7rem',
                                        fontWeight: 600
                                    }}>
                                        {failedServices.length}
                                    </Typography>
                                )}
                            </Box>
                            {loading ? (
                                <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                                    <CircularProgress size={32} />
                                </Box>
                            ) : failedServices.length === 0 ? (
                                <Box sx={{ textAlign: 'center', p: 3 }}>
                                    <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem' }}>
                                        No failed services ✅
                                    </Typography>
                                </Box>
                            ) : (
                                <Box sx={{ 
                                    flex: 1, 
                                    overflow: 'auto', 
                                    maxHeight: '350px',
                                    display: 'flex', 
                                    flexDirection: 'column', 
                                    gap: 0.75,
                                    pr: 0.5,
                                    '&::-webkit-scrollbar': {
                                        width: '6px',
                                    },
                                    '&::-webkit-scrollbar-track': {
                                        background: 'action.hover',
                                        borderRadius: '10px',
                                    },
                                    '&::-webkit-scrollbar-thumb': {
                                        background: 'error.main',
                                        borderRadius: '10px',
                                    },
                                    '&::-webkit-scrollbar-thumb:hover': {
                                        background: 'error.dark',
                                    }
                                }}>
                                    {failedServices.map((service, index) => (
                                        <Box
                                            key={index}
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 1,
                                                px: 1.25,
                                                py: 0.75,
                                                backgroundColor: service.status === 'broken' ? 'warning.lighter' : 'error.lighter',
                                                borderRadius: 0.75,
                                                borderLeft: `3px solid`,
                                                borderLeftColor: service.status === 'broken' ? 'warning.main' : 'error.main',
                                                transition: 'all 0.15s',
                                                '&:hover': {
                                                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                                                    transform: 'translateX(2px)'
                                                }
                                            }}
                                        >
                                            <ErrorIcon 
                                                sx={{ 
                                                    fontSize: 16, 
                                                    color: service.status === 'broken' ? 'warning.main' : 'error.main',
                                                    flexShrink: 0
                                                }} 
                                            />
                                            <Typography 
                                                variant="body2" 
                                                sx={{ 
                                                    fontSize: '0.75rem',
                                                    wordBreak: 'break-word',
                                                    flex: 1,
                                                    lineHeight: 1.3,
                                                    color: 'text.primary'
                                                }}
                                            >
                                                {service.name}
                                            </Typography>
                                        </Box>
                                    ))}
                                </Box>
                            )}
                        </Paper>
                    </Box>
                </Box>
            </Box>
        </DashboardLayout>
    );
}
