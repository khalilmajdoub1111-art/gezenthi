package tn.gezenthi.catalog.omra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.catalog.CrudController;

/** Wiring for Omra programmes: CRUD service, admin endpoints at /api/admin/omra. */
public final class OmraApi {

    private OmraApi() {
    }

    @Configuration
    static class Config {
        @Bean
        CatalogCrud<OmraProgram, OmraProgramDto> omraCrud(OmraProgramRepository repo) {
            return new CatalogCrud<>("Programme Omra", repo, OmraProgram::new, OmraProgram::apply, OmraProgramDto::of);
        }
    }

    @RestController
    @RequestMapping("/api/admin/omra")
    static class AdminController extends CrudController<OmraProgramDto> {
        AdminController(CatalogCrud<OmraProgram, OmraProgramDto> crud) {
            super(crud);
        }
    }
}
