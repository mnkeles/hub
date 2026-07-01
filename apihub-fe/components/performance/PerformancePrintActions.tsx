'use client';

import PrintIcon from '@mui/icons-material/Print';
import { Button } from '@mui/material';
import { useTranslations } from 'next-intl';

export default function PerformancePrintActions() {
    const t = useTranslations('performance');

    return (
        <Button variant="outlined" size="small" startIcon={<PrintIcon fontSize="small" />} onClick={() => window.print()}>
            {t('printPdf')}
        </Button>
    );
}
