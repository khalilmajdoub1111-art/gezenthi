package tn.gezenthi.catalog.trip;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.catalog.CrudController;

/** Wiring for trips: CRUD service, admin endpoints at /api/admin/trips. */
public final class TripApi {

    private TripApi() {
    }

    @Configuration
    static class Config {
        @Bean
        CatalogCrud<Trip, TripDto> tripCrud(TripRepository repo) {
            return new CatalogCrud<>("Voyage", repo, Trip::new, Trip::apply, TripDto::of);
        }
    }

    @RestController
    @RequestMapping("/api/admin/trips")
    static class AdminController extends CrudController<TripDto> {
        AdminController(CatalogCrud<Trip, TripDto> crud) {
            super(crud);
        }
    }
}
