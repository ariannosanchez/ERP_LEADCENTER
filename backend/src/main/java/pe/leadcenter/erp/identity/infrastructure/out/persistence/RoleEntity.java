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

/**
 * Roles are audited with history but never deleted (no soft delete): the table has no {@code
 * deleted_at}, so the case-insensitive unique name always refers to a live role.
 */
@Entity
@Table(name = "roles")
@Audited
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RoleEntity extends AuditedEntity {

    @Id private UUID id;

    @Version private Long version;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean system;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_code", nullable = false)
    private Set<String> permissionCodes = new HashSet<>();
}
