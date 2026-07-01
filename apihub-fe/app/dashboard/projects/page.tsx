'use client';

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Alert,
    Chip,
    CircularProgress,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import FolderIcon from '@mui/icons-material/Folder';
import DashboardLayout from '@/components/DashboardLayout';
import { projectService } from '@/services/projectService';
import { ProjectDto } from '@/types/api';
import { getErrorMessage } from '@/lib/errorUtils';

export default function ProjectsPage() {
    const [projects, setProjects] = useState<ProjectDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingProject, setEditingProject] = useState<ProjectDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const [formData, setFormData] = useState<ProjectDto>({
        projectId: null,
        name: '',
        description: '',
        shortCode: '',
    });

    useEffect(() => {
        fetchProjects();
    }, []);

    const fetchProjects = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await projectService.getAll();
            setProjects(data);
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to fetch projects'));
        } finally {
            setLoading(false);
        }
    };

    const handleOpenDialog = (project?: ProjectDto) => {
        if (project) {
            setEditingProject(project);
            setFormData(project);
        } else {
            setEditingProject(null);
            setFormData({
                projectId: null,
                name: '',
                description: '',
                shortCode: '',
            });
        }
        setOpenDialog(true);
        setError(null);
        setSuccess(null);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingProject(null);
        setFormData({
            projectId: null,
            name: '',
            description: '',
            shortCode: '',
        });
    };

    const handleSave = async () => {
        try {
            setLoading(true);
            setError(null);

            if (editingProject) {
                await projectService.update(formData);
                setSuccess('Project updated successfully');
            } else {
                await projectService.save(formData);
                setSuccess('Project created successfully');
            }

            handleCloseDialog();
            fetchProjects();
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to save project'));
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (projectId: number) => {
        if (!confirm('Are you sure you want to delete this project?')) {
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await projectService.delete(projectId);
            setSuccess('Project deleted successfully');
            fetchProjects();
        } catch (err) {
            setError(getErrorMessage(err, 'Failed to delete project'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <FolderIcon sx={{ fontSize: 40, color: 'primary.main' }} />
                        <Typography variant="h4" sx={{ fontWeight: 700 }}>
                            Projects
                        </Typography>
                    </Box>
                    <Button
                        variant="contained"
                        startIcon={<AddIcon />}
                        onClick={() => handleOpenDialog()}
                    >
                        Add Project
                    </Button>
                </Box>

                {error && (
                    <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                        {error}
                    </Alert>
                )}

                {success && (
                    <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
                        {success}
                    </Alert>
                )}

                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>ID</TableCell>
                                <TableCell>Short Code</TableCell>
                                <TableCell>Name</TableCell>
                                <TableCell>Description</TableCell>
                                <TableCell>Systems</TableCell>
                                <TableCell align="right">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {loading ? (
                                <TableRow>
                                    <TableCell colSpan={6} align="center">
                                        <CircularProgress />
                                    </TableCell>
                                </TableRow>
                            ) : projects.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} align="center">
                                        No projects found
                                    </TableCell>
                                </TableRow>
                            ) : (
                                projects.map((project) => (
                                    <TableRow key={project.projectId}>
                                        <TableCell>{project.projectId}</TableCell>
                                        <TableCell>
                                            <Chip label={project.shortCode} color="primary" size="small" />
                                        </TableCell>
                                        <TableCell>{project.name}</TableCell>
                                        <TableCell>{project.description}</TableCell>
                                        <TableCell>
                                            {project.generalWebSystemDtoList?.length || 0} systems
                                        </TableCell>
                                        <TableCell align="right">
                                            <IconButton
                                                color="primary"
                                                onClick={() => handleOpenDialog(project)}
                                            >
                                                <EditIcon />
                                            </IconButton>
                                            <IconButton
                                                color="error"
                                                onClick={() => handleDelete(project.projectId!)}
                                            >
                                                <DeleteIcon />
                                            </IconButton>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>

                {/* Add/Edit Dialog */}
                <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                    <DialogTitle>
                        {editingProject ? 'Edit Project' : 'Add New Project'}
                    </DialogTitle>
                    <DialogContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
                            <TextField
                                label="Short Code"
                                fullWidth
                                value={formData.shortCode}
                                onChange={(e) => setFormData({ ...formData, shortCode: e.target.value })}
                                required
                            />
                            <TextField
                                label="Name"
                                fullWidth
                                value={formData.name}
                                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                required
                            />
                            <TextField
                                label="Description"
                                fullWidth
                                multiline
                                rows={3}
                                value={formData.description}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            />
                        </Box>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseDialog}>Cancel</Button>
                        <Button
                            variant="contained"
                            onClick={handleSave}
                            disabled={loading || !formData.shortCode || !formData.name}
                        >
                            {loading ? 'Saving...' : 'Save'}
                        </Button>
                    </DialogActions>
                </Dialog>
            </Box>
        </DashboardLayout>
    );
}
