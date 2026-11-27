package pe.leadcenter.erp.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.leadcenter.erp.TestcontainersConfiguration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exports the OpenAPI document served at {@code /v3/api-docs} so the frontend can generate its
 * client from it. The target is taken from the {@code openapi.export.path} system property (default
 * {@code target/openapi.json}).
 *
 * <p>Sync command: {@code ./mvnw -B -Dtest=OpenApiExportIT
 * -Dopenapi.export.path=../frontend/openapi/erp-api.json test} from {@code backend/}, then {@code
 * pnpm api:generate} from {@code frontend/}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiExportIT {

    private static final String DEFAULT_EXPORT_PATH = "target/openapi.json";

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser
    void exportsOpenApiDocument() throws Exception {
        String body =
                mvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        // Pretty-printed so that snapshot diffs stay reviewable.
        JsonMapper mapper = JsonMapper.builder().build();
        String pretty =
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body))
                        + "\n";

        Path target = Path.of(System.getProperty("openapi.export.path", DEFAULT_EXPORT_PATH));
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(target, pretty, StandardCharsets.UTF_8);

        assertThat(target).exists();
        assertThat(pretty).contains("\"openapi\"");
    }
}
