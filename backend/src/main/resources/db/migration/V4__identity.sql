-- Identity module: roles, role permissions, users, user roles (+ Envers history tables).
-- Users and roles are never physically or logically deleted: users are deactivated (active = false).

CREATE TABLE roles (
	id          UUID                     NOT NULL PRIMARY KEY,
	name        VARCHAR(100)             NOT NULL,
	description TEXT,
	system      BOOLEAN                  NOT NULL DEFAULT FALSE,
	version     BIGINT                   NOT NULL DEFAULT 0,
	created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
	created_by  UUID,
	updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
	updated_by  UUID
);
CREATE UNIQUE INDEX roles_name_lower_key ON roles (lower(name));

CREATE TABLE role_permissions (
	role_id         UUID         NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
	permission_code VARCHAR(100) NOT NULL,
	PRIMARY KEY (role_id, permission_code),
	CONSTRAINT role_permissions_code_check CHECK (permission_code IN (
		'usuarios.ver', 'usuarios.gestionar', 'roles.ver', 'roles.gestionar',
		'leads.ver_propios', 'leads.ver_todos', 'leads.asignar', 'leads.gestionar'))
);

CREATE TABLE users (
	id            UUID                     NOT NULL PRIMARY KEY,
	email         VARCHAR(254)             NOT NULL,
	full_name     VARCHAR(150)             NOT NULL,
	password_hash VARCHAR(255)             NOT NULL,
	active        BOOLEAN                  NOT NULL DEFAULT TRUE,
	version       BIGINT                   NOT NULL DEFAULT 0,
	created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
	created_by    UUID,
	updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
	updated_by    UUID,
	CONSTRAINT users_email_key UNIQUE (email),
	CONSTRAINT users_email_normalized_check CHECK (email = lower(btrim(email)))
);

CREATE TABLE user_roles (
	user_id UUID NOT NULL REFERENCES users (id),
	role_id UUID NOT NULL REFERENCES roles (id),
	PRIMARY KEY (user_id, role_id)
);

-- Envers history
CREATE TABLE roles_aud (
	id          UUID   NOT NULL,
	rev         BIGINT NOT NULL REFERENCES revinfo (rev),
	revtype     SMALLINT,
	name        VARCHAR(100),
	description TEXT,
	system      BOOLEAN,
	version     BIGINT,
	created_at  TIMESTAMP WITH TIME ZONE,
	created_by  UUID,
	updated_at  TIMESTAMP WITH TIME ZONE,
	updated_by  UUID,
	PRIMARY KEY (id, rev)
);

CREATE TABLE role_permissions_aud (
	rev             BIGINT       NOT NULL REFERENCES revinfo (rev),
	revtype         SMALLINT,
	role_id         UUID         NOT NULL,
	permission_code VARCHAR(100) NOT NULL,
	PRIMARY KEY (rev, role_id, permission_code)
);

CREATE TABLE users_aud (
	id            UUID   NOT NULL,
	rev           BIGINT NOT NULL REFERENCES revinfo (rev),
	revtype       SMALLINT,
	email         VARCHAR(254),
	full_name     VARCHAR(150),
	password_hash VARCHAR(255),
	active        BOOLEAN,
	version       BIGINT,
	created_at    TIMESTAMP WITH TIME ZONE,
	created_by    UUID,
	updated_at    TIMESTAMP WITH TIME ZONE,
	updated_by    UUID,
	PRIMARY KEY (id, rev)
);

CREATE TABLE user_roles_aud (
	rev     BIGINT NOT NULL REFERENCES revinfo (rev),
	revtype SMALLINT,
	user_id UUID   NOT NULL,
	role_id UUID   NOT NULL,
	PRIMARY KEY (rev, user_id, role_id)
);

-- S4 seed roles (well-known ids shared with Role.ADMINISTRATOR_ID / Role.SALES_ID); created_by NULL = system.
INSERT INTO roles (id, name, description, system, version, created_at, updated_at) VALUES
	('00000000-0000-7000-8000-000000000001', 'Administrador',
	 'Full access (protected system role)', TRUE, 0, now(), now()),
	('00000000-0000-7000-8000-000000000002', 'Comercial / Ventas',
	 'Sales advisor: sees and manages own leads', FALSE, 0, now(), now());

INSERT INTO role_permissions (role_id, permission_code)
SELECT '00000000-0000-7000-8000-000000000001', code FROM (VALUES
	('usuarios.ver'), ('usuarios.gestionar'), ('roles.ver'), ('roles.gestionar'),
	('leads.ver_propios'), ('leads.ver_todos'), ('leads.asignar'), ('leads.gestionar')) AS p (code);

INSERT INTO role_permissions (role_id, permission_code) VALUES
	('00000000-0000-7000-8000-000000000002', 'leads.ver_propios'),
	('00000000-0000-7000-8000-000000000002', 'leads.gestionar');
