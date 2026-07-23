package etiya.omniAutomation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_accounts")
public class ServiceAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_account_id")
    private Long serviceAccountId;

    @Column(name = "service_code", nullable = false, unique = true)
    private String serviceCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "owner")
    private String owner;

    @Column(name = "enabled")
    private int enabled = 1;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udate")
    private Date udate = new Date();
}
