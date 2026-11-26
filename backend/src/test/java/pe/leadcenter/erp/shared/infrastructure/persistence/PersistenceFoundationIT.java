package pe.leadcenter.erp.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import pe.leadcenter.erp.TestcontainersConfiguration;
import pe.leadcenter.erp.shared.security.AuthenticatedUser;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PersistenceFoundationIT {

    @Autowired SampleRecordRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired Environment environment;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(UUID id) {
        AuthenticatedUser user = () -> id;
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(user, "n/a", "ROLE_X"));
    }

    @Test
    void systemWritesWithoutAuthenticationLeaveAuditorEmpty() {
        SecurityContextHolder.clearContext();
        SampleRecord saved = repository.saveAndFlush(new SampleRecord("system"));

        assertThat(saved.getCreatedBy()).isNull();
        assertThat(saved.getUpdatedBy()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void anonymousPrincipalIsTreatedAsSystem() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.authentication
                                .AnonymousAuthenticationToken(
                                "key",
                                "anonymousUser",
                                org.springframework.security.core.authority.AuthorityUtils
                                        .createAuthorityList("ROLE_ANONYMOUS")));
        SampleRecord saved = repository.saveAndFlush(new SampleRecord("anon"));

        assertThat(saved.getCreatedBy()).isNull();
    }

    @Test
    void authenticatedWritesRecordCreatorAndUpdater() {
        UUID creator = UUID.randomUUID();
        UUID editor = UUID.randomUUID();

        authenticateAs(creator);
        UUID id = repository.saveAndFlush(new SampleRecord("before")).getId();

        authenticateAs(editor);
        tx.executeWithoutResult(s -> repository.findById(id).orElseThrow().rename("after"));

        SampleRecord reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getCreatedBy()).isEqualTo(creator);
        assertThat(reloaded.getUpdatedBy()).isEqualTo(editor);
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(reloaded.getCreatedAt());
    }

    @Test
    void deleteIsSoftAndHidesTheRowFromQueries() {
        UUID id = repository.saveAndFlush(new SampleRecord("to-delete")).getId();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
        Integer physicalRows =
                jdbc.queryForObject(
                        "select count(*) from sample_record where id = ? and deleted_at is not null",
                        Integer.class,
                        id);
        assertThat(physicalRows).isEqualTo(1);
    }

    @Test
    void envers_recordsRevisionsWithTheAuthenticatedUser() {
        UUID actor = UUID.randomUUID();
        authenticateAs(actor);
        UUID id = repository.saveAndFlush(new SampleRecord("v1")).getId();
        tx.executeWithoutResult(s -> repository.findById(id).orElseThrow().rename("v2"));

        Integer revisions =
                jdbc.queryForObject(
                        "select count(*) from sample_record_aud where id = ?", Integer.class, id);
        assertThat(revisions).isEqualTo(2);

        UUID modifiedBy =
                jdbc.queryForObject(
                        "select r.modified_by from sample_record_aud a join revinfo r on r.rev = a.rev"
                                + " where a.id = ? order by a.rev limit 1",
                        UUID.class,
                        id);
        assertThat(modifiedBy).isEqualTo(actor);
    }

    @Test
    void timeZoneIsUtc() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"))
                .isEqualTo("UTC");
    }
}
