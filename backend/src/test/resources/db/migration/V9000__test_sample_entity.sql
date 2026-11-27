-- Test-only schema for the persistence foundation tests (never shipped: lives in src/test).
CREATE TABLE sample_record (
	id         UUID NOT NULL PRIMARY KEY,
	name       TEXT NOT NULL,
	deleted_at TIMESTAMP WITH TIME ZONE,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,
	created_by UUID,
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
	updated_by UUID
);

CREATE TABLE sample_record_aud (
	id         UUID NOT NULL,
	rev        BIGINT NOT NULL REFERENCES revinfo (rev),
	revtype    SMALLINT,
	name       TEXT,
	created_at TIMESTAMP WITH TIME ZONE,
	created_by UUID,
	updated_at TIMESTAMP WITH TIME ZONE,
	updated_by UUID,
	PRIMARY KEY (id, rev)
);
