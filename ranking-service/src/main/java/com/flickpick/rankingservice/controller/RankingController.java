package com.flickpick.rankingservice.controller;

import com.flickpick.rankingservice.dto.ComparisonResponse;
import com.flickpick.rankingservice.dto.RankingEntryResponse;
import com.flickpick.rankingservice.dto.SubmitComparisonRequest;
import com.flickpick.rankingservice.service.RankingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/next-comparison")
    public ResponseEntity<ComparisonResponse> nextComparison(Authentication authentication) {
        return rankingService.getNextComparison(userId(authentication))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/compare")
    public ResponseEntity<Void> compare(
            Authentication authentication, @Valid @RequestBody SubmitComparisonRequest request) {
        rankingService.submitComparison(userId(authentication), request.winnerMovieId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<RankingEntryResponse> rankings(Authentication authentication) {
        return rankingService.getRankings(userId(authentication)).stream()
                .map(RankingEntryResponse::from)
                .toList();
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
