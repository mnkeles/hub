package etiya.omniAutomation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gnl_default_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project", nullable = false)
    private String project;

    @Column(name = "system_short_code", nullable = false)
    private String systemShortCode;

    @Column(name = "process_flow", nullable = false)
    private String processFlow;

    @Column(name = "parameter_context", columnDefinition = "TEXT")
    private String parameterContext; // JSON formatında saklanacak

    @Column(name = "global_headers", columnDefinition = "TEXT")
    private String globalHeaders; // JSON formatında saklanacak

    @Column(name = "is_active")
    private Boolean isActive = true;
}
