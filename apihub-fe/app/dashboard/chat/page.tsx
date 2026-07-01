'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useTranslations } from 'next-intl';
import {
    Box,
    Paper,
    TextField,
    IconButton,
    Typography,
    Avatar,
    CircularProgress,
    Tooltip,
    Fade,
    Button,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PersonIcon from '@mui/icons-material/Person';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import RefreshIcon from '@mui/icons-material/Refresh';
import StopIcon from '@mui/icons-material/Stop';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import CloseIcon from '@mui/icons-material/Close';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import DashboardLayout from '@/components/DashboardLayout';
import { useProject } from '@/contexts/ProjectContext';
import { useStreamingChat } from '@/hooks/useStreamingChat';

export default function ChatPage() {
    const t = useTranslations('chatUi');
    const { selectedProject } = useProject();
    const [inputMessage, setInputMessage] = useState('');
    const [initialLoading, setInitialLoading] = useState(true);
    const [uploadedFile, setUploadedFile] = useState<File | null>(null);
    const [fileContent, setFileContent] = useState<string | null>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const {
        messages,
        currentAiMessage,
        currentAiReasoning,
        currentAiUsage,
        isStreaming,
        streamActivity,
        sendMessage,
        cancelStream,
        loadHistory,
        clearHistory: clearChatHistory,
    } = useStreamingChat();

    // Otomatik scroll
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, currentAiMessage, currentAiReasoning, currentAiUsage, streamActivity]);

    type TokenUsage = {
        promptTokens?: number | null;
        completionTokens?: number | null;
        totalTokens?: number | null;
    };

    const renderThinkingBlock = (reasoning?: string | null, placeholderText?: string | null) => {
        const normalizedReasoning = reasoning?.trim() || '';
        const displayText = normalizedReasoning || placeholderText || '';
        const sectionTitle = normalizedReasoning ? t('labels.thinking') : t('labels.processing');

        if (!displayText) {
            return null;
        }

        return (
            <Paper
                variant="outlined"
                sx={{
                    mt: 1,
                    p: 1.25,
                    backgroundColor: 'rgba(255, 152, 0, 0.08)',
                    borderStyle: 'dashed',
                    borderColor: 'warning.main',
                }}
            >
                <Typography variant="caption" sx={{ fontWeight: 700, color: 'warning.dark', display: 'block', mb: 0.75 }}>
                    {sectionTitle}
                </Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', color: 'text.secondary', lineHeight: 1.5 }}>
                    {displayText}
                </Typography>
            </Paper>
        );
    };

    const renderUsageFooter = (usage?: TokenUsage | null) => {
        const hasUsage = !!usage && (
            usage.promptTokens != null ||
            usage.completionTokens != null ||
            usage.totalTokens != null
        );

        if (!hasUsage) {
            return null;
        }

        return (
            <Typography variant="caption" sx={{ display: 'block', mt: 1, color: 'text.secondary' }}>
                {t('labels.tokens')}: P {usage?.promptTokens ?? '-'} | C {usage?.completionTokens ?? '-'} | T {usage?.totalTokens ?? '-'}
            </Typography>
        );
    };

    const getStreamActivityLabel = () => {
        switch (streamActivity) {
            case 'submitting':
                return t('activity.submitting');
            case 'waiting':
                return t('activity.waiting');
            case 'thinking':
                return t('activity.thinking');
            case 'responding':
                return t('activity.responding');
            default:
                return null;
        }
    };

    // Geçmişi yükle
    useEffect(() => {
        const loadInitialHistory = async () => {
            await loadHistory();
            setInitialLoading(false);
        };
        loadInitialHistory();
    }, [loadHistory]);

    const handleSendMessage = async () => {
        if ((!inputMessage.trim() && !uploadedFile) || isStreaming) return;

        let message = inputMessage.trim();
        
        // If there's a file, include it in the message
        if (uploadedFile && fileContent) {
            message = `${message}\n\n[${t('labels.harFile')}: ${uploadedFile.name}]\n\`\`\`json\n${fileContent}\n\`\`\``;
        }
        
        setInputMessage('');
        setUploadedFile(null);
        setFileContent(null);
        await sendMessage({
            message,
            projectShortCode: selectedProject?.shortCode?.trim() || undefined,
        });
    };

    const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

        // Validate file type
        if (!file.name.endsWith('.har')) {
            alert(t('errors.invalidHarFile'));
            return;
        }

        // Validate file size (max 10MB)
        if (file.size > 10 * 1024 * 1024) {
            alert(t('errors.harFileTooLarge'));
            return;
        }

        setUploadedFile(file);

        // Read file content
        const reader = new FileReader();
        reader.onload = (e) => {
            const content = e.target?.result as string;
            try {
                // Validate JSON format
                JSON.parse(content);
                setFileContent(content);
            } catch (error) {
                alert(t('errors.invalidHarFormat'));
                setUploadedFile(null);
            }
        };
        reader.readAsText(file);

        // Reset input
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleRemoveFile = () => {
        setUploadedFile(null);
        setFileContent(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleAttachClick = () => {
        fileInputRef.current?.click();
    };

    const handleClearHistory = async () => {
        if (!confirm(t('confirmations.clearHistory'))) return;
        await clearChatHistory();
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    // System mesajlarını filtrele
    const displayMessages = messages.filter(msg => msg.role !== 'system');
    const showStreamingAssistantBubble = isStreaming && streamActivity !== 'idle';
    const streamingThinkingPlaceholder = !currentAiReasoning && !currentAiMessage ? getStreamActivityLabel() : null;

    if (initialLoading) {
        return (
            <DashboardLayout>
                <Box display="flex" justifyContent="center" alignItems="center" minHeight="80vh">
                    <CircularProgress />
                </Box>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout>
            <Box sx={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
            {/* Header */}
            <Paper
                elevation={2}
                sx={{
                    p: 2,
                    mb: 2,
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                }}
            >
                <Box display="flex" justifyContent="space-between" alignItems="center">
                    <Box display="flex" alignItems="center" gap={2}>
                        <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)' }}>
                            <SmartToyIcon />
                        </Avatar>
                        <Box>
                            <Typography variant="h5" fontWeight="bold">
                                {t('main.title')}
                            </Typography>
                            <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                {t('main.subtitle')}
                            </Typography>
                        </Box>
                    </Box>
                    <Box display="flex" gap={1}>
                        <Tooltip title={t('tooltips.refreshHistory')}>
                            <IconButton onClick={loadHistory} sx={{ color: 'white' }}>
                                <RefreshIcon />
                            </IconButton>
                        </Tooltip>
                        <Tooltip title={t('tooltips.clearHistory')}>
                            <IconButton onClick={handleClearHistory} sx={{ color: 'white' }}>
                                <DeleteOutlineIcon />
                            </IconButton>
                        </Tooltip>
                    </Box>
                </Box>
            </Paper>

            {/* Messages Container */}
            <Paper
                elevation={1}
                sx={{
                    flex: 1,
                    overflow: 'auto',
                    p: 3,
                    backgroundColor: '#f5f7fa',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                {displayMessages.length === 0 && !showStreamingAssistantBubble ? (
                    <Box
                        display="flex"
                        flexDirection="column"
                        alignItems="center"
                        justifyContent="center"
                        height="100%"
                        gap={2}
                    >
                        <SmartToyIcon sx={{ fontSize: 80, color: '#667eea', opacity: 0.5 }} />
                        <Typography variant="h6" color="text.secondary">
                            {t('main.emptyTitle')}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {t('main.emptySubtitle')}
                        </Typography>
                    </Box>
                ) : (
                    <Box>
                        {displayMessages.map((msg, index) => (
                            <Fade in key={index} timeout={300}>
                                <Box
                                    sx={{
                                        display: 'flex',
                                        justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                                        alignItems: 'flex-start',
                                        mb: 2,
                                    }}
                                >
                                    {msg.role === 'assistant' && (
                                        <Avatar
                                            sx={{
                                                bgcolor: '#667eea',
                                                mr: 1,
                                                width: 36,
                                                height: 36,
                                            }}
                                        >
                                            <SmartToyIcon fontSize="small" />
                                        </Avatar>
                                    )}
                                    <Paper
                                        elevation={2}
                                        sx={{
                                            p: 2,
                                            maxWidth: '70%',
                                            backgroundColor: msg.role === 'user' ? '#667eea' : 'white',
                                            color: msg.role === 'user' ? 'white' : 'text.primary',
                                            borderRadius: 2,
                                            wordBreak: 'break-word',
                                        }}
                                    >
                                        <Box sx={{ whiteSpace: 'pre-wrap' }}>
                                            {msg.role === 'assistant' && renderThinkingBlock(msg.reasoning)}
                                            {msg.content && (
                                                <Typography variant="body1" sx={{ lineHeight: 1.6 }}>
                                                    {msg.content}
                                                </Typography>
                                            )}
                                            {msg.role === 'assistant' && renderUsageFooter({
                                                promptTokens: msg.promptTokens,
                                                completionTokens: msg.completionTokens,
                                                totalTokens: msg.totalTokens,
                                            })}
                                        </Box>
                                    </Paper>
                                    {msg.role === 'user' && (
                                        <Avatar
                                            sx={{
                                                bgcolor: '#764ba2',
                                                ml: 1,
                                                width: 36,
                                                height: 36,
                                            }}
                                        >
                                            <PersonIcon fontSize="small" />
                                        </Avatar>
                                    )}
                                </Box>
                            </Fade>
                        ))}
                        
                        {/* Streaming mesajı göster */}
                        {showStreamingAssistantBubble && (
                            <Fade in timeout={300}>
                                <Box
                                    sx={{
                                        display: 'flex',
                                        justifyContent: 'flex-start',
                                        mb: 2,
                                    }}
                                >
                                    <Avatar
                                        sx={{
                                            bgcolor: '#667eea',
                                            mr: 1,
                                            width: 36,
                                            height: 36,
                                        }}
                                    >
                                        <SmartToyIcon fontSize="small" />
                                    </Avatar>
                                    <Paper
                                        elevation={2}
                                        sx={{
                                            p: 2,
                                            maxWidth: '70%',
                                            backgroundColor: 'white',
                                            borderRadius: 2,
                                            wordBreak: 'break-word',
                                            position: 'relative',
                                        }}
                                    >
                                        <Box sx={{ whiteSpace: 'pre-wrap' }}>
                                            {renderThinkingBlock(currentAiReasoning, streamingThinkingPlaceholder)}
                                            {currentAiMessage && (
                                                <>
                                                    <Typography
                                                        variant="body1"
                                                        sx={{ lineHeight: 1.6 }}
                                                    >
                                                        {currentAiMessage}
                                                    </Typography>
                                                    <Box
                                                        component="span"
                                                        sx={{
                                                            display: 'inline-block',
                                                            width: '2px',
                                                            height: '1em',
                                                            backgroundColor: '#667eea',
                                                            marginLeft: '2px',
                                                            animation: 'blink 1s infinite',
                                                            '@keyframes blink': {
                                                                '0%, 49%': { opacity: 1 },
                                                                '50%, 100%': { opacity: 0 },
                                                            },
                                                        }}
                                                    />
                                                </>
                                            )}
                                            {renderUsageFooter(currentAiUsage)}
                                        </Box>
                                    </Paper>
                                </Box>
                            </Fade>
                        )}
                        
                        <div ref={messagesEndRef} />
                    </Box>
                )}
            </Paper>

            {/* Input Area */}
            <Paper
                elevation={3}
                sx={{
                    p: 2,
                    mt: 2,
                    backgroundColor: 'white',
                }}
            >
                {/* File Upload Preview */}
                {uploadedFile && (
                    <Box
                        sx={{
                            mb: 1,
                            p: 1.5,
                            backgroundColor: '#f0f4ff',
                            borderRadius: 1,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                        }}
                    >
                        <Box display="flex" alignItems="center" gap={1}>
                            <InsertDriveFileIcon sx={{ color: '#667eea' }} />
                            <Box>
                                <Typography variant="body2" fontWeight={600}>
                                    {uploadedFile.name}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {(uploadedFile.size / 1024).toFixed(2)} KB
                                </Typography>
                            </Box>
                        </Box>
                        <IconButton
                            size="small"
                            onClick={handleRemoveFile}
                            sx={{ color: 'error.main' }}
                        >
                            <CloseIcon fontSize="small" />
                        </IconButton>
                    </Box>
                )}

                <Box display="flex" gap={1} alignItems="flex-end">
                    {/* Hidden file input */}
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept=".har"
                        style={{ display: 'none' }}
                        onChange={handleFileSelect}
                    />
                    
                    {/* Attach button */}
                    <Tooltip title={t('tooltips.attachHarFile')}>
                        <IconButton
                            onClick={handleAttachClick}
                            disabled={isStreaming}
                            sx={{
                                color: uploadedFile ? '#667eea' : 'text.secondary',
                                '&:hover': {
                                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                                },
                            }}
                        >
                            <AttachFileIcon />
                        </IconButton>
                    </Tooltip>
                    <TextField
                        fullWidth
                        multiline
                        maxRows={4}
                        value={inputMessage}
                        onChange={(e) => setInputMessage(e.target.value)}
                        onKeyPress={handleKeyPress}
                        placeholder={t('prompts.chatInputWithEnter')}
                        disabled={isStreaming}
                        variant="outlined"
                        sx={{
                            '& .MuiOutlinedInput-root': {
                                borderRadius: 2,
                            },
                        }}
                    />
                    {isStreaming ? (
                        <Tooltip title={t('tooltips.stopResponse')}>
                            <IconButton
                                color="error"
                                onClick={cancelStream}
                                sx={{
                                    bgcolor: '#dc3545',
                                    color: 'white',
                                    width: 48,
                                    height: 48,
                                    '&:hover': {
                                        bgcolor: '#c82333',
                                    },
                                }}
                            >
                                <StopIcon />
                            </IconButton>
                        </Tooltip>
                    ) : (
                        <IconButton
                            color="primary"
                            onClick={handleSendMessage}
                            disabled={isStreaming || (!inputMessage.trim() && !uploadedFile)}
                            sx={{
                                bgcolor: '#667eea',
                                color: 'white',
                                width: 48,
                                height: 48,
                                '&:hover': {
                                    bgcolor: '#764ba2',
                                },
                                '&:disabled': {
                                    bgcolor: '#e0e0e0',
                                },
                            }}
                        >
                            <SendIcon />
                        </IconButton>
                    )}
                </Box>
            </Paper>
            </Box>
        </DashboardLayout>
    );
}
