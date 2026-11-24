package tn.gezenthi.supplier;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Copies a supplier's static hotel list into supplier_hotel.
 * Hotels no longer returned are deactivated (not deleted) so history stays traceable.
 * Every run is recorded in supplier_sync_log with who triggered it and the outcome.
 */
@Service
public class SupplierSyncService {

    private static final Logger log = LoggerFactory.getLogger(SupplierSyncService.class);

    public record SyncResult(String supplier, String status, int hotels, String message) {
    }

    private final SupplierRegistry registry;
    private final SupplierHotelRepository hotels;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    SupplierSyncService(SupplierRegistry registry, SupplierHotelRepository hotels, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.registry = registry;
        this.hotels = hotels;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public SyncResult sync(String code, String triggeredBy) {
        HotelSupplier supplier = registry.find(code).orElseThrow(() -> new SupplierException("Fournisseur inconnu : " + code));
        UUID logId = UUID.randomUUID();
        jdbc.update("insert into supplier_sync_log (id, supplier_code, started_at, status, triggered_by) values (?, ?, now(), 'RUNNING', ?)",
                logId, code, triggeredBy);
        try {
            List<HotelSupplier.StaticHotel> fetched = supplier.fetchHotels(registry.settings(code).getCountries());
            int count = tx.execute(status -> upsert(code, fetched));
            finish(logId, "SUCCESS", count, count + " hôtels synchronisés.");
            log.info("Supplier sync {} OK: {} hotels (by {})", code, count, triggeredBy);
            return new SyncResult(code, "SUCCESS", count, count + " hôtels synchronisés.");
        } catch (RuntimeException e) {
            String msg = e instanceof SupplierException ? e.getMessage() : "Erreur inattendue (" + e.getClass().getSimpleName() + ")";
            finish(logId, "FAILED", 0, msg);
            log.warn("Supplier sync {} FAILED: {}", code, msg);
            return new SyncResult(code, "FAILED", 0, msg);
        }
    }

    private int upsert(String code, List<HotelSupplier.StaticHotel> fetched) {
        Instant now = Instant.now();
        Map<String, SupplierHotel> existing = hotels.findBySupplierCode(code).stream()
                .collect(Collectors.toMap(SupplierHotel::getExternalId, Function.identity()));
        Map<String, HotelSupplier.StaticHotel> unique = new HashMap<>();
        fetched.stream().filter(h -> !h.externalId().isBlank()).forEach(h -> unique.put(h.externalId(), h));

        unique.values().forEach(h -> {
            SupplierHotel row = existing.getOrDefault(h.externalId(), new SupplierHotel(code, h.externalId()));
            row.refresh(h, now);
            hotels.save(row);
        });
        existing.values().stream().filter(r -> !unique.containsKey(r.getExternalId())).forEach(SupplierHotel::deactivate);
        return unique.size();
    }

    private void finish(UUID logId, String status, int count, String message) {
        String safe = message == null ? null : message.substring(0, Math.min(500, message.length()));
        jdbc.update("update supplier_sync_log set finished_at = now(), status = ?, hotels = ?, message = ? where id = ?",
                status, count, safe, logId);
    }
}
