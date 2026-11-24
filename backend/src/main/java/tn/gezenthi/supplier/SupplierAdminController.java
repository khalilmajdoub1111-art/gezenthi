package tn.gezenthi.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Back-office view of suppliers: status, connection test, static data sync, sync history. */
@RestController
@RequestMapping("/api/admin/suppliers")
class SupplierAdminController {

    record SupplierStatus(String code, String name, String type, boolean configured, boolean enabled,
                          boolean bookingEnabled, BigDecimal markupPercent, List<String> countries,
                          long cachedHotels, Map<String, Object> lastSync) {
    }

    record ActionResult(boolean ok, String message) {
    }

    private final SupplierRegistry registry;
    private final SupplierSyncService sync;
    private final SupplierHotelRepository hotels;
    private final JdbcTemplate jdbc;

    SupplierAdminController(SupplierRegistry registry, SupplierSyncService sync, SupplierHotelRepository hotels, JdbcTemplate jdbc) {
        this.registry = registry;
        this.sync = sync;
        this.hotels = hotels;
        this.jdbc = jdbc;
    }

    @GetMapping
    List<SupplierStatus> list() {
        return registry.all().stream().map(s -> {
            SupplierProperties.Supplier cfg = registry.settings(s.code());
            List<Map<String, Object>> last = jdbc.queryForList(
                    "select status, hotels, message, started_at, finished_at, triggered_by from supplier_sync_log "
                            + "where supplier_code = ? order by started_at desc limit 1", s.code());
            return new SupplierStatus(s.code(), s.displayName(), cfg.getType(), s.configured(), cfg.isEnabled(),
                    cfg.isBookingEnabled(), cfg.getMarkupPercent(), cfg.getCountries(),
                    hotels.countBySupplierCodeAndActiveTrue(s.code()), last.isEmpty() ? null : last.getFirst());
        }).toList();
    }

    @PostMapping("/{code}/test")
    ActionResult test(@PathVariable String code) {
        HotelSupplier s = supplier(code);
        try {
            return new ActionResult(true, s.testConnection());
        } catch (SupplierException e) {
            return new ActionResult(false, e.getMessage());
        }
    }

    @PostMapping("/{code}/sync")
    ActionResult sync(@PathVariable String code, Authentication auth) {
        supplier(code);
        SupplierSyncService.SyncResult r = sync.sync(code, auth == null ? "system" : auth.getName());
        return new ActionResult("SUCCESS".equals(r.status()), r.message());
    }

    @GetMapping("/{code}/sync-log")
    List<Map<String, Object>> syncLog(@PathVariable String code) {
        supplier(code);
        return jdbc.queryForList("select status, hotels, message, started_at, finished_at, triggered_by from supplier_sync_log "
                + "where supplier_code = ? order by started_at desc limit 20", code);
    }

    private HotelSupplier supplier(String code) {
        return registry.find(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur inconnu"));
    }
}
