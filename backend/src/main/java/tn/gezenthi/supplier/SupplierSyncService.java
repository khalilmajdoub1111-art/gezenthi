package tn.gezenthi.supplier;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Copies a supplier's static hotel list into supplier_hotel, in the BACKGROUND
 * (suppliers rate-limit these calls, so a full sync can take many minutes).
 *
 * - Each city is saved in its own transaction: an interruption keeps what was already fetched.
 * - Progress is written to supplier_sync_log (status RUNNING, "Ville 12/48…") for the admin page.
 * - Hotels no longer returned are deactivated (not deleted) — only after a COMPLETE run.
 * - One sync per supplier at a time.
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

    /** A sync can't survive a restart: mark leftovers as failed so the admin isn't misled. */
    @EventListener(ApplicationReadyEvent.class)
    void closeInterruptedRuns() {
        int n = jdbc.update("update supplier_sync_log set status = 'FAILED', finished_at = now(), "
                + "message = 'Interrompue (redémarrage du serveur). Relancez la synchronisation.' where status = 'RUNNING'");
        if (n > 0) log.warn("{} interrupted supplier sync(s) marked as FAILED", n);
    }

    /** Starts a background sync and returns immediately. */
    public SyncResult start(String code, String triggeredBy) {
        HotelSupplier supplier = registry.find(code).orElseThrow(() -> new SupplierException("Fournisseur inconnu : " + code));
        UUID logId;
        synchronized (this) {
            Integer running = jdbc.queryForObject(
                    "select count(*) from supplier_sync_log where supplier_code = ? and status = 'RUNNING'", Integer.class, code);
            if (running != null && running > 0) {
                return new SyncResult(code, "RUNNING", 0, "Une synchronisation est déjà en cours.");
            }
            logId = UUID.randomUUID();
            jdbc.update("insert into supplier_sync_log (id, supplier_code, started_at, status, message, triggered_by) "
                    + "values (?, ?, now(), 'RUNNING', 'Démarrage…', ?)", logId, code, triggeredBy);
        }
        Thread.ofVirtual().name("supplier-sync-" + code).start(() -> run(supplier, logId, triggeredBy));
        return new SyncResult(code, "RUNNING", 0, "Synchronisation démarrée. Elle peut prendre plusieurs minutes (limite du fournisseur).");
    }

    private void run(HotelSupplier supplier, UUID logId, String triggeredBy) {
        String code = supplier.code();
        Set<String> seen = new HashSet<>();
        try {
            supplier.fetchHotels(registry.settings(code).getCountries(), (city, index, total, batch) -> {
                tx.executeWithoutResult(status -> upsert(code, batch));
                batch.forEach(h -> seen.add(h.externalId()));
                progress(logId, seen.size(), "Ville " + index + "/" + total + " : " + city + " — " + seen.size() + " hôtels");
            });
            int deactivated = tx.execute(status -> deactivateMissing(code, seen));
            String msg = seen.size() + " hôtels synchronisés" + (deactivated > 0 ? ", " + deactivated + " retirés" : "") + ".";
            finish(logId, "SUCCESS", seen.size(), msg);
            log.info("Supplier sync {} OK: {} (by {})", code, msg, triggeredBy);
        } catch (RuntimeException e) {
            String reason = e instanceof SupplierException ? e.getMessage() : "Erreur inattendue (" + e.getClass().getSimpleName() + ")";
            String msg = reason + (seen.isEmpty() ? "" : " — " + seen.size() + " hôtels déjà enregistrés sont conservés.");
            finish(logId, "FAILED", seen.size(), msg);
            log.warn("Supplier sync {} FAILED: {}", code, reason);
        }
    }

    private void upsert(String code, List<HotelSupplier.StaticHotel> batch) {
        if (batch.isEmpty()) return;
        Instant now = Instant.now();
        Map<String, SupplierHotel> existing = hotels.findBySupplierCode(code).stream()
                .collect(Collectors.toMap(SupplierHotel::getExternalId, Function.identity()));
        batch.stream().filter(h -> !h.externalId().isBlank()).forEach(h -> {
            SupplierHotel row = existing.getOrDefault(h.externalId(), new SupplierHotel(code, h.externalId()));
            row.refresh(h, now);
            hotels.save(row);
        });
    }

    private int deactivateMissing(String code, Set<String> seen) {
        List<SupplierHotel> missing = hotels.findBySupplierCode(code).stream()
                .filter(r -> r.isActive() && !seen.contains(r.getExternalId())).toList();
        missing.forEach(SupplierHotel::deactivate);
        hotels.saveAll(missing);
        return missing.size();
    }

    private void progress(UUID logId, int count, String message) {
        jdbc.update("update supplier_sync_log set hotels = ?, message = ? where id = ?", count, truncate(message), logId);
    }

    private void finish(UUID logId, String status, int count, String message) {
        jdbc.update("update supplier_sync_log set finished_at = now(), status = ?, hotels = ?, message = ? where id = ?",
                status, count, truncate(message), logId);
    }

    private static String truncate(String s) {
        return s == null ? null : s.substring(0, Math.min(500, s.length()));
    }
}
