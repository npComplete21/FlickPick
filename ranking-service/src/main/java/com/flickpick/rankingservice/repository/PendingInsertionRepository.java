package com.flickpick.rankingservice.repository;

import com.flickpick.rankingservice.domain.PendingInsertion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingInsertionRepository extends JpaRepository<PendingInsertion, Long> {

    Optional<PendingInsertion> findByUserId(Long userId);
}
