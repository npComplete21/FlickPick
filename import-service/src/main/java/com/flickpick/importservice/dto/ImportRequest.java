package com.flickpick.importservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ImportRequest(
        // Letterboxd usernames are letters, digits, underscores and hyphens.
        // Constrained here because this value is interpolated straight into
        // the feed URL we fetch.
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String letterboxdUsername) {
}
