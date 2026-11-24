package tn.gezenthi.catalog;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Admin REST endpoints shared by every offer type. Subclasses only set the URL:
 * GET list · GET /{id} · POST · PUT /{id} · DELETE /{id}.
 *
 * SECURITY TODO: /api/admin/** is not authenticated yet (local dev only).
 * Spring Security with real admin accounts must be added before any deployment.
 */
public abstract class CrudController<D> {

    private final CatalogCrud<?, D> crud;

    protected CrudController(CatalogCrud<?, D> crud) {
        this.crud = crud;
    }

    @GetMapping
    public List<D> list() {
        return crud.findAll();
    }

    @GetMapping("/{id}")
    public D get(@PathVariable UUID id) {
        return crud.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public D create(@Valid @RequestBody D body) {
        return crud.create(body);
    }

    @PutMapping("/{id}")
    public D update(@PathVariable UUID id, @Valid @RequestBody D body) {
        return crud.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        crud.delete(id);
    }
}
