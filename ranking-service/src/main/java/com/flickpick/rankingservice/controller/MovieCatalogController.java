package com.flickpick.rankingservice.controller;

import com.flickpick.rankingservice.dto.CatalogUpdateResponse;
import com.flickpick.rankingservice.dto.MovieUpsertRequest;
import com.flickpick.rankingservice.service.MovieCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Called by Import Service, not the browser. It's still guarded by the same
// JWT rule as everything else: Import Service forwards the end user's token
// rather than holding a credential of its own, so the catalog can only grow
// on behalf of a real authenticated user.
@RestController
@RequestMapping("/api/movies")
@Validated
public class MovieCatalogController {

    private final MovieCatalogService movieCatalogService;

    public MovieCatalogController(MovieCatalogService movieCatalogService) {
        this.movieCatalogService = movieCatalogService;
    }

    // The user identity comes from the forwarded JWT, never from the body —
    // so a push can only ever record ratings for the authenticated caller.
    @PostMapping
    public ResponseEntity<CatalogUpdateResponse> upsert(
            Authentication authentication, @RequestBody List<@Valid MovieUpsertRequest> requests) {
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(movieCatalogService.upsertAll(userId, requests));
    }
}
