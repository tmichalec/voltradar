package sk.brutech.voltradar.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sk.brutech.voltradar.cache.ConnectorStatusCache;
import sk.brutech.voltradar.ingestion.zse.ZseDataIngestionService;
import sk.brutech.voltradar.persistence.service.ChargingLocationPersistenceService;
import sk.brutech.voltradar.scheduler.ZseDataRefreshScheduler;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @MockitoBean
    private ZseDataIngestionService zseDataIngestionService;

    @MockitoBean
    private ChargingLocationPersistenceService chargingLocationPersistenceService;

    @MockitoBean
    private ConnectorStatusCache connectorStatusCache;

    @MockitoBean
    private ZseDataRefreshScheduler zseDataRefreshScheduler;

    @Test
    void flywayMigrationsAreApplied() {
        assertThat(flyway).isNotNull();
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertThat(appliedMigrations).isNotEmpty();
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("2");
    }
}
