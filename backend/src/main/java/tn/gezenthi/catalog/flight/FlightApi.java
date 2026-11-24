package tn.gezenthi.catalog.flight;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.catalog.CrudController;

/** Wiring for flight deals: CRUD service, admin endpoints at /api/admin/flights. */
public final class FlightApi {

    private FlightApi() {
    }

    @Configuration
    static class Config {
        @Bean
        CatalogCrud<FlightDeal, FlightDealDto> flightCrud(FlightDealRepository repo) {
            return new CatalogCrud<>("Vol", repo, FlightDeal::new, FlightDeal::apply, FlightDealDto::of);
        }
    }

    @RestController
    @RequestMapping("/api/admin/flights")
    static class AdminController extends CrudController<FlightDealDto> {
        AdminController(CatalogCrud<FlightDeal, FlightDealDto> crud) {
            super(crud);
        }
    }
}
