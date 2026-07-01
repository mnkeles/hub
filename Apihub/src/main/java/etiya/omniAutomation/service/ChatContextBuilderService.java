package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatContextBuilderService {

    private static final int MAX_PROMPT_CONTEXT_LENGTH = 12000;
    private static final List<String> PRIORITY_KEYS = List.of(
            "analysisReferenceId",
            "projectShortCode",
            "systemShortCode",
            "analysisSummary",
            "projectId",
            "projectName",
            "flowShortCodeSuggestion",
            "summary",
            "statistics",
            "warnings",
            "recommendations",
            "baseUrls",
            "endpoints",
            "ignoredRequests",
            "logicalSteps",
            "relationships",
            "processFlowDraft",
            "reviewedHarContext",
            "originalHarAnalysisResult",
            "reviewedDraft",
            "includedSteps",
            "excludedSteps",
            "unresolvedSteps",
            "blockers",
            "selectedApiMappings",
            "selectedApiMatches",
            "selectedApiInformationMatches",
            "selectedApiInformation",
            "stepShortCodes"
    );

    private final ObjectMapper objectMapper;

    public String buildChatContext(ChatRequest request) {
        if (!hasStructuredContext(request)) {
            return null;
        }

        String primaryContext = serialize(buildStructuredContextPayload(request), true);
        if (primaryContext.length() <= MAX_PROMPT_CONTEXT_LENGTH) {
            return primaryContext;
        }

        String reducedContext = serialize(buildStructuredContext(request, new ReductionOptions(3, 6, 400)), false);
        return StringUtils.abbreviate(reducedContext, MAX_PROMPT_CONTEXT_LENGTH);
    }

    public boolean hasStructuredContext(ChatRequest request) {
        return request != null && (
                StringUtils.isNotBlank(request.getContextType())
                        || StringUtils.isNotBlank(request.getHarAnalysisReferenceId())
                        || request.getHarAnalysisResult() != null
                        || request.getReviewedProcessFlowDraft() != null
                        || request.getCurrentStepResolutionState() != null
                        || request.getReviewedHarContext() != null
                        || request.getIncludedSteps() != null
                        || request.getExcludedSteps() != null
                        || request.getSelectedApiMappings() != null
                        || request.getUnresolvedSteps() != null
                        || request.getBlockers() != null
                        || StringUtils.isNotBlank(request.getProjectShortCode())
                        || StringUtils.isNotBlank(request.getSystemShortCode())
        );
    }

    public Map<String, Object> buildStructuredContextPayload(ChatRequest request) {
        if (!hasStructuredContext(request)) {
            return new LinkedHashMap<>();
        }
        return buildStructuredContext(request, new ReductionOptions(4, 12, 1000));
    }

    private Map<String, Object> buildStructuredContext(ChatRequest request, ReductionOptions options) {
        Map<String, Object> context = new LinkedHashMap<>();
        putIfNotBlank(context, "contextType", resolveContextType(request));
        putIfNotBlank(context, "analysisReferenceId", request.getHarAnalysisReferenceId());
        putIfNotBlank(context, "projectShortCode", request.getProjectShortCode());
        putIfNotBlank(context, "systemShortCode", request.getSystemShortCode());
        putIfPresent(context, "harAnalysisResult", reduceSection(request.getHarAnalysisResult(), options));
        putIfPresent(context, "reviewedProcessFlowDraft", reduceSection(request.getReviewedProcessFlowDraft(), options));
        putIfPresent(context, "currentStepResolutionState", reduceSection(request.getCurrentStepResolutionState(), options));
        putIfPresent(context, "reviewedHarContext", reduceSection(request.getReviewedHarContext(), options));
        putIfPresent(context, "includedSteps", reduceSection(request.getIncludedSteps(), options));
        putIfPresent(context, "excludedSteps", reduceSection(request.getExcludedSteps(), options));
        putIfPresent(context, "selectedApiMappings", reduceSection(request.getSelectedApiMappings(), options));
        putIfPresent(context, "unresolvedSteps", reduceSection(request.getUnresolvedSteps(), options));
        putIfPresent(context, "blockers", reduceSection(request.getBlockers(), options));
        return context;
    }

    private String resolveContextType(ChatRequest request) {
        if (StringUtils.isNotBlank(request.getContextType())) {
            return request.getContextType();
        }
        if (request.getReviewedHarContext() != null
                || request.getIncludedSteps() != null
                || request.getExcludedSteps() != null
                || request.getSelectedApiMappings() != null
                || request.getUnresolvedSteps() != null
                || request.getBlockers() != null) {
            return "HAR_REVIEW";
        }
        if (request.getReviewedProcessFlowDraft() != null || request.getCurrentStepResolutionState() != null) {
            return "HAR_REVIEW";
        }
        if (request.getHarAnalysisResult() != null) {
            return "HAR_ANALYSIS";
        }
        return "STRUCTURED_CHAT_CONTEXT";
    }

    private Object reduceSection(Object section, ReductionOptions options) {
        if (section == null) {
            return null;
        }

        Object normalizedSection;
        try {
            normalizedSection = normalizeSection(section);
        }
        catch (IllegalArgumentException ex) {
            normalizedSection = String.valueOf(section);
        }

        return reduceValue(normalizedSection, 0, options, true);
    }

    private Object normalizeSection(Object section) {
        if (section instanceof String textSection) {
            String trimmedSection = StringUtils.trimToEmpty(textSection);
            if (trimmedSection.startsWith("{") || trimmedSection.startsWith("[")) {
                try {
                    return objectMapper.readValue(trimmedSection, Object.class);
                }
                catch (JsonProcessingException ex) {
                    return trimmedSection;
                }
            }
            return trimmedSection;
        }
        return objectMapper.convertValue(section, Object.class);
    }

    private Object reduceValue(Object value, int depth, ReductionOptions options, boolean rootLevel) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence text) {
            return abbreviate(text.toString(), options.maxTextLength());
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (depth >= options.maxDepth()) {
            return abbreviate(serialize(value, false), options.maxTextLength());
        }
        if (value instanceof Map<?, ?> mapValue) {
            return reduceMap(mapValue, depth + 1, options, rootLevel);
        }
        if (value instanceof Collection<?> collectionValue) {
            return reduceCollection(collectionValue, depth + 1, options);
        }
        return abbreviate(String.valueOf(value), options.maxTextLength());
    }

    private Map<String, Object> reduceMap(Map<?, ?> rawMap, int depth, ReductionOptions options, boolean rootLevel) {
        Map<String, Object> normalizedMap = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null) {
                normalizedMap.put(String.valueOf(key), value);
            }
        });

        Map<String, Object> reducedMap = new LinkedHashMap<>();
        List<String> orderedKeys = new ArrayList<>();

        for (String priorityKey : PRIORITY_KEYS) {
            if (normalizedMap.containsKey(priorityKey)) {
                orderedKeys.add(priorityKey);
            }
        }

        for (String key : normalizedMap.keySet()) {
            if (!orderedKeys.contains(key)) {
                orderedKeys.add(key);
            }
        }

        int processedCount = 0;
        for (String key : orderedKeys) {
            if (processedCount >= options.maxCollectionItems()) {
                break;
            }
            Object reducedValue = reduceValue(normalizedMap.get(key), depth, options, false);
            if (isMeaningful(reducedValue)) {
                reducedMap.put(key, reducedValue);
                processedCount++;
            }
        }

        int remainingFieldCount = normalizedMap.size() - reducedMap.size();
        if (remainingFieldCount > 0) {
            reducedMap.put("remainingFieldCount", remainingFieldCount);
        }

        return reducedMap;
    }

    private List<Object> reduceCollection(Collection<?> collection, int depth, ReductionOptions options) {
        List<Object> reducedCollection = new ArrayList<>();

        for (Object item : collection) {
            if (reducedCollection.size() >= options.maxCollectionItems()) {
                break;
            }
            Object reducedItem = reduceValue(item, depth, options, false);
            if (isMeaningful(reducedItem)) {
                reducedCollection.add(reducedItem);
            }
        }

        int remainingItemCount = collection.size() - reducedCollection.size();
        if (remainingItemCount > 0) {
            Map<String, Object> remainingItems = new LinkedHashMap<>();
            remainingItems.put("remainingItemCount", remainingItemCount);
            reducedCollection.add(remainingItems);
        }

        return reducedCollection;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (isMeaningful(value)) {
            target.put(key, value);
        }
    }

    private boolean isMeaningful(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String textValue) {
            return StringUtils.isNotBlank(textValue);
        }
        if (value instanceof Collection<?> collectionValue) {
            return !collectionValue.isEmpty();
        }
        if (value instanceof Map<?, ?> mapValue) {
            return !mapValue.isEmpty();
        }
        return true;
    }

    private String abbreviate(String value, int maxLength) {
        return StringUtils.abbreviate(StringUtils.trimToEmpty(value), maxLength);
    }

    private String serialize(Object value, boolean pretty) {
        try {
            if (pretty) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            }
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private record ReductionOptions(int maxDepth, int maxCollectionItems, int maxTextLength) {
    }
}
