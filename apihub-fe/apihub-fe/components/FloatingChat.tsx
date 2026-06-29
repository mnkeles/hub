'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useTranslations } from 'next-intl';
import {
    Box,
    Alert,
    Paper,
    TextField,
    IconButton,
    Typography,
    Avatar,
    Tooltip,
    Fab,
    Chip,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PersonIcon from '@mui/icons-material/Person';
import CloseIcon from '@mui/icons-material/Close';
import MinimizeIcon from '@mui/icons-material/Minimize';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import StopIcon from '@mui/icons-material/Stop';
import { useStreamingChat } from '@/hooks/useStreamingChat';
import { useProject } from '@/contexts/ProjectContext';

interface FloatingChatProps {
    title?: string;
    subtitle?: string;
    suggestions?: string[];
    position?: 'bottom-right' | 'bottom-left';
    bottomOffset?: number;
    sideOffset?: number;
    infoMessage?: string;
    clearOnUnmount?: boolean;
    projectShortCode?: string;
    systemShortCode?: string;
}

export default function FloatingChat({
    title,
    subtitle,
    suggestions,
    position = 'bottom-right',
    bottomOffset = 24,
    sideOffset = 24,
    infoMessage,
    clearOnUnmount = false,
    projectShortCode,
    systemShortCode,
}: FloatingChatProps) {
    const t = useTranslations('chatUi');
    const { selectedProject } = useProject();
    const [chatOpen, setChatOpen] = useState(false);
    const [inputMessage, setInputMessage] = useState('');
    const [uploadedFile, setUploadedFile] = useState<File | null>(null);
    const [fileContent, setFileContent] = useState<string | null>(null);
    const [showSuggestions, setShowSuggestions] = useState(true);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const resolvedProjectShortCode = projectShortCode?.trim() || selectedProject?.shortCode?.trim() || undefined;
    const resolvedSystemShortCode = systemShortCode?.trim() || undefined;
    const resolvedTitle = title ?? t('floating.defaultTitle');
    const resolvedSubtitle = subtitle ?? t('floating.defaultSubtitle');
    const resolvedSuggestions = suggestions ?? [
        t('floating.defaultSuggestionSlowest'),
        t('floating.defaultSuggestionTrend'),
        t('floating.defaultSuggestionOptimize'),
    ];

    const {
        messages,
        currentAiMessage,
        currentAiReasoning,
        currentAiUsage,
        isStreaming,
        streamActivity,
        sendMessage,
        cancelStream,
        clearMessages,
    } = useStreamingChat();

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, currentAiMessage, currentAiReasoning, currentAiUsage, streamActivity]);

    const renderThinkingBlock = (
        reasoning?: string | null,
        placeholderText?: string | null
    ) => {
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
                    p: 1,
                    backgroundColor: 'rgba(255, 152, 0, 0.08)',
                    borderStyle: 'dashed',
                    borderColor: 'warning.main',
                }}
            >
                <Typography variant="caption" sx={{ fontWeight: 700, color: 'warning.dark', display: 'block', mb: 0.5 }}>
                    {sectionTitle}
                </Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', color: 'text.secondary', lineHeight: 1.5 }}>
                    {displayText}
                </Typography>
            </Paper>
        );
    };

    const renderUsageFooter = (usage?: { promptTokens?: number | null; completionTokens?: number | null; totalTokens?: number | null } | null) => {
        const hasUsage = !!usage && (
            usage.promptTokens != null ||
            usage.completionTokens != null ||
            usage.totalTokens != null
        );

        if (!hasUsage) {
            return null;
        }

        return (
            <Typography variant="caption" sx={{ display: 'block', mt: 0.75, color: 'text.secondary' }}>
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

    useEffect(() => {
        return () => {
            if (clearOnUnmount) {
                clearMessages();
                setInputMessage('');
                setUploadedFile(null);
                setFileContent(null);
                setShowSuggestions(true);
            }
        };
    }, [clearMessages, clearOnUnmount]);

    const handleSendMessage = async () => {
        if ((!inputMessage.trim() && !uploadedFile) || isStreaming) return;

        let message = inputMessage.trim();
        
        if (uploadedFile && fileContent) {
            message = `${message}\n\n[${t('labels.harFile')}: ${uploadedFile.name}]\n\`\`\`json\n${fileContent}\n\`\`\``;
        }
        
        setInputMessage('');
        setUploadedFile(null);
        setFileContent(null);
        setShowSuggestions(false);
        await sendMessage({
            message,
            projectShortCode: resolvedProjectShortCode,
            systemShortCode: resolvedSystemShortCode,
        });
    };

    const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

        if (!file.name.endsWith('.har')) {
            alert(t('errors.invalidHarFile'));
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            alert(t('errors.harFileTooLarge'));
            return;
        }

        setUploadedFile(file);

        const reader = new FileReader();
        reader.onload = (e) => {
            const content = e.target?.result as string;
            try {
                JSON.parse(content);
                setFileContent(content);
            } catch (error) {
                alert(t('errors.invalidHarFormat'));
                setUploadedFile(null);
            }
        };
        reader.readAsText(file);

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

    const handleSuggestionClick = (suggestion: string) => {
        setInputMessage(suggestion);
        setShowSuggestions(false);
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const displayMessages = messages.filter(msg => msg.role !== 'system');
    const showStreamingAssistantBubble = isStreaming && streamActivity !== 'idle';
    const streamingThinkingPlaceholder = !currentAiReasoning && !currentAiMessage ? getStreamActivityLabel() : null;

    const positionStyles = position === 'bottom-right' 
        ? { bottom: bottomOffset, right: sideOffset }
        : { bottom: bottomOffset, left: sideOffset };

    return (
        <>
            {/* Floating Chat Window */}
            {chatOpen && (
                <Paper
                    elevation={8}
                    sx={{
                        position: 'fixed',
                        ...positionStyles,
                        width: 420,
                        height: 600,
                        display: 'flex',
                        flexDirection: 'column',
                        zIndex: 1300,
                        borderRadius: 3,
                        overflow: 'hidden',
                    }}
                >
                    {/* Header */}
                    <Box
                        sx={{
                            p: 2,
                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            color: 'white',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                        }}
                    >
                        <Box display="flex" alignItems="center" gap={1.5}>
                            <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 36, height: 36 }}>
                                <SmartToyIcon fontSize="small" />
                            </Avatar>
                            <Box>
                                <Typography variant="subtitle1" fontWeight="bold">
                                    {resolvedTitle}
                                </Typography>
                                <Typography variant="caption" sx={{ opacity: 0.9 }}>
                                    {resolvedSubtitle}
                                </Typography>
                            </Box>
                        </Box>
                        <Box display="flex" gap={0.5}>
                            <Tooltip title={t('tooltips.minimize')}>
                                <IconButton size="small" onClick={() => setChatOpen(false)} sx={{ color: 'white' }}>
                                    <MinimizeIcon fontSize="small" />
                                </IconButton>
                            </Tooltip>
                            <Tooltip title={t('tooltips.close')}>
                                <IconButton size="small" onClick={() => setChatOpen(false)} sx={{ color: 'white' }}>
                                    <CloseIcon fontSize="small" />
                                </IconButton>
                            </Tooltip>
                        </Box>
                    </Box>

                    {/* Messages */}
                    <Box
                        sx={{
                            flex: 1,
                            overflow: 'auto',
                            p: 2,
                            bgcolor: '#f8f9fa',
                        }}
                    >
                        {infoMessage && (
                            <Alert severity="info" sx={{ mb: 2, borderRadius: 2 }}>
                                {infoMessage}
                            </Alert>
                        )}

                        {displayMessages.length === 0 && showSuggestions ? (
                            <Box sx={{ textAlign: 'center', py: 4 }}>
                                <SmartToyIcon sx={{ fontSize: 60, color: '#667eea', opacity: 0.5, mb: 2 }} />
                                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                                    {resolvedSubtitle}
                                </Typography>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                                    {resolvedSuggestions.map((suggestion, idx) => (
                                        <Chip
                                            key={idx}
                                            label={suggestion}
                                            onClick={() => handleSuggestionClick(suggestion)}
                                            sx={{
                                                cursor: 'pointer',
                                                bgcolor: 'white',
                                                '&:hover': { bgcolor: '#f0f4ff' },
                                            }}
                                        />
                                    ))}
                                </Box>
                            </Box>
                        ) : (
                            <Box>
                                {displayMessages.map((msg, index) => (
                                    <Box
                                        key={index}
                                        sx={{
                                            display: 'flex',
                                            justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                                            mb: 2,
                                        }}
                                    >
                                        {msg.role === 'assistant' && (
                                            <Avatar
                                                sx={{
                                                    bgcolor: '#667eea',
                                                    mr: 1,
                                                    width: 32,
                                                    height: 32,
                                                }}
                                            >
                                                <SmartToyIcon fontSize="small" />
                                            </Avatar>
                                        )}
                                        <Paper
                                            elevation={2}
                                            sx={{
                                                p: 1.5,
                                                maxWidth: '75%',
                                                backgroundColor: msg.role === 'user' ? '#667eea' : 'white',
                                                color: msg.role === 'user' ? 'white' : 'text.primary',
                                                borderRadius: 2,
                                                wordBreak: 'break-word',
                                            }}
                                        >
                                            <Box sx={{ whiteSpace: 'pre-wrap' }}>
                                                {msg.role === 'assistant' && renderThinkingBlock(msg.reasoning)}
                                                {msg.content && (
                                                    <Typography
                                                        variant="body2"
                                                        sx={{ lineHeight: 1.6 }}
                                                    >
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
                                                    width: 32,
                                                    height: 32,
                                                }}
                                            >
                                                <PersonIcon fontSize="small" />
                                            </Avatar>
                                        )}
                                    </Box>
                                ))}

                                {showStreamingAssistantBubble && (
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
                                                width: 32,
                                                height: 32,
                                            }}
                                        >
                                            <SmartToyIcon fontSize="small" />
                                        </Avatar>
                                        <Paper
                                            elevation={2}
                                            sx={{
                                                p: 1.5,
                                                maxWidth: '75%',
                                                backgroundColor: 'white',
                                                borderRadius: 2,
                                                wordBreak: 'break-word',
                                            }}
                                        >
                                            <Box sx={{ whiteSpace: 'pre-wrap' }}>
                                                {renderThinkingBlock(currentAiReasoning, streamingThinkingPlaceholder)}
                                                {currentAiMessage && (
                                                    <>
                                                        <Typography
                                                            variant="body2"
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
                                )}

                                <div ref={messagesEndRef} />
                            </Box>
                        )}
                    </Box>

                    {/* Input Area */}
                    <Box
                        sx={{
                            p: 2,
                            bgcolor: 'white',
                            borderTop: '1px solid',
                            borderColor: 'divider',
                        }}
                    >
                        {uploadedFile && (
                            <Box
                                sx={{
                                    mb: 1,
                                    p: 1,
                                    backgroundColor: '#f0f4ff',
                                    borderRadius: 1,
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                }}
                            >
                                <Box display="flex" alignItems="center" gap={1}>
                                    <InsertDriveFileIcon sx={{ color: '#667eea', fontSize: 20 }} />
                                    <Box>
                                        <Typography variant="caption" fontWeight={600}>
                                            {uploadedFile.name}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary" display="block">
                                            {(uploadedFile.size / 1024).toFixed(2)} KB
                                        </Typography>
                                    </Box>
                                </Box>
                                <IconButton size="small" onClick={handleRemoveFile} sx={{ color: 'error.main' }}>
                                    <CloseIcon fontSize="small" />
                                </IconButton>
                            </Box>
                        )}

                        <Box display="flex" gap={1} alignItems="flex-end">
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept=".har"
                                style={{ display: 'none' }}
                                onChange={handleFileSelect}
                            />
                            
                            <Tooltip title={t('tooltips.attachHarFile')}>
                                <IconButton
                                    onClick={handleAttachClick}
                                    disabled={isStreaming}
                                    size="small"
                                    sx={{
                                        color: uploadedFile ? '#667eea' : 'text.secondary',
                                        '&:hover': {
                                            backgroundColor: 'rgba(102, 126, 234, 0.1)',
                                        },
                                    }}
                                >
                                    <AttachFileIcon fontSize="small" />
                                </IconButton>
                            </Tooltip>

                            <TextField
                                fullWidth
                                size="small"
                                multiline
                                maxRows={3}
                                value={inputMessage}
                                onChange={(e) => setInputMessage(e.target.value)}
                                onKeyPress={handleKeyPress}
                                placeholder={t('prompts.chatInput')}
                                disabled={isStreaming}
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: 2,
                                    },
                                }}
                            />

                            {isStreaming ? (
                                <Tooltip title={t('tooltips.stopResponse')}>
                                    <IconButton
                                        onClick={cancelStream}
                                        sx={{
                                            bgcolor: '#dc3545',
                                            color: 'white',
                                            width: 40,
                                            height: 40,
                                            '&:hover': {
                                                bgcolor: '#c82333',
                                            },
                                        }}
                                    >
                                        <StopIcon fontSize="small" />
                                    </IconButton>
                                </Tooltip>
                            ) : (
                                <IconButton
                                    onClick={handleSendMessage}
                                    disabled={isStreaming || (!inputMessage.trim() && !uploadedFile)}
                                    sx={{
                                        bgcolor: '#667eea',
                                        color: 'white',
                                        width: 40,
                                        height: 40,
                                        '&:hover': {
                                            bgcolor: '#764ba2',
                                        },
                                        '&:disabled': {
                                            bgcolor: '#e0e0e0',
                                        },
                                    }}
                                >
                                    <SendIcon fontSize="small" />
                                </IconButton>
                            )}
                        </Box>
                    </Box>
                </Paper>
            )}

            {/* Floating Action Button */}
            {!chatOpen && (
                <Tooltip title={resolvedTitle}>
                    <Fab
                        color="primary"
                        onClick={() => setChatOpen(true)}
                        sx={{
                            position: 'fixed',
                            ...positionStyles,
                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            '&:hover': {
                                background: 'linear-gradient(135deg, #764ba2 0%, #667eea 100%)',
                            },
                        }}
                    >
                        <ChatBubbleOutlineIcon />
                    </Fab>
                </Tooltip>
            )}
        </>
    );
}
