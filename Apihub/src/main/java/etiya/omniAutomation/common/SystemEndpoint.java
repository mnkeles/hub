package etiya.omniAutomation.common;

import etiya.omniAutomation.repository.GeneralWebSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemEndpoint {

    private final GeneralWebSystemRepository generalWebSystemRepository;
    Map<String, String> systemEndpoints = new HashMap<>();

    @PostConstruct
    public void initialize() {
        this.generalWebSystemRepository.findAll().forEach(item ->
            systemEndpoints.put(item.getShortCode() + "_" + item.getProjectId(), item.getUrl())
        );
    }

    public String find(String find) {
        return systemEndpoints.get(find);
    }
}