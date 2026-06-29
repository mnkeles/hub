package etiya.omniAutomation.business.dto;

import java.util.Date;
import java.util.List;

public record PerformanceValidationChecklist(
        List<PerformanceValidationChecklistItem> items,
        String manualNote,
        Date manualNoteUpdatedAt
) {
}
