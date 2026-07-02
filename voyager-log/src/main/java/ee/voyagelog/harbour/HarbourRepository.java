package ee.voyagelog.harbour;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HarbourRepository extends JpaRepository<Harbour, Long> {

    Optional<Harbour> findFirstByNameIgnoreCaseContaining(String namePart);
}
