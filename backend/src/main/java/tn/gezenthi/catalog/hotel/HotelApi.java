package tn.gezenthi.catalog.hotel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.catalog.CrudController;

/** Wiring for hotels: repository, CRUD service, admin endpoints at /api/admin/hotels. */
public final class HotelApi {

    private HotelApi() {
    }

    @Configuration
    static class Config {
        @Bean
        CatalogCrud<Hotel, HotelDto> hotelCrud(HotelRepository repo) {
            return new CatalogCrud<>("Hôtel", repo, Hotel::new, Hotel::apply, HotelDto::of);
        }
    }

    @RestController
    @RequestMapping("/api/admin/hotels")
    static class AdminController extends CrudController<HotelDto> {
        AdminController(CatalogCrud<Hotel, HotelDto> crud) {
            super(crud);
        }
    }
}
