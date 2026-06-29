package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.ApiInformationDto;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.ApiInformationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-information")
@RequiredArgsConstructor
@Slf4j
public class ApiInformationController {

    private final ApiInformationServiceImpl apiInformationService;

    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAll(@RequestBody(required = false) GeneralPageRequest pageRequest) {
        try {
            if (pageRequest == null) {
                pageRequest = new GeneralPageRequest(0, 100);
            }
            
            List<ApiInformationDto> apiInfoList = apiInformationService.getAll(pageRequest);
            long totalCount = pageRequest.getFilterList() != null && !pageRequest.getFilterList().isEmpty() 
                ? apiInformationService.count(pageRequest) 
                : apiInfoList.size();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", apiInfoList);
            response.put("totalCount", totalCount);
            response.put("offset", pageRequest.getOffset());
            response.put("limit", pageRequest.getLimit());
            
            log.info("Fetched {} API information records out of {}", apiInfoList.size(), totalCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching API information list", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<ApiInformationDto>> getAllSimple() {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<ApiInformationDto> apiInfoList = apiInformationService.getAll(pageRequest);
            log.info("Fetched {} API information records", apiInfoList.size());
            return ResponseEntity.ok(apiInfoList);
        } catch (Exception e) {
            log.error("Error fetching all API information", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiInformationDto> getById(@PathVariable Long id) {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<ApiInformationDto> apiInfoList = apiInformationService.getAll(pageRequest);
            ApiInformationDto apiInfo = apiInfoList.stream()
                .filter(api -> api.getId().equals(id))
                .findFirst()
                .orElse(null);
            
            if (apiInfo == null) {
                log.warn("API information not found with id: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Fetched API information with id: {}", id);
            return ResponseEntity.ok(apiInfo);
        } catch (Exception e) {
            log.error("Error fetching API information by id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/short-code/{shortCode}")
    public ResponseEntity<ApiInformationDto> getByShortCode(@PathVariable String shortCode) {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<ApiInformationDto> apiInfoList = apiInformationService.getAll(pageRequest);
            ApiInformationDto apiInfo = apiInfoList.stream()
                .filter(api -> shortCode.equals(api.getShortCode()))
                .findFirst()
                .orElse(null);
            
            if (apiInfo == null) {
                log.warn("API information not found with short code: {}", shortCode);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Fetched API information with short code: {}", shortCode);
            return ResponseEntity.ok(apiInfo);
        } catch (Exception e) {
            log.error("Error fetching API information by short code: {}", shortCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/project/{projectShortCode}")
    public ResponseEntity<List<ApiInformationDto>> getByProject(@PathVariable String projectShortCode) {
        try {
            List<ApiInformationDto> apiInfoList = apiInformationService.findAllByProjectShortCode(projectShortCode);
            log.info("Fetched {} API information records for project: {}", apiInfoList.size(), projectShortCode);
            return ResponseEntity.ok(apiInfoList);
        } catch (Exception e) {
            log.error("Error fetching API information for project: {}", projectShortCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Result> save(@RequestBody ApiInformationDto apiInformationDto) {
        try {
            Result result = apiInformationService.save(apiInformationDto);
            log.info("API information saved successfully: {}", apiInformationDto.getShortCode());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error saving API information", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @PutMapping("/update")
    public ResponseEntity<Result> update(@RequestBody ApiInformationDto apiInformationDto) {
        try {
            Result result = apiInformationService.save(apiInformationDto);
            log.info("API information updated successfully: {}", apiInformationDto.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating API information", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Long id) {
        try {
            Result result = apiInformationService.deleteApiInformation(id);
            log.info("API information deleted successfully: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting API information: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API Information service is running");
    }
}
