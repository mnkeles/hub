package etiya.omniAutomation.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "api_provider_system")
public class ProviderSystemEntity {

    @Id
    @GeneratedValue
    @Column(name = "api_provider_system_id")
    private long id;

    @Column(name = "description")
    private String description;

    @Column(name = "short_code")
    private String shortCode;

    @OneToMany(mappedBy = "providerSystemEntity")
    @ToString.Exclude()
    private List<ApiInformationEntity> apiInformationEntities;

}
