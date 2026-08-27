package com.flickpick.importservice.repository;

import com.flickpick.importservice.domain.ImportedFilm;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportedFilmRepository extends JpaRepository<ImportedFilm, Long> {

    List<ImportedFilm> findByUserIdOrderByImportedAtDesc(Long userId);

    List<ImportedFilm> findByUserIdAndTmdbIdIn(Long userId, List<Long> tmdbIds);
}
