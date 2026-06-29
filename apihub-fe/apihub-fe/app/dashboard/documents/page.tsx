'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    TextField,
    Button,
    List,
    ListItem,
    ListItemText,
    ListItemButton,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    CircularProgress,
    Divider,
    Chip,
    Alert,
    Snackbar,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SearchIcon from '@mui/icons-material/Search';
import SaveIcon from '@mui/icons-material/Save';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import DashboardLayout from '@/components/DashboardLayout';
import { documentService, Document, DocumentCreateRequest } from '@/services/documentService';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export default function DocumentsPage() {
    const [documents, setDocuments] = useState<Document[]>([]);
    const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [searchMode, setSearchMode] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<Document[]>([]);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [documentToDelete, setDocumentToDelete] = useState<string | null>(null);
    
    const [formData, setFormData] = useState<DocumentCreateRequest>({
        content: '',
        title: '',
        category: '',
    });
    
    const [snackbar, setSnackbar] = useState({
        open: false,
        message: '',
        severity: 'success' as 'success' | 'error',
    });

    const [viewMode, setViewMode] = useState<'split' | 'edit' | 'preview'>('split');
    const [showEditor, setShowEditor] = useState(false);

    useEffect(() => {
        loadDocuments();
    }, []);

    const loadDocuments = async () => {
        setLoading(true);
        try {
            const data = await documentService.getAllDocuments();
            setDocuments(data);
            setSearchMode(false);
        } catch (error) {
            console.error('Error loading documents:', error);
            showSnackbar('Failed to load documents', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleDocumentSelect = async (doc: Document) => {
        setLoading(true);
        try {
            const fullDocument = await documentService.getDocumentById(doc.id);
            setSelectedDocument(fullDocument);
            setFormData({
                content: fullDocument.content || '',
                title: fullDocument.title || '',
                category: fullDocument.category || '',
            });
            setViewMode('split');
            setShowEditor(true);
        } catch (error) {
            console.error('Error loading document:', error);
            showSnackbar('Failed to load document', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleNewDocument = () => {
        setSelectedDocument(null);
        setFormData({
            content: '',
            title: '',
            category: '',
        });
        setViewMode('split');
        setShowEditor(true);
    };

    const handleSave = async () => {
        if (!formData.title || !formData.content) {
            showSnackbar('Title and content are required', 'error');
            return;
        }

        setSaving(true);
        try {
            if (selectedDocument) {
                await documentService.updateDocument(selectedDocument.id, formData);
                showSnackbar('Document updated successfully', 'success');
            } else {
                await documentService.createDocument(formData);
                showSnackbar('Document created successfully', 'success');
            }
            await loadDocuments();
        } catch (error) {
            console.error('Error saving document:', error);
            showSnackbar('Failed to save document', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteClick = (id: string) => {
        setDocumentToDelete(id);
        setDeleteDialogOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (!documentToDelete) return;

        try {
            await documentService.deleteDocument(documentToDelete);
            showSnackbar('Document deleted successfully', 'success');
            if (selectedDocument?.id === documentToDelete) {
                setSelectedDocument(null);
                setFormData({ content: '', title: '', category: '' });
            }
            await loadDocuments();
        } catch (error) {
            console.error('Error deleting document:', error);
            showSnackbar('Failed to delete document', 'error');
        } finally {
            setDeleteDialogOpen(false);
            setDocumentToDelete(null);
        }
    };

    const handleSearch = async () => {
        if (!searchQuery.trim()) {
            showSnackbar('Please enter a search query', 'error');
            return;
        }

        setLoading(true);
        try {
            const results = await documentService.searchDocuments(searchQuery, 10);
            setSearchResults(results);
            setSearchMode(true);
        } catch (error) {
            console.error('Error searching documents:', error);
            showSnackbar('Failed to search documents', 'error');
        } finally {
            setLoading(false);
        }
    };

    const showSnackbar = (message: string, severity: 'success' | 'error') => {
        setSnackbar({ open: true, message, severity });
    };

    const displayList = searchMode ? searchResults : documents;

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                        Documentation
                    </Typography>
                    <Button
                        variant="contained"
                        startIcon={<AddIcon />}
                        onClick={handleNewDocument}
                        sx={{ borderRadius: 2 }}
                    >
                        New Document
                    </Button>
                </Box>

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '300px 1fr' }, gap: 3, height: 'calc(100vh - 200px)' }}>
                    {/* Left Sidebar - Document List */}
                    <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        <Box sx={{ mb: 2 }}>
                            <TextField
                                fullWidth
                                size="small"
                                placeholder="Search documents..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                                InputProps={{
                                    endAdornment: (
                                        <IconButton size="small" onClick={handleSearch}>
                                            <SearchIcon />
                                        </IconButton>
                                    ),
                                }}
                            />
                            {searchMode && (
                                <Button
                                    size="small"
                                    onClick={loadDocuments}
                                    sx={{ mt: 1 }}
                                >
                                    Clear Search
                                </Button>
                            )}
                        </Box>
                        <Divider sx={{ mb: 1 }} />
                        {loading ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                                <CircularProgress />
                            </Box>
                        ) : displayList.length === 0 ? (
                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', p: 4 }}>
                                {searchMode ? 'No results found' : 'No documents yet'}
                            </Typography>
                        ) : (
                            <List sx={{ flex: 1, overflow: 'auto' }}>
                                {displayList.map((doc) => (
                                    <ListItem
                                        key={doc.id}
                                        disablePadding
                                        secondaryAction={
                                            <IconButton
                                                edge="end"
                                                size="small"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleDeleteClick(doc.id);
                                                }}
                                            >
                                                <DeleteIcon fontSize="small" />
                                            </IconButton>
                                        }
                                    >
                                        <ListItemButton
                                            selected={selectedDocument?.id === doc.id}
                                            onClick={() => handleDocumentSelect(doc)}
                                        >
                                            <ListItemText
                                                primary={doc.title || 'Untitled'}
                                                secondary={doc.category}
                                                primaryTypographyProps={{
                                                    sx: { fontWeight: 500, fontSize: '0.9rem' }
                                                }}
                                                secondaryTypographyProps={{
                                                    sx: { fontSize: '0.75rem' }
                                                }}
                                            />
                                        </ListItemButton>
                                    </ListItem>
                                ))}
                            </List>
                        )}
                    </Paper>

                    {/* Right Side - Editor and Preview */}
                    <Paper sx={{ p: 3, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        {showEditor ? (
                            <>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                                    <Box sx={{ display: 'flex', gap: 1 }}>
                                        <Chip
                                            label="Split"
                                            onClick={() => setViewMode('split')}
                                            color={viewMode === 'split' ? 'primary' : 'default'}
                                            size="small"
                                        />
                                        <Chip
                                            label="Edit"
                                            icon={<EditIcon />}
                                            onClick={() => setViewMode('edit')}
                                            color={viewMode === 'edit' ? 'primary' : 'default'}
                                            size="small"
                                        />
                                        <Chip
                                            label="Preview"
                                            icon={<VisibilityIcon />}
                                            onClick={() => setViewMode('preview')}
                                            color={viewMode === 'preview' ? 'primary' : 'default'}
                                            size="small"
                                        />
                                    </Box>
                                    <Button
                                        variant="contained"
                                        startIcon={saving ? <CircularProgress size={16} /> : <SaveIcon />}
                                        onClick={handleSave}
                                        disabled={saving}
                                    >
                                        {saving ? 'Saving...' : 'Save'}
                                    </Button>
                                </Box>

                                <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                                    <TextField
                                        label="Title"
                                        value={formData.title}
                                        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                                        size="small"
                                        fullWidth
                                    />
                                    <TextField
                                        label="Category"
                                        value={formData.category}
                                        onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                                        size="small"
                                        sx={{ minWidth: 200 }}
                                    />
                                </Box>

                                <Box sx={{ 
                                    display: 'grid', 
                                    gridTemplateColumns: viewMode === 'split' ? '1fr 1fr' : '1fr',
                                    gap: 2, 
                                    flex: 1, 
                                    overflow: 'hidden' 
                                }}>
                                    {/* Editor */}
                                    {(viewMode === 'split' || viewMode === 'edit') && (
                                        <Box sx={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                                            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                                                Markdown Editor
                                            </Typography>
                                            <TextField
                                                multiline
                                                value={formData.content}
                                                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                                                placeholder="Write your markdown content here..."
                                                sx={{
                                                    flex: 1,
                                                    '& .MuiInputBase-root': {
                                                        height: '100%',
                                                        alignItems: 'flex-start',
                                                        fontFamily: 'monospace',
                                                        fontSize: '0.9rem',
                                                    },
                                                    '& textarea': {
                                                        height: '100% !important',
                                                        overflow: 'auto !important',
                                                    },
                                                }}
                                            />
                                        </Box>
                                    )}

                                    {/* Preview */}
                                    {(viewMode === 'split' || viewMode === 'preview') && (
                                        <Box sx={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                                            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                                                Preview
                                            </Typography>
                                            <Paper
                                                variant="outlined"
                                                sx={{
                                                    flex: 1,
                                                    p: 2,
                                                    overflow: 'auto',
                                                    backgroundColor: 'background.default',
                                                }}
                                            >
                                                <Box sx={{ 
                                                    '& h1': { fontSize: '2rem', fontWeight: 700, mb: 2 },
                                                    '& h2': { fontSize: '1.5rem', fontWeight: 600, mb: 1.5, mt: 2 },
                                                    '& h3': { fontSize: '1.25rem', fontWeight: 600, mb: 1, mt: 1.5 },
                                                    '& p': { mb: 1.5, lineHeight: 1.7 },
                                                    '& code': { 
                                                        backgroundColor: 'action.hover', 
                                                        padding: '2px 6px', 
                                                        borderRadius: '4px',
                                                        fontFamily: 'monospace',
                                                        fontSize: '0.875rem',
                                                    },
                                                    '& pre': { 
                                                        backgroundColor: 'action.hover', 
                                                        p: 2, 
                                                        borderRadius: 1, 
                                                        overflow: 'auto',
                                                        mb: 2,
                                                    },
                                                    '& ul, & ol': { mb: 1.5, pl: 3 },
                                                    '& li': { mb: 0.5 },
                                                    '& blockquote': { 
                                                        borderLeft: '4px solid', 
                                                        borderColor: 'primary.main',
                                                        pl: 2, 
                                                        ml: 0,
                                                        fontStyle: 'italic',
                                                        color: 'text.secondary',
                                                    },
                                                    '& table': { 
                                                        borderCollapse: 'collapse', 
                                                        width: '100%', 
                                                        mb: 2 
                                                    },
                                                    '& th, & td': { 
                                                        border: '1px solid', 
                                                        borderColor: 'divider',
                                                        p: 1,
                                                        textAlign: 'left',
                                                    },
                                                    '& th': { 
                                                        backgroundColor: 'action.hover',
                                                        fontWeight: 600,
                                                    },
                                                }}>
                                                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                                        {formData.content || '*No content yet*'}
                                                    </ReactMarkdown>
                                                </Box>
                                            </Paper>
                                        </Box>
                                    )}
                                </Box>
                            </>
                        ) : (
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                <Typography variant="body1" color="text.secondary">
                                    Select a document or create a new one to get started
                                </Typography>
                            </Box>
                        )}
                    </Paper>
                </Box>
            </Box>

            {/* Delete Confirmation Dialog */}
            <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
                <DialogTitle>Delete Document</DialogTitle>
                <DialogContent>
                    <Typography>
                        Are you sure you want to delete this document? This action cannot be undone.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
                    <Button onClick={handleDeleteConfirm} color="error" variant="contained">
                        Delete
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Snackbar for notifications */}
            <Snackbar
                open={snackbar.open}
                autoHideDuration={4000}
                onClose={() => setSnackbar({ ...snackbar, open: false })}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert
                    onClose={() => setSnackbar({ ...snackbar, open: false })}
                    severity={snackbar.severity}
                    sx={{ width: '100%' }}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </DashboardLayout>
    );
}
