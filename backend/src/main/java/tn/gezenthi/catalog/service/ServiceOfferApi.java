package tn.gezenthi.catalog.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.catalog.CrudController;

/** Wiring for Visa / CTN / TRE services: CRUD service, admin endpoints at /api/admin/services. */
public final class ServiceOfferApi {

    private ServiceOfferApi() {
    }

    @Configuration
    static class Config {
        @Bean
        CatalogCrud<ServiceOffer, ServiceOfferDto> serviceCrud(ServiceOfferRepository repo) {
            return new CatalogCrud<>("Service", repo, ServiceOffer::new, ServiceOffer::apply, ServiceOfferDto::of);
        }
    }

    @RestController
    @RequestMapping("/api/admin/services")
    static class AdminController extends CrudController<ServiceOfferDto> {
        AdminController(CatalogCrud<ServiceOffer, ServiceOfferDto> crud) {
            super(crud);
        }
    }
}
