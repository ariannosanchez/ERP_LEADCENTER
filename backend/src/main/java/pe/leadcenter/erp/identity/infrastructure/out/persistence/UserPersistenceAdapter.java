package pe.leadcenter.erp.identity.infrastructure.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.User;
import pe.leadcenter.erp.identity.domain.port.out.UserRepository;

@Repository
@Transactional
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public User save(User user) {
        UserEntity entity =
                jpa.findById(user.id())
                        .map(
                                existing -> {
                                    mapper.update(user, existing);
                                    return existing;
                                })
                        .orElseGet(() -> mapper.toEntity(user));
        return mapper.toDomain(jpa.saveAndFlush(entity));
    }
}
