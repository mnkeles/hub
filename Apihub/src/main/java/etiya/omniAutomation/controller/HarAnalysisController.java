package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.HarAnalysisResultDto;
import etiya.omniAutomation.request.HarAnalysisRequest;
import etiya.omniAutomation.service.HarAnalysisService;
import etiya.omniAutomation.service.HarAnalysisSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/har")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HarAnalysisController {

    private static final long MAX_FILE_SIZE = 100L * 1024L * 1024L;

    private final HarAnalysisService harAnalysisService;
    private final HarAnalysisSessionService harAnalysisSessionService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectShortCode") String projectShortCode,
            @RequestParam("systemShortCode") String systemShortCode
    ) {
        try {
            validateUploadRequest(file, projectShortCode, systemShortCode);

            HarAnalysisResultDto analysisResult = harAnalysisService.analyzeUpload(projectShortCode, systemShortCode, file);
            String analysisReferenceId = harAnalysisSessionService.storeAnalysis(getCurrentUserId(), analysisResult);
            analysisResult.setAnalysisReferenceId(analysisReferenceId);
            return ResponseEntity.ok(analysisResult);
        }
        catch (IllegalArgumentException ex) {
            log.warn("HAR upload analyze validation hatası - User: {}", getCurrentUserId(), ex);
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
        catch (Exception ex) {
            log.error("HAR upload analyze hatası - User: {}", getCurrentUserId(), ex);
            return ResponseEntity.internalServerError().body(Map.of("message", "HAR file analyze edilirken hata oluştu."));
        }
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyze(@RequestBody HarAnalysisRequest request) {
        try {
            validateAnalyzeRequest(request);
            HarAnalysisResultDto analysisResult = harAnalysisService.analyze(
                    request.getProjectShortCode(),
                    request.getSystemShortCode(),
                    request.getHarContent()
            );
            String analysisReferenceId = harAnalysisSessionService.storeAnalysis(getCurrentUserId(), analysisResult);
            analysisResult.setAnalysisReferenceId(analysisReferenceId);
            return ResponseEntity.ok(analysisResult);
        }
        catch (IllegalArgumentException ex) {
            log.warn("HAR analyze validation hatası - User: {}", getCurrentUserId(), ex);
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
        catch (Exception ex) {
            log.error("HAR analyze hatası - User: {}", getCurrentUserId(), ex);
            return ResponseEntity.internalServerError().body(Map.of("message", "HAR content analyze edilirken hata oluştu."));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("HAR analysis service is running");
    }

    private void validateUploadRequest(MultipartFile file, String projectShortCode, String systemShortCode) {
        validateRequiredFields(projectShortCode, systemShortCode);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("HAR file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large. Maximum size is 100MB.");
        }
        String originalFilename = file.getOriginalFilename();
        boolean supportedFile = originalFilename != null
                && (originalFilename.toLowerCase().endsWith(".har") || originalFilename.toLowerCase().endsWith(".json"));
        if (!supportedFile) {
            throw new IllegalArgumentException("Invalid file type. Only .har or .json files are accepted.");
        }
    }

    private void validateAnalyzeRequest(HarAnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("HAR analysis request is required.");
        }
        validateRequiredFields(request.getProjectShortCode(), request.getSystemShortCode());
        if (request.getHarContent() == null || request.getHarContent().isBlank()) {
            throw new IllegalArgumentException("HAR content is required.");
        }
    }

    private void validateRequiredFields(String projectShortCode, String systemShortCode) {
        if (projectShortCode == null || projectShortCode.isBlank()) {
            throw new IllegalArgumentException("projectShortCode is required.");
        }
        if (systemShortCode == null || systemShortCode.isBlank()) {
            throw new IllegalArgumentException("systemShortCode is required.");
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
