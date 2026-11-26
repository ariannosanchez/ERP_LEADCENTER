package pe.leadcenter.erp.shared.infrastructure.persistence;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * Audited entity with logical deletion for business entities. {@code repository.delete(..)} sets
 * {@code deleted_at} (UTC timestamp) instead of removing the row, and Hibernate filters deleted
 * rows from every query. The table needs a nullable {@code deleted_at timestamptz} column.
 */
@MappedSuperclass
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public abstract class SoftDeletableAuditedEntity extends AuditedEntity {}
