'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { usePathname } from 'next/navigation';
import { ProjectDto } from '@/types/api';
import { projectService } from '@/services/projectService';
import { userService, CurrentUser } from '@/services/userService';
import { useAuth } from '@/contexts/AuthContext';

interface ProjectContextType {
    projects: ProjectDto[];
    selectedProject: ProjectDto | null;
    setSelectedProject: (project: ProjectDto | null) => void;
    loading: boolean;
    projectSelectDisabled: boolean;
}

const ProjectContext = createContext<ProjectContextType | undefined>(undefined);

const PUBLIC_PATHS = new Set(['/', '/login', '/landing', '/logout']);

function getUserProjectId(currentUser: CurrentUser): number | null {
    return (currentUser as CurrentUser & { projectId?: number | null }).projectId ?? null;
}

export function ProjectProvider({ children }: { children: ReactNode }) {
    const [projects, setProjects] = useState<ProjectDto[]>([]);
    const [selectedProject, setSelectedProjectState] = useState<ProjectDto | null>(null);
    const [loading, setLoading] = useState(false);
    const [projectSelectDisabled, setProjectSelectDisabled] = useState(false);
    const { isAuthenticated, isLoading: authLoading } = useAuth();
    const pathname = usePathname();

    const resetProjects = useCallback(() => {
        setProjects([]);
        setSelectedProjectState(null);
        setProjectSelectDisabled(false);
        setLoading(false);
    }, []);

    const loadProjects = useCallback(async () => {
        try {
            setLoading(true);
            const currentUser = await userService.getCurrentUser();
            const userProjectId = getUserProjectId(currentUser);
            const data = await projectService.getAll();
            const hasRestrictedProject = userProjectId !== null;

            setProjects(data);
            setProjectSelectDisabled(hasRestrictedProject);

            if (data.length === 0) {
                setSelectedProjectState(null);
                return;
            }

            if (hasRestrictedProject) {
                const userProject = data.find((project) => project.projectId === userProjectId) ?? data[0];
                setSelectedProjectState(userProject);
                return;
            }

            setSelectedProjectState((previousProject) => {
                if (!previousProject?.projectId) {
                    return data[0];
                }

                return data.find((project) => project.projectId === previousProject.projectId) ?? data[0];
            });
        } catch (err) {
            console.error('Failed to load projects:', err);
            resetProjects();
        } finally {
            setLoading(false);
        }
    }, [resetProjects]);

    useEffect(() => {
        if (authLoading) {
            return;
        }

        if (!isAuthenticated || PUBLIC_PATHS.has(pathname)) {
            resetProjects();
            return;
        }

        void loadProjects();
    }, [authLoading, isAuthenticated, loadProjects, pathname, resetProjects]);

    const setSelectedProject = (project: ProjectDto | null) => {
        if (projectSelectDisabled) {
            return;
        }

        setSelectedProjectState(project);
    };

    return (
        <ProjectContext.Provider value={{ projects, selectedProject, setSelectedProject, loading, projectSelectDisabled }}>
            {children}
        </ProjectContext.Provider>
    );
}

export function useProject() {
    const context = useContext(ProjectContext);
    if (context === undefined) {
        throw new Error('useProject must be used within a ProjectProvider');
    }
    return context;
}
