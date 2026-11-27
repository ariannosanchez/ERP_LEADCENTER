package pe.leadcenter.erp.identity.infrastructure.out.persistence;

import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.identity.domain.Role;

/** Domain {@code Role} (accessors are not bean getters) to/from {@link RoleEntity}. */
@Mapper
interface RoleMapper {

    @Mapping(target = "id", expression = "java(role.id())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "name", expression = "java(role.name())")
    @Mapping(target = "description", expression = "java(role.description())")
    @Mapping(target = "system", expression = "java(role.system())")
    @Mapping(target = "permissionCodes", expression = "java(toCodes(role.permissions()))")
    RoleEntity toEntity(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "name", expression = "java(role.name())")
    @Mapping(target = "description", expression = "java(role.description())")
    @Mapping(target = "system", expression = "java(role.system())")
    @Mapping(target = "permissionCodes", expression = "java(toCodes(role.permissions()))")
    void update(Role role, @MappingTarget RoleEntity entity);

    Set<String> toCodes(Set<Permission> permissions);

    Set<Permission> toPermissions(Set<String> codes);

    default String toCode(Permission permission) {
        return permission.code();
    }

    /** An unknown stored code fails loudly ({@code permission.unknown}); it is never dropped. */
    default Permission toPermission(String code) {
        return Permission.fromCode(code);
    }

    default Role toDomain(RoleEntity entity) {
        return Role.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isSystem(),
                toPermissions(entity.getPermissionCodes()));
    }
}
