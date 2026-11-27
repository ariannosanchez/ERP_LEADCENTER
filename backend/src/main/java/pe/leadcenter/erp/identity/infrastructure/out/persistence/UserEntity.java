package pe.leadcenter.erp.identity.infrastructure.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import pe.leadcenter.erp.shared.infrastructure.persistence.AuditedEntity;

/** Users are deactivated ({@code active = false}), never deleted, so no soft delete either. */
@Entity
@Table(name = "users")
@Audited
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UserEntity extends AuditedEntity {

    @Id private UUID id;

    @Version private Long version;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_id", nullable = false)
    private Set<UUID> roleIds = new HashSet<>();
}
