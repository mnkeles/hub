export function getErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof Error && error.message) {
        return error.message;
    }

    if (typeof error === 'object' && error !== null && 'message' in error) {
        const message = (error as { message?: unknown }).message;
        if (typeof message === 'string' && message) {
            return message;
        }
    }

    return fallback;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null && 'response' in error) {
        const response = (error as { response?: { data?: { message?: unknown } } }).response;
        const message = response?.data?.message;
        if (typeof message === 'string' && message) {
            return message;
        }
    }

    return getErrorMessage(error, fallback);
}

export function getApiErrorStatus(error: unknown): number | undefined {
    if (typeof error === 'object' && error !== null && 'response' in error) {
        const status = (error as { response?: { status?: unknown } }).response?.status;
        return typeof status === 'number' ? status : undefined;
    }

    if (typeof error === 'object' && error !== null && 'status' in error) {
        const status = (error as { status?: unknown }).status;
        return typeof status === 'number' ? status : undefined;
    }

    return undefined;
}
