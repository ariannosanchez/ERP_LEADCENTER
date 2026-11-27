package pe.leadcenter.erp.identity.infrastructure.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.leadcenter.erp.ErpApiApplication;

/**
 * Restarts the whole application several times on ONE Testcontainers database.
 *
 * <p>SAFETY: the datasource is always passed as highest-precedence command-line arguments pointing
 * at the container, so application.yml's localhost default can never be used.
 */
@Testcontainers
class BootstrapAdministratorRestartIT {

    private static final String EMAIL = "root@leadcenter.pe";
    private static final String PASSWORD = "Corre-Horse-Battery-9";
    private static final String OTHER_PASSWORD = "Another-Passw0rd-77";

    @Container
    static final PostgreSQLContainer DB =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Test
    void restartsKeepTheBootstrapStateConsistentAndNeverPrintThePassword() throws Exception {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(captured, true, StandardCharsets.UTF_8);
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        root.addAppender(logs);
        System.setOut(capture);
        System.setErr(capture);
        try {
            // 1. No variables: starts, no user.
            start("", "");
            assertThat(snapshot()).isEmpty();

            // 2. Valid variables: exactly one active administrator with a bcrypt hash.
            start(EMAIL, PASSWORD);
            List<String> afterCreate = snapshot();
            assertThat(afterCreate).hasSize(1);
            assertThat(afterCreate.getFirst())
                    .contains(EMAIL, "{bcrypt}", "|t|")
                    .doesNotContain(PASSWORD);

            // 3. Restart with other values: an administrator exists, rows are identical.
            start("someone-else@leadcenter.pe", OTHER_PASSWORD);
            assertThat(snapshot()).isEqualTo(afterCreate);

            // 4. Conflict: the only administrator is deactivated and its email stays taken.
            execute("update users set active = false");
            List<String> deactivated = snapshot();
            assertThat(deactivated).isNotEqualTo(afterCreate).hasSize(1);
            assertThatThrownBy(() -> start(EMAIL, OTHER_PASSWORD)).isInstanceOf(Exception.class);
            assertThat(snapshot()).isEqualTo(deactivated);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            root.detachAppender(logs);
        }
        String output =
                captured.toString(StandardCharsets.UTF_8)
                        + "\n"
                        + String.join(
                                "\n",
                                logs.list.stream()
                                        .map(ILoggingEvent::getFormattedMessage)
                                        .toList());
        assertThat(output).contains("Bootstrap administrator created");
        assertThat(output).doesNotContain(PASSWORD).doesNotContain(OTHER_PASSWORD);
    }

    private static void start(String email, String password) {
        try (ConfigurableApplicationContext context =
                SpringApplication.run(
                        ErpApiApplication.class,
                        "--spring.datasource.url=" + DB.getJdbcUrl(),
                        "--spring.datasource.username=" + DB.getUsername(),
                        "--spring.datasource.password=" + DB.getPassword(),
                        "--server.port=0",
                        "--erp.bootstrap.admin.email=" + email,
                        "--erp.bootstrap.admin.password=" + password)) {
            assertThat(context.isRunning()).isTrue();
        }
    }

    private static List<String> snapshot() throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet rs =
                        statement.executeQuery(
                                "select u.id, u.email, u.full_name, u.password_hash, u.active,"
                                        + " u.version, coalesce(string_agg(r.role_id::text, ','"
                                        + " order by r.role_id), '') from users u left join"
                                        + " user_roles r on r.user_id = u.id group by u.id"
                                        + " order by u.email")) {
            while (rs.next()) {
                StringBuilder row = new StringBuilder("|");
                for (int i = 1; i <= 7; i++) {
                    row.append(rs.getString(i)).append('|');
                }
                rows.add(row.toString());
            }
        }
        return rows;
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
    }
}
