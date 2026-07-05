package ee.voyagelog.skipper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkipperRepository extends JpaRepository<Skipper, Long> {

    Optional<Skipper> findByTelegramChatId(Long telegramChatId);
}
