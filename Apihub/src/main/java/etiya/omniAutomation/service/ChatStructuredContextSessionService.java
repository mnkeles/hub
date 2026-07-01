package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ChatRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatStructuredContextSessionService {

    @Value("${chat.context.expiration-minutes:${chat.history.expiration-minutes:3}}")
    private int expirationMinutes;

    private final ChatContextBuilderService chatContextBuilderService;
    private final HarAnalysisSessionService harAnalysisSessionService;
    private final Map<String, ChatRequest> userStructuredContexts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastActivityTimes = new ConcurrentHashMap<>();

    public ChatStructuredContextSessionService(
            ChatContextBuilderService chatContextBuilderService,
            HarAnalysisSessionService harAnalysisSessionService
    ) {
        this.chatContextBuilderService = chatContextBuilderService;
        this.harAnalysisSessionService = harAnalysisSessionService;
    }

    public ChatRequest mergeWithStoredContext(String userId, ChatRequest request) {
        ChatRequest safeRequest = request != null ? request : new ChatRequest();
        ChatRequest normalizedRequest = normalizeGenericContext(safeRequest);
        ChatRequest hydratedRequest = hydrateAnalysisReference(userId, normalizedRequest);
        ChatRequest effectiveRequest = copyRequest(hydratedRequest);
        ChatRequest storedContext = userStructuredContexts.get(userId);

        if (storedContext != null) {
            applyMissingContext(effectiveRequest, storedContext);
        }

        if (chatContextBuilderService.hasStructuredContext(hydratedRequest)) {
            ChatRequest mergedContext = storedContext != null ? copyRequest(storedContext) : new ChatRequest();
            overlayContext(mergedContext, hydratedRequest);
            ChatRequest reducedContext = reduceContextRequest(mergedContext);
            userStructuredContexts.put(userId, reducedContext);
            overlayContext(effectiveRequest, reducedContext);
        }

        updateLastActivity(userId);
        return effectiveRequest;
    }

    public Map<String, Object> getCurrentStructuredContext(String userId) {
        updateLastActivity(userId);
        ChatRequest storedContext = userStructuredContexts.get(userId);
        Map<String, Object> response = new LinkedHashMap<>();

        if (storedContext == null || !chatContextBuilderService.hasStructuredContext(storedContext)) {
            response.put("available", false);
            response.put("message", "Aktif HAR review bağlamı bulunamadı.");
            return response;
        }

        response.put("available", true);
        response.put("updatedAt", lastActivityTimes.get(userId));
        response.putAll(chatContextBuilderService.buildStructuredContextPayload(storedContext));
        return response;
    }

    public void clearContext(String userId) {
        userStructuredContexts.remove(userId);
        lastActivityTimes.remove(userId);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredContexts() {
        LocalDateTime now = LocalDateTime.now();
        lastActivityTimes.forEach((userId, lastActivity) -> {
            if (lastActivity.plusMinutes(expirationMinutes).isBefore(now)) {
                clearContext(userId);
            }
        });
    }

    private ChatRequest copyRequest(ChatRequest source) {
        ChatRequest target = new ChatRequest();
        if (source == null) {
            return target;
        }
        target.setMessage(source.getMessage());
        target.setContext(source.getContext());
        target.setContextType(source.getContextType());
        target.setHarAnalysisResult(source.getHarAnalysisResult());
        target.setReviewedProcessFlowDraft(source.getReviewedProcessFlowDraft());
        target.setCurrentStepResolutionState(source.getCurrentStepResolutionState());
        target.setHarAnalysisReferenceId(source.getHarAnalysisReferenceId());
        target.setReviewedHarContext(source.getReviewedHarContext());
        target.setIncludedSteps(source.getIncludedSteps());
        target.setExcludedSteps(source.getExcludedSteps());
        target.setSelectedApiMappings(source.getSelectedApiMappings());
        target.setUnresolvedSteps(source.getUnresolvedSteps());
        target.setBlockers(source.getBlockers());
        target.setProjectShortCode(source.getProjectShortCode());
        target.setSystemShortCode(source.getSystemShortCode());
        return target;
    }

    private void applyMissingContext(ChatRequest target, ChatRequest source) {
        if (StringUtils.isBlank(target.getContextType())) {
            target.setContextType(source.getContextType());
        }
        if (target.getHarAnalysisResult() == null) {
            target.setHarAnalysisResult(source.getHarAnalysisResult());
        }
        if (target.getReviewedProcessFlowDraft() == null) {
            target.setReviewedProcessFlowDraft(source.getReviewedProcessFlowDraft());
        }
        if (target.getCurrentStepResolutionState() == null) {
            target.setCurrentStepResolutionState(source.getCurrentStepResolutionState());
        }
        if (StringUtils.isBlank(target.getHarAnalysisReferenceId())) {
            target.setHarAnalysisReferenceId(source.getHarAnalysisReferenceId());
        }
        if (target.getReviewedHarContext() == null) {
            target.setReviewedHarContext(source.getReviewedHarContext());
        }
        if (target.getIncludedSteps() == null) {
            target.setIncludedSteps(source.getIncludedSteps());
        }
        if (target.getExcludedSteps() == null) {
            target.setExcludedSteps(source.getExcludedSteps());
        }
        if (target.getSelectedApiMappings() == null) {
            target.setSelectedApiMappings(source.getSelectedApiMappings());
        }
        if (target.getUnresolvedSteps() == null) {
            target.setUnresolvedSteps(source.getUnresolvedSteps());
        }
        if (target.getBlockers() == null) {
            target.setBlockers(source.getBlockers());
        }
        if (StringUtils.isBlank(target.getProjectShortCode())) {
            target.setProjectShortCode(source.getProjectShortCode());
        }
        if (StringUtils.isBlank(target.getSystemShortCode())) {
            target.setSystemShortCode(source.getSystemShortCode());
        }
    }

    private void overlayContext(ChatRequest target, ChatRequest source) {
        if (StringUtils.isNotBlank(source.getContextType())) {
            target.setContextType(source.getContextType());
        }
        if (source.getHarAnalysisResult() != null) {
            target.setHarAnalysisResult(source.getHarAnalysisResult());
        }
        if (source.getReviewedProcessFlowDraft() != null) {
            target.setReviewedProcessFlowDraft(source.getReviewedProcessFlowDraft());
        }
        if (source.getCurrentStepResolutionState() != null) {
            target.setCurrentStepResolutionState(source.getCurrentStepResolutionState());
        }
        if (StringUtils.isNotBlank(source.getHarAnalysisReferenceId())) {
            target.setHarAnalysisReferenceId(source.getHarAnalysisReferenceId());
        }
        if (source.getReviewedHarContext() != null) {
            target.setReviewedHarContext(source.getReviewedHarContext());
        }
        if (source.getIncludedSteps() != null) {
            target.setIncludedSteps(source.getIncludedSteps());
        }
        if (source.getExcludedSteps() != null) {
            target.setExcludedSteps(source.getExcludedSteps());
        }
        if (source.getSelectedApiMappings() != null) {
            target.setSelectedApiMappings(source.getSelectedApiMappings());
        }
        if (source.getUnresolvedSteps() != null) {
            target.setUnresolvedSteps(source.getUnresolvedSteps());
        }
        if (source.getBlockers() != null) {
            target.setBlockers(source.getBlockers());
        }
        if (StringUtils.isNotBlank(source.getProjectShortCode())) {
            target.setProjectShortCode(source.getProjectShortCode());
        }
        if (StringUtils.isNotBlank(source.getSystemShortCode())) {
            target.setSystemShortCode(source.getSystemShortCode());
        }
    }

    private ChatRequest reduceContextRequest(ChatRequest source) {
        Map<String, Object> reducedPayload = chatContextBuilderService.buildStructuredContextPayload(source);
        ChatRequest reducedRequest = new ChatRequest();
        reducedRequest.setContextType((String) reducedPayload.get("contextType"));
        reducedRequest.setHarAnalysisReferenceId((String) reducedPayload.get("analysisReferenceId"));
        reducedRequest.setProjectShortCode((String) reducedPayload.get("projectShortCode"));
        reducedRequest.setSystemShortCode((String) reducedPayload.get("systemShortCode"));
        reducedRequest.setHarAnalysisResult(reducedPayload.get("harAnalysisResult"));
        reducedRequest.setReviewedProcessFlowDraft(reducedPayload.get("reviewedProcessFlowDraft"));
        reducedRequest.setCurrentStepResolutionState(reducedPayload.get("currentStepResolutionState"));
        reducedRequest.setReviewedHarContext(reducedPayload.get("reviewedHarContext"));
        reducedRequest.setIncludedSteps(reducedPayload.get("includedSteps"));
        reducedRequest.setExcludedSteps(reducedPayload.get("excludedSteps"));
        reducedRequest.setSelectedApiMappings(reducedPayload.get("selectedApiMappings"));
        reducedRequest.setUnresolvedSteps(reducedPayload.get("unresolvedSteps"));
        reducedRequest.setBlockers(reducedPayload.get("blockers"));
        return reducedRequest;
    }

    private ChatRequest normalizeGenericContext(ChatRequest request) {
        if (request == null || request.getContext() == null) {
            return request;
        }

        if (!(request.getContext() instanceof Map<?, ?> rawContextMap)) {
            return request;
        }

        Object type = rawContextMap.get("type");
        if (!StringUtils.equalsIgnoreCase(String.valueOf(type), "har-review")) {
            return request;
        }

        Object reviewedHar = rawContextMap.get("reviewedHar");
        if (!(reviewedHar instanceof Map<?, ?> reviewedHarMap)) {
            return request;
        }

        ChatRequest normalizedRequest = copyRequest(request);
        if (StringUtils.isBlank(normalizedRequest.getContextType())) {
            normalizedRequest.setContextType("HAR_REVIEW");
        }
        if (normalizedRequest.getReviewedHarContext() == null) {
            normalizedRequest.setReviewedHarContext(reviewedHar);
        }
        if (normalizedRequest.getReviewedProcessFlowDraft() == null) {
            normalizedRequest.setReviewedProcessFlowDraft(reviewedHarMap.get("reviewedDraft"));
        }
        if (normalizedRequest.getIncludedSteps() == null) {
            normalizedRequest.setIncludedSteps(reviewedHarMap.get("includedSteps"));
        }
        if (normalizedRequest.getExcludedSteps() == null) {
            normalizedRequest.setExcludedSteps(reviewedHarMap.get("excludedSteps"));
        }
        if (normalizedRequest.getSelectedApiMappings() == null) {
            normalizedRequest.setSelectedApiMappings(reviewedHarMap.get("selectedApiMappings"));
        }
        if (normalizedRequest.getUnresolvedSteps() == null) {
            normalizedRequest.setUnresolvedSteps(reviewedHarMap.get("unresolvedSteps"));
        }
        if (normalizedRequest.getBlockers() == null) {
            normalizedRequest.setBlockers(reviewedHarMap.get("blockers"));
        }
        if (StringUtils.isBlank(normalizedRequest.getProjectShortCode()) && reviewedHarMap.get("projectShortCode") != null) {
            normalizedRequest.setProjectShortCode(String.valueOf(reviewedHarMap.get("projectShortCode")));
        }
        if (StringUtils.isBlank(normalizedRequest.getSystemShortCode()) && reviewedHarMap.get("systemShortCode") != null) {
            normalizedRequest.setSystemShortCode(String.valueOf(reviewedHarMap.get("systemShortCode")));
        }
        normalizedRequest.setContext(null);
        return normalizedRequest;
    }

    private ChatRequest hydrateAnalysisReference(String userId, ChatRequest request) {
        if (request == null || StringUtils.isBlank(request.getHarAnalysisReferenceId()) || request.getHarAnalysisResult() != null) {
            return request;
        }

        Object resolvedHarAnalysis = harAnalysisSessionService.getAnalysisPayload(userId, request.getHarAnalysisReferenceId());
        if (resolvedHarAnalysis == null) {
            return request;
        }

        ChatRequest hydratedRequest = copyRequest(request);
        hydratedRequest.setHarAnalysisResult(resolvedHarAnalysis);

        if (resolvedHarAnalysis instanceof Map<?, ?> resolvedAnalysisMap) {
            if (StringUtils.isBlank(hydratedRequest.getProjectShortCode()) && resolvedAnalysisMap.get("projectShortCode") != null) {
                hydratedRequest.setProjectShortCode(String.valueOf(resolvedAnalysisMap.get("projectShortCode")));
            }
            if (StringUtils.isBlank(hydratedRequest.getSystemShortCode()) && resolvedAnalysisMap.get("systemShortCode") != null) {
                hydratedRequest.setSystemShortCode(String.valueOf(resolvedAnalysisMap.get("systemShortCode")));
            }
        }

        return hydratedRequest;
    }

    private void updateLastActivity(String userId) {
        lastActivityTimes.put(userId, LocalDateTime.now());
    }
}
