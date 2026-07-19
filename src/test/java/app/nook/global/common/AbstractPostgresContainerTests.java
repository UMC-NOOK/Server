package app.nook.global.common;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresContainerTests {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("nook_test")
            .withUsername("test")
            .withPassword("test")
            .withEnv("POSTGRES_INITDB_ARGS", "--locale=C --encoding=UTF8");

    static {
        POSTGRES.start();
    }
}
