package io.github.mavaze.mockserver.config;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;

import java.util.LinkedHashMap;
import java.util.Map;

public class Constants {

    public static final String HOSTNAME = "hostname";
    public static final String REQUEST_PATH = "request.path";
    public static final String REQUEST_METHOD = "request.method";
    public static final String REQUEST_SCHEME = "request.scheme";
    public static final String REQUEST_QUERY_PREFIX = "request.query.";
    public static final String REQUEST_PARAMS_PREFIX = "request.param.";
    public static final String REQUEST_HEADERS_PREFIX = "request.header.";

    /**
     * This method is not expected here in Constants class.
     * This was an old approach. Backing it up.
     */
    @Deprecated
    @SuppressWarnings("serial")
    private Map<String, String> build(final HttpServerExchange exchange) {
        final Map<String, String> substitutes = new LinkedHashMap<String, String>() {{
            put(HOSTNAME, exchange.getHostName());
            put(REQUEST_PATH, exchange.getRequestPath());
            put(REQUEST_METHOD, exchange.getRequestMethod().toString());
            put(REQUEST_SCHEME, exchange.getRequestScheme());
        }};

        exchange.getQueryParameters().forEach((queryParam, value) -> {
            substitutes.put(REQUEST_QUERY_PREFIX + queryParam, value.element());
        });
        exchange.getPathParameters().forEach((pathParam, value) -> {
            substitutes.put(REQUEST_PARAMS_PREFIX + pathParam, value.element());
        });

        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        requestHeaders.getHeaderNames().forEach(headerName -> {
            final HeaderValues headerValues = requestHeaders.get(headerName);
            if (!headerValues.isEmpty()) {
                final String nameLowerCase = headerName.toString().toLowerCase();
                if (headerValues.size() == 1) {
                    substitutes.put(REQUEST_HEADERS_PREFIX + nameLowerCase, headerValues.get(0));
                    substitutes.put(REQUEST_HEADERS_PREFIX + nameLowerCase + "[0]", headerValues.get(0));
                } else {
                    substitutes.put(REQUEST_HEADERS_PREFIX + nameLowerCase, headerValues.toString());
                    for (int i = 0; i < headerValues.size(); i++) {
                        substitutes.put(REQUEST_HEADERS_PREFIX + nameLowerCase + "[" + i + "]", headerValues.get(i));
                    }
                }
            }
        });
        return substitutes;
    }
}
