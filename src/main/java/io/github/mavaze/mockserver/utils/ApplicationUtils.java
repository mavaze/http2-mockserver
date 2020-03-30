package io.github.mavaze.mockserver.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.UndertowMessages;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;

@Slf4j
public class ApplicationUtils {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static String toJson(final Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} to json string.", obj);
            return obj.toString();
        }
    }

    public static String read(final InputStream openStream) throws IOException {
        try (final InputStream in = openStream;
             BufferedReader buffRead = new BufferedReader(new InputStreamReader(in))) {
            return buffRead.lines().collect(Collectors.joining(LINE_SEPARATOR));
        }
    }

    public static InputStream getResourceAsStream(final String resourceName) throws IOException {
        return getResource(resourceName).openStream();
    }

    public static URL getResource(final String resourceName) {
        ClassLoader loader = firstNonNull(
                        Thread.currentThread().getContextClassLoader(), ApplicationUtils.class.getClassLoader());
        return Optional.ofNullable(loader.getResource(resourceName))
                .orElseThrow(() -> UndertowMessages.MESSAGES.datasourceNotFound("Resource not found: " + resourceName));
    }

    public static <T> T firstNonNull(final T... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new NullPointerException("All parameters are null"));
    }
}
