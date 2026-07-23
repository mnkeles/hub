package etiya.omniAutomation.entity;

import etiya.omniAutomation.business.dto.PerformanceScheduleStatus;
import etiya.omniAutomation.request.PerformanceRequest;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "perf_schedule")
public class PerformanceScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "process_flow_id")
    private Long processFlowId;

    @Column(name = "name")
    private String name;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "enabled")
    private Boolean enabled;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_snapshot", columnDefinition = "jsonb")
    private PerformanceRequest requestSnapshot;

    @Column(name = "last_run_at")
    private Date lastRunAt;

    @Column(name = "next_run_at")
    private Date nextRunAt;

    @Column(name = "last_result_id")
    private Long lastResultId;

    @Column(name = "last_status")
    @Enumerated(EnumType.STRING)
    private PerformanceScheduleStatus lastStatus;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}
