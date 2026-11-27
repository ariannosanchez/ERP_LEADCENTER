package pe.leadcenter.erp.identity.infrastructure.out.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.PasswordHash;
import pe.leadcenter.erp.identity.domain.User;

/** Domain {@code User} (accessors are not bean getters) to/from {@link UserEntity}. */
@Mapper
interface UserMapper {

    @Mapping(target = "id", expression = "java(user.id())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "email", expression = "java(user.email().value())")
    @Mapping(target = "fullName", expression = "java(user.fullName())")
    @Mapping(target = "passwordHash", expression = "java(user.passwordHash().value())")
    @Mapping(target = "active", expression = "java(user.active())")
    @Mapping(target = "roleIds", expression = "java(new java.util.HashSet<>(user.roleIds()))")
    UserEntity toEntity(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "email", expression = "java(user.email().value())")
    @Mapping(target = "fullName", expression = "java(user.fullName())")
    @Mapping(target = "passwordHash", expression = "java(user.passwordHash().value())")
    @Mapping(target = "active", expression = "java(user.active())")
    @Mapping(target = "roleIds", expression = "java(new java.util.HashSet<>(user.roleIds()))")
    void update(User user, @MappingTarget UserEntity entity);

    default User toDomain(UserEntity entity) {
        return User.restore(
                entity.getId(),
                new Email(entity.getEmail()),
                entity.getFullName(),
                new PasswordHash(entity.getPasswordHash()),
                entity.isActive(),
                entity.getRoleIds());
    }
}
