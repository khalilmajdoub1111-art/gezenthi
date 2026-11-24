package tn.gezenthi.catalog.omra;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OmraProgramRepository extends JpaRepository<OmraProgram, UUID> {
}
