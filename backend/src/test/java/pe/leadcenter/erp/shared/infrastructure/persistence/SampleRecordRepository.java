package pe.leadcenter.erp.shared.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SampleRecordRepository extends JpaRepository<SampleRecord, UUID> {}
