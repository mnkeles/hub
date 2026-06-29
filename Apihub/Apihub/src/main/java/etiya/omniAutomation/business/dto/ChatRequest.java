package etiya.omniAutomation.business.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    @JsonAlias("userMessage")
    private String message;
    private Object context;
    private String contextType;
    @JsonAlias("originalHarAnalysisResult")
    private Object harAnalysisResult;
    @JsonAlias("reviewedDraft")
    private Object reviewedProcessFlowDraft;
    @JsonAlias("stepResolutionState")
    private Object currentStepResolutionState;
    @JsonAlias("analysisReferenceId")
    private String harAnalysisReferenceId;
    private Object reviewedHarContext;
    private Object includedSteps;
    private Object excludedSteps;
    private Object selectedApiMappings;
    private Object unresolvedSteps;
    private Object blockers;
    private String projectShortCode;
    private String systemShortCode;
}
