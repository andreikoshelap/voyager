package ee.voyagelog.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findFirstBySkipperIdAndStatusIn(Long skipperId, Collection<TripStatus> statuses);

    List<Trip> findByStatusAndEtaReturnBefore(TripStatus status, Instant cutoff);

    List<Trip> findByStatusAndOverdueAtBefore(TripStatus status, Instant cutoff);
}
