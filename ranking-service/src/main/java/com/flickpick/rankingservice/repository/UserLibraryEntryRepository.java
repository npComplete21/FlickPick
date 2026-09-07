package com.flickpick.rankingservice.repository;

import com.flickpick.rankingservice.domain.UserLibraryEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLibraryEntryRepository extends JpaRepository<UserLibraryEntry, Long> {

    Optional<UserLibraryEntry> findByUserIdAndMovieId(Long userId, Long movieId);

    List<UserLibraryEntry> findByUserId(Long userId);

    long countByUserId(Long userId);
}
