package com.flickpick.importservice.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

// Pushes newly imported films into Ranking Service's catalog.
//
// Import Service holds no credential of its own: it forwards the end user's
// bearer token, so Ranking Service authenticates the same human who asked
// for the import. That keeps one auth mechanism in the system instead of
// two, at the cost of only being able to call Ranking Service while acting
// on a live user request (a background job would need a real service
// identity).
@Component
public class RankingClient {

    private final RestClient restClient;
    private final String baseUrl;

    public RankingClient(@Value("${ranking-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create();
        this.baseUrl = baseUrl;
    }

    public CatalogUpdateResponse pushMovies(List<MovieUpsertRequest> movies, String bearerToken) {
        if (movies.isEmpty()) {
            return new CatalogUpdateResponse(0, 0);
        }
        try {
            return restClient
                    .post()
                    .uri(baseUrl + "/api/movies")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(movies)
                    .retrieve()
                    .body(CatalogUpdateResponse.class);
        } catch (RestClientException e) {
            // The films are already saved locally at this point, so surface
            // this as a real failure rather than swallowing it — otherwise
            // the user would be told the import succeeded while nothing
            // became rankable.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Imported films could not be sent to Ranking Service: " + e.getMessage());
        }
    }

    public record CatalogUpdateResponse(int added, int updated) {
    }
}
