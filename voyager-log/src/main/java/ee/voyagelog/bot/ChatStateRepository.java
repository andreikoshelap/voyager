package ee.voyagelog.bot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatStateRepository extends JpaRepository<ChatState, Long> {
}
