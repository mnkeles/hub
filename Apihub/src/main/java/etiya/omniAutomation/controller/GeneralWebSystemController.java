package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.GeneralWebSystemServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/general-web-system")
@RequiredArgsConstructor
@Slf4j
public class GeneralWebSystemController {

    private final GeneralWebSystemServiceImpl generalWebSystemService;

    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAll(@RequestBody(required = false) GeneralPageRequest pageRequest) {
        try {
            // Eğer pageRequest null ise, default değerler ile oluştur
            if (pageRequest == null) {
                pageRequest = new GeneralPageRequest(0, 100);
            }
            
            List<GeneralWebSystemDto> systems = generalWebSystemService.getAll(pageRequest);
            long totalCount = generalWebSystemService.count(pageRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", systems);
            response.put("totalCount", totalCount);
            response.put("offset", pageRequest.getOffset());
            response.put("limit", pageRequest.getLimit());
            
            log.info("Fetched {} general web systems out of {}", systems.size(), totalCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching general web systems", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<GeneralWebSystemDto>> getAllSimple() {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<GeneralWebSystemDto> systems = generalWebSystemService.getAll(pageRequest);
            log.info("Fetched {} general web systems", systems.size());
            return ResponseEntity.ok(systems);
        } catch (Exception e) {
            log.error("Error fetching all general web systems", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Result> save(@RequestBody GeneralWebSystemDto generalWebSystemDto) {
        try {
            Result result = generalWebSystemService.save(generalWebSystemDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error saving general web system", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete general web system by ID
     * @param id General web system ID
     * @return Success or error result
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Long id) {
        try {
            Result result = generalWebSystemService.delete(id);
            log.info("General web system deleted successfully: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting general web system: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("General Web System service is running");
    }
}
