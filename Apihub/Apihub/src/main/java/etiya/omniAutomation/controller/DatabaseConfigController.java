package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.DatabaseConfigDto;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.request.GeneralFilter;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.DatabaseConfigServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database-config")
@RequiredArgsConstructor
@Slf4j
public class DatabaseConfigController {

    private final DatabaseConfigServiceImpl databaseConfigService;

    /**
     * Get paginated list of database configurations with optional filters
     * @param pageRequest Optional pagination and filter parameters
     * @return Paginated database config list with metadata
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAll(@RequestBody(required = false) GeneralPageRequest pageRequest) {
        try {
            if (pageRequest == null) {
                pageRequest = new GeneralPageRequest(0, 100);
            }
            
            List<DatabaseConfigDto> configs = databaseConfigService.getAll(pageRequest);
            long totalCount = pageRequest.getFilterList() != null && !pageRequest.getFilterList().isEmpty() 
                ? databaseConfigService.count(pageRequest) 
                : configs.size();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", configs);
            response.put("totalCount", totalCount);
            response.put("offset", pageRequest.getOffset());
            response.put("limit", pageRequest.getLimit());
            
            log.info("Fetched {} database configs out of {}", configs.size(), totalCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching database configs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all database configurations without pagination
     * @return List of all database configurations
     */
    @GetMapping("/all")
    public ResponseEntity<List<DatabaseConfigDto>> getAllSimple() {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<DatabaseConfigDto> configs = databaseConfigService.getAll(pageRequest);
            log.info("Fetched {} database configs", configs.size());
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("Error fetching all database configs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get database configuration by ID
     * @param id Database config ID
     * @return Database configuration details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConfigDto> getById(@PathVariable Long id) {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<DatabaseConfigDto> configs = databaseConfigService.getAll(pageRequest);
            DatabaseConfigDto config = configs.stream()
                .filter(c -> c.getDbConfigId().equals(id))
                .findFirst()
                .orElse(null);
            
            if (config == null) {
                log.warn("Database config not found with id: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Fetched database config with id: {}", id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error fetching database config by id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get database configuration by short code
     * @param shortCode Database config short code
     * @return Database configuration details
     */
    @GetMapping("/short-code/{shortCode}")
    public ResponseEntity<DatabaseConfigDto> getByShortCode(@PathVariable String shortCode) {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<DatabaseConfigDto> configs = databaseConfigService.getAll(pageRequest);
            DatabaseConfigDto config = configs.stream()
                .filter(c -> shortCode.equals(c.getShortCode()))
                .findFirst()
                .orElse(null);
            
            if (config == null) {
                log.warn("Database config not found with short code: {}", shortCode);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Fetched database config with short code: {}", shortCode);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error fetching database config by short code: {}", shortCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get database configurations by project short code
     * @param projectShortCode Project short code
     * @return List of database configurations for the project
     */
    @GetMapping("/project/{projectShortCode}")
    public ResponseEntity<List<DatabaseConfigDto>> getByProject(@PathVariable String projectShortCode) {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            pageRequest.getFilterList().add(GeneralFilter.builder()
                .criteria(GeneralEnums.FilterCriteria.PROJECT_ID)
                .value(projectShortCode)
                .build());
            
            List<DatabaseConfigDto> configs = databaseConfigService.getAll(pageRequest);
            log.info("Fetched {} database configs for project: {}", configs.size(), projectShortCode);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("Error fetching database configs for project: {}", projectShortCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get only active database configurations by project
     * @param projectShortCode Project short code
     * @return List of active database configurations
     */
    @GetMapping("/project/{projectShortCode}/active")
    public ResponseEntity<List<DatabaseConfigDto>> getActiveByProject(@PathVariable String projectShortCode) {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            pageRequest.getFilterList().add(GeneralFilter.builder()
                .criteria(GeneralEnums.FilterCriteria.PROJECT_ID)
                .value(projectShortCode)
                .build());
            
            List<DatabaseConfigDto> configs = databaseConfigService.getAll(pageRequest);
            List<DatabaseConfigDto> activeConfigs = configs.stream()
                .filter(DatabaseConfigDto::isActv)
                .toList();
            
            log.info("Fetched {} active database configs for project: {}", activeConfigs.size(), projectShortCode);
            return ResponseEntity.ok(activeConfigs);
        } catch (Exception e) {
            log.error("Error fetching active database configs for project: {}", projectShortCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Save new database configuration
     * @param databaseConfigDto Database configuration data
     * @return Success or error result
     */
    @PostMapping("/save")
    public ResponseEntity<Result> save(@RequestBody DatabaseConfigDto databaseConfigDto) {
        try {
            Result result = databaseConfigService.save(databaseConfigDto);
            log.info("Database config saved successfully: {}", databaseConfigDto.getShortCode());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error saving database config", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update existing database configuration
     * @param databaseConfigDto Database configuration data
     * @return Success or error result
     */
    @PutMapping("/update")
    public ResponseEntity<Result> update(@RequestBody DatabaseConfigDto databaseConfigDto) {
        try {
            Result result = databaseConfigService.save(databaseConfigDto);
            log.info("Database config updated successfully: {}", databaseConfigDto.getDbConfigId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating database config", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete database configuration by ID
     * @param id Database config ID
     * @return Success or error result
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Long id) {
        try {
            Result result = databaseConfigService.delete(id);
            log.info("Database config deleted successfully: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting database config: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Database Config service is running");
    }
}
