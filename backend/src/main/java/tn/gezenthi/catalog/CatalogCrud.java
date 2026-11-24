package tn.gezenthi.catalog;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generic create/read/update/delete for one offer type.
 * One instance per type is declared in {@link CatalogConfig}; the mapping between
 * entity and DTO is passed in, so each type only defines its fields once.
 *
 * @param <E> JPA entity
 * @param <D> API DTO (request and response body)
 */
public class CatalogCrud<E extends CatalogEntity, D> {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final String label;
    private final JpaRepository<E, UUID> repository;
    private final Supplier<E> factory;
    private final BiConsumer<E, D> applier;
    private final Function<E, D> mapper;

    public CatalogCrud(String label, JpaRepository<E, UUID> repository, Supplier<E> factory,
                       BiConsumer<E, D> applier, Function<E, D> mapper) {
        this.label = label;
        this.repository = repository;
        this.factory = factory;
        this.applier = applier;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<D> findAll() {
        return repository.findAll(NEWEST_FIRST).stream().map(mapper).toList();
    }

    /** Public site: published items only. Catalogue is small, filtering in memory is fine. */
    @Transactional(readOnly = true)
    public List<D> findPublished() {
        return repository.findAll(NEWEST_FIRST).stream()
                .filter(CatalogEntity::isPublished)
                .map(mapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public D get(UUID id) {
        return mapper.apply(load(id));
    }

    @Transactional
    public D create(D dto) {
        E entity = factory.get();
        applier.accept(entity, dto);
        return mapper.apply(repository.saveAndFlush(entity));
    }

    @Transactional
    public D update(UUID id, D dto) {
        E entity = load(id);
        applier.accept(entity, dto);
        return mapper.apply(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(load(id));
    }

    private E load(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, label + " introuvable : " + id));
    }
}
