package pe.leadcenter.erp.identity.infrastructure.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.leadcenter.erp.identity.domain.Role;
import pe.leadcenter.erp.identity.domain.port.out.RoleRepository;

@Repository
@Transactional
@RequiredArgsConstructor
class RolePersistenceAdapter implements RoleRepository {

    private final RoleJpaRepository jpa;
    private final RoleMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity =
                jpa.findById(role.id())
                        .map(
                                existing -> {
                                    mapper.update(role, existing);
                                    return existing;
                                })
                        .orElseGet(() -> mapper.toEntity(role));
        return mapper.toDomain(jpa.saveAndFlush(entity));
    }
}
