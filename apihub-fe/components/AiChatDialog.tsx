'use client';
import { useState, useEffect, useRef } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    TextField,
    IconButton,
    Box,
    Typography,
    Paper,
    Fab,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import DeleteIcon from '@mui/icons-material/Delete';
import SettingsIcon from '@mui/icons-material/Settings';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface Props {
    open: boolean;
    onClose: () => void;
    projectShortCode: string;
}

interface Message {
    role: 'user' | 'assistant';
    content: string;
}

export default function AiChatDialog({ open, onClose, projectShortCode }: Props) {
    const [messages, setMessages] = useState<Message[]>([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const handleSend = async () => {
        if (!input.trim()) return;

        const userMessage: Message = { role: 'user', content: input };
        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setLoading(true);

        try {
            // API çağrısı yapılacak
            // const response = await fetch('/api/chat', { ... });

            // Geçici mock response
            setTimeout(() => {
                const botMessage: Message = {
                    role: 'assistant',
                    content: 'Bu bir test yanıtıdır. OpenAI entegrasyonu eklenecek.',
                };
                setMessages(prev => [...prev, botMessage]);
                setLoading(false);
            }, 1000);
        } catch (error) {
            console.error('Chat error:', error);
            setLoading(false);
        }
    };

    const handleClear = () => {
        setMessages([]);
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="sm"
            fullWidth
            PaperProps={{
                sx: { height: '600px', display: 'flex', flexDirection: 'column' }
            }}
        >
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6">AI Asistan</Typography>
                <Box>
                    <IconButton size="small" color="primary">
                        <SettingsIcon />
                    </IconButton>
                    <IconButton size="small" color="error" onClick={handleClear}>
                        <DeleteIcon />
                    </IconButton>
                    <IconButton size="small" onClick={onClose}>
                        <CloseIcon />
                    </IconButton>
                </Box>
            </DialogTitle>

            <DialogContent sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
                <Box sx={{ flex: 1, overflow: 'auto', mb: 2 }}>
                    {messages.map((message, index) => (
                        <Paper
                            key={index}
                            sx={{
                                p: 2,
                                mb: 1,
                                maxWidth: message.role === 'assistant' ? '90%' : '70%',
                                alignSelf: message.role === 'user' ? 'flex-end' : 'flex-start',
                                bgcolor: message.role === 'user' ? 'primary.main' : 'grey.200',
                                color: message.role === 'user' ? 'white' : 'text.primary',
                                ml: message.role === 'user' ? 'auto' : 0,
                                '& h2': {
                                    fontSize: '1.25rem',
                                    fontWeight: 700,
                                    mt: 2,
                                    mb: 1,
                                },
                                '& h3': {
                                    fontSize: '1.1rem',
                                    fontWeight: 600,
                                    mt: 1.5,
                                    mb: 0.75,
                                },
                                '& table': {
                                    width: '100%',
                                    borderCollapse: 'collapse',
                                    mt: 1,
                                    mb: 1,
                                    fontSize: '0.875rem',
                                },
                                '& th, & td': {
                                    border: '1px solid #ddd',
                                    padding: '8px',
                                    textAlign: 'left',
                                },
                                '& th': {
                                    backgroundColor: '#f5f5f5',
                                    fontWeight: 600,
                                },
                                '& ul, & ol': {
                                    pl: 2,
                                    my: 1,
                                },
                                '& li': {
                                    mb: 0.5,
                                },
                                '& p': {
                                    my: 0.5,
                                },
                                '& code': {
                                    backgroundColor: 'rgba(0,0,0,0.05)',
                                    padding: '2px 4px',
                                    borderRadius: '3px',
                                    fontSize: '0.875em',
                                },
                            }}
                        >
                            {message.role === 'assistant' ? (
                                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                    {message.content}
                                </ReactMarkdown>
                            ) : (
                                <Typography variant="body2">{message.content}</Typography>
                            )}
                        </Paper>
                    ))}
                    {loading && (
                        <Paper sx={{ p: 2, maxWidth: '70%', bgcolor: 'grey.200' }}>
                            <Typography variant="body2">Yazıyor...</Typography>
                        </Paper>
                    )}
                    <div ref={messagesEndRef} />
                </Box>

                <Box sx={{ display: 'flex', gap: 1 }}>
                    <TextField
                        fullWidth
                        size="small"
                        placeholder="Mesajınızı yazın..."
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                        disabled={loading}
                    />
                    <IconButton color="primary" onClick={handleSend} disabled={loading || !input.trim()}>
                        <SendIcon />
                    </IconButton>
                </Box>
            </DialogContent>
        </Dialog>
    );
}