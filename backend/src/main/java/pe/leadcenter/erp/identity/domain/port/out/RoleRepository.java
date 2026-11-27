package pe.leadcenter.erp.identity.domain.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.leadcenter.erp.identity.domain.Role;

public interface RoleRepository {

    Optional<Role> findById(UUID id);

    List<Role> findAllById(Collection<UUID> ids);

    List<Role> findAll();

    Role save(Role role);
}
