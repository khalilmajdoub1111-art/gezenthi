package tn.gezenthi.supplier;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Background sync with the fake supplier. Not transactional (the sync runs on its own thread),
 * so everything it writes is deleted afterwards.
 */
@SpringBootTest
@Import(FakeSupplierConfig.class)
class SupplierSyncTests {

    @Autowired SupplierSyncService sync;
    @Autowired SupplierHotelRepository hotels;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from supplier_hotel where supplier_code = ?", FakeSupplierConfig.CODE);
        jdbc.update("delete from supplier_sync_log where supplier_code = ?", FakeSupplierConfig.CODE);
    }

    @Test
    void syncRunsInBackgroundSavesEachCityAndLogsProgress() throws Exception {
        SupplierSyncService.SyncResult started = sync.start(FakeSupplierConfig.CODE, "test");
        assertThat(started.status()).isEqualTo("RUNNING");

        String status = waitForEnd();
        assertThat(status).isEqualTo("SUCCESS");
        assertThat(hotels.countBySupplierCodeAndActiveTrue(FakeSupplierConfig.CODE)).isEqualTo(2);
        assertThat(hotels.findActiveInCity(FakeSupplierConfig.CODE, "sousse")).hasSize(1);
        assertThat(jdbc.queryForObject("select message from supplier_sync_log where supplier_code = ?", String.class,
                FakeSupplierConfig.CODE)).contains("2 hôtels synchronisés");
    }

    @Test
    void secondSyncWhileRunningIsRefused() {
        jdbc.update("insert into supplier_sync_log (id, supplier_code, started_at, status) values (gen_random_uuid(), ?, now(), 'RUNNING')",
                FakeSupplierConfig.CODE);
        assertThat(sync.start(FakeSupplierConfig.CODE, "test").message()).contains("déjà en cours");
    }

    private String waitForEnd() throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            String s = jdbc.queryForObject("select status from supplier_sync_log where supplier_code = ? order by started_at desc limit 1",
                    String.class, FakeSupplierConfig.CODE);
            if (!"RUNNING".equals(s)) return s;
            Thread.sleep(100);
        }
        return "TIMEOUT";
    }
}
