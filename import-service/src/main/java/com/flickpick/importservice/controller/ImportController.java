package com.flickpick.importservice.controller;

import com.flickpick.importservice.dto.ImportRequest;
import com.flickpick.importservice.dto.ImportResultResponse;
import com.flickpick.importservice.dto.ImportedFilmResponse;
import com.flickpick.importservice.service.ImportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    // The raw Authorization header is taken here and handed down so it can
    // be forwarded to Ranking Service. The JWT filter has already verified
    // it by this point; this is the same token travelling one hop further,
    // not a second credential.
    @PostMapping
    public ResponseEntity<ImportResultResponse> importFromLetterboxd(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @Valid @RequestBody ImportRequest request) {
        return ResponseEntity.ok(importService.importFromLetterboxd(
                userId(authentication), request.letterboxdUsername(), bearerToken));
    }

    @GetMapping
    public List<ImportedFilmResponse> imported(Authentication authentication) {
        return importService.getImportedFilms(userId(authentication)).stream()
                .map(ImportedFilmResponse::from)
                .toList();
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
