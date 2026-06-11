package com.cablepulse.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Computes weak-stable ETags for JSON payloads and supports conditional
 * {@code If-None-Match} requests (HTTP 304 Not Modified).
 */
public final class EtagSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EtagSupport() {}

    public static String computeEtag(Object payload) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(payload);
            String digest = DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8));
            return "\"" + digest + "\"";
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize payload for ETag", ex);
        }
    }

    public static boolean etagMatches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank() || etag == null || etag.isBlank()) {
            return false;
        }
        final String normalizedEtag = stripQuotes(etag);
        for (String candidate : ifNoneMatch.split(",")) {
            final String token = stripQuotes(candidate.trim());
            if (token.equals("*") || token.equalsIgnoreCase(normalizedEtag)) {
                return true;
            }
        }
        return false;
    }

    public static <T> ResponseEntity<T> respondWithEtag(
            String ifNoneMatch,
            Object etagSource,
            Supplier<ResponseEntity<T>> fullResponseSupplier) {

        final String etag = computeEtag(etagSource);
        if (etagMatches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header(HttpHeaders.ETAG, etag)
                    .build();
        }

        ResponseEntity<T> response = fullResponseSupplier.get();
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header(HttpHeaders.ETAG, etag)
                .body(response.getBody());
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
