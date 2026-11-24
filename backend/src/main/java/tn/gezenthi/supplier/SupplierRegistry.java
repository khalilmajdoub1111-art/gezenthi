package tn.gezenthi.supplier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tn.gezenthi.supplier.ostravel.OsTravelSupplier;
import tools.jackson.databind.json.JsonMapper;

/**
 * All known suppliers: those declared in configuration (gezenthi.suppliers.*) plus any
 * HotelSupplier bean (used by tests). Adding a supplier on the same platform = config only;
 * a new platform = one new adapter class + a "type" here.
 */
@Component
@EnableConfigurationProperties(SupplierProperties.class)
public class SupplierRegistry {

    private final Map<String, HotelSupplier> suppliers = new LinkedHashMap<>();
    private final Map<String, SupplierProperties.Supplier> settings = new LinkedHashMap<>();

    SupplierRegistry(SupplierProperties props, JsonMapper json, ObjectProvider<HotelSupplier> extraBeans) {
        props.getSuppliers().forEach((code, cfg) -> {
            HotelSupplier adapter = switch (cfg.getType()) {
                case "os-travel" -> new OsTravelSupplier(code, cfg, json);
                default -> throw new IllegalStateException("Unknown supplier type '" + cfg.getType() + "' for " + code);
            };
            suppliers.put(code, adapter);
            settings.put(code, cfg);
        });
        extraBeans.orderedStream().forEach(s -> {
            suppliers.put(s.code(), s);
            settings.computeIfAbsent(s.code(), c -> {
                SupplierProperties.Supplier cfg = new SupplierProperties.Supplier();
                cfg.setEnabled(true);
                return cfg;
            });
        });
    }

    public Collection<HotelSupplier> all() {
        return suppliers.values();
    }

    public Optional<HotelSupplier> find(String code) {
        return Optional.ofNullable(suppliers.get(code));
    }

    public SupplierProperties.Supplier settings(String code) {
        return settings.get(code);
    }

    /** Suppliers that take part in public searches. */
    public List<HotelSupplier> activeForSearch() {
        return suppliers.values().stream()
                .filter(s -> settings.get(s.code()).isEnabled() && s.configured())
                .toList();
    }
}
