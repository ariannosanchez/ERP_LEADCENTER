package pe.leadcenter.erp.shared.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import pe.leadcenter.erp.shared.domain.UuidV7;

/** Test-only entity showing the opt-in: soft delete + audit columns + Envers history. */
@Entity
@Table(name = "sample_record")
@Audited
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SampleRecord extends SoftDeletableAuditedEntity {

    @Id private UUID id;

    private String name;

    public SampleRecord(String name) {
        this.id = UuidV7.generate();
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
