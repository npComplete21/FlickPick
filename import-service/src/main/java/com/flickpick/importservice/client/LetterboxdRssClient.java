package com.flickpick.importservice.client;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Fetches and parses a public Letterboxd RSS feed.
//
// Parsed with the JDK's own DOM parser rather than adding a feed library:
// the document is small, the shape is fixed, and the only awkward part
// (pulling a poster out of an HTML description) is a regex either way.
@Component
public class LetterboxdRssClient {

    private static final String LETTERBOXD_NS = "https://letterboxd.com";
    private static final String TMDB_NS = "https://themoviedb.org";

    // The poster is only available as an <img> inside the description's
    // HTML — Letterboxd exposes no dedicated poster element.
    private static final Pattern POSTER_PATTERN =
            Pattern.compile("<img src=\"([^\"]+)\"");

    private final RestClient restClient;
    private final String baseUrl;

    // Built from RestClient's own static factory rather than an injected
    // RestClient.Builder bean: Boot 4.1's starters here don't contribute
    // that auto-configuration, and spring-web's factory has no such
    // dependency to break.
    public LetterboxdRssClient(@Value("${letterboxd.base-url}") String baseUrl) {
        this.restClient = RestClient.create();
        this.baseUrl = baseUrl;
    }

    public List<LetterboxdFilm> fetchWatchedFilms(String username) {
        String body = fetchFeed(username);
        return parse(body);
    }

    private String fetchFeed(String username) {
        String url = "%s/%s/rss/".formatted(baseUrl, username.trim());
        try {
            return restClient.get().uri(url).retrieve().body(String.class);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Letterboxd serves an HTML 404 page for unknown usernames.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No public Letterboxd feed for user '" + username + "'");
        } catch (org.springframework.web.client.RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not reach Letterboxd: " + e.getMessage());
        }
    }

    private List<LetterboxdFilm> parse(String xml) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // This XML comes from outside our system, so disable external
            // entity resolution — otherwise a hostile feed could make the
            // parser fetch local files or internal URLs (XXE).
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Letterboxd returned a feed we couldn't parse");
        }

        List<LetterboxdFilm> films = new ArrayList<>();
        NodeList items = document.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            if (items.item(i) instanceof Element item) {
                toFilm(item).ifPresent(films::add);
            }
        }
        return films;
    }

    private java.util.Optional<LetterboxdFilm> toFilm(Element item) {
        String title = textOf(item, LETTERBOXD_NS, "filmTitle");
        String tmdbId = textOf(item, TMDB_NS, "movieId");

        // Roughly half a Letterboxd feed's items are lists and other
        // activity rather than watched films; those carry no filmTitle.
        // Without a TMDb id there's also no stable identity to dedup on,
        // so such an entry can't be pushed to the ranking catalog.
        if (title == null || tmdbId == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new LetterboxdFilm(
                Long.valueOf(tmdbId),
                title,
                parseInt(textOf(item, LETTERBOXD_NS, "filmYear")),
                parsePoster(textOf(item, null, "description")),
                parseDate(textOf(item, LETTERBOXD_NS, "watchedDate")),
                parseRating(textOf(item, LETTERBOXD_NS, "memberRating"))));
    }

    private String textOf(Element item, String namespace, String localName) {
        NodeList nodes = namespace == null
                ? item.getElementsByTagName(localName)
                : item.getElementsByTagNameNS(namespace, localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        String text = node.getTextContent();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String parsePoster(String description) {
        if (description == null) {
            return null;
        }
        Matcher matcher = POSTER_PATTERN.matcher(description);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseRating(String value) {
        try {
            return value == null ? null : new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
