package pe.leadcenter.erp.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.leadcenter.erp.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuditorFallbackIT {

    @Autowired SampleRecordRepository repository;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String name) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                name, "n/a", AuthorityUtils.createAuthorityList("ROLE_X")));
    }

    @Test
    void nonUuidNameLeavesCreatedByNull() {
        authenticateAs("admin@example.com");

        assertThat(repository.saveAndFlush(new SampleRecord("x")).getCreatedBy()).isNull();
    }

    @Test
    void uuidNameIsUsedAsAuditor() {
        UUID actor = UUID.randomUUID();
        authenticateAs(actor.toString());

        assertThat(repository.saveAndFlush(new SampleRecord("x")).getCreatedBy()).isEqualTo(actor);
    }

    @Test
    void softDeleteIsRecordedAsEnversModification() {
        UUID id = repository.saveAndFlush(new SampleRecord("x")).getId();

        repository.deleteById(id);
        repository.flush();

        List<Integer> revTypes =
                jdbc.queryForList(
                        "select revtype from sample_record_aud where id = ? order by rev",
                        Integer.class,
                        id);
        assertThat(revTypes).containsExactly(0, 2);
    }
}
