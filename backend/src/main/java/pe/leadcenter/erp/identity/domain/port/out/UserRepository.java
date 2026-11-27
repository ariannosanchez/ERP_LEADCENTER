package pe.leadcenter.erp.identity.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.User;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    List<User> findAll();

    User save(User user);
}
