package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HarAnalysisResultDto {

    private String analysisReferenceId;
    private String projectShortCode;
    private String systemShortCode;
    private Long projectId;
    private String projectName;
    private String flowShortCodeSuggestion;
    private String summary;
    private List<Map<String, Object>> endpoints;
    private List<String> baseUrls;
    private List<String> recommendations;
    private List<String> warnings;
    private Map<String, Object> statistics;
    private List<Map<String, Object>> ignoredRequests;
    private List<Map<String, Object>> logicalSteps;
    private List<Map<String, Object>> relationships;
    private Map<String, Object> processFlowDraft;
}
