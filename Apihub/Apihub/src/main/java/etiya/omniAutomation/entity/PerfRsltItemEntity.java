package etiya.omniAutomation.entity;

import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.common.GeneralEnums;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "perf_rslt_item")
public class PerfRsltItemEntity {

    @Id
    @Column(name = "perf_rslt_item_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "perf_rslt_item_id_seq")
    @SequenceGenerator(name = "perf_rslt_item_id_seq", sequenceName = "perf_rslt_item_perf_rslt_item_id_seq", allocationSize = 1)
    private Long perfRsltItemId;

    @Column(name = "perf_rslt_id")
    private Long perfRsltId;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private PerformanceThreadGroup performanceThreadGroup;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "perf_rslt_id", insertable = false, updatable = false)
    private PerfRsltEntity perfRsltEntity;

}
