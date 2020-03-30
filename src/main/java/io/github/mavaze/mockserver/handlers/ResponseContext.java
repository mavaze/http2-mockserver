package io.github.mavaze.mockserver.handlers;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.mavaze.mockserver.config.Constants.HOSTNAME;
import static io.github.mavaze.mockserver.handlers.StubbedHttp2RequestHandler.REQUEST_BODY;
import static io.github.mavaze.mockserver.utils.ApplicationUtils.toListMap;
import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Data
@Builder
public class ResponseContext {

    private Map<String, ?> global;
    private RequestContext request;

    private LocalDateTime now;

    @Data
    @Builder
    public static class RequestContext {
        private String path;
        private Object body;
        private String scheme;
        private HttpString method;

        private Map<String, List<String>> header;
        private Map<String, List<String>> form;
        private Map<String, List<String>> query;
        private Map<String, List<String>> param;
    }

    @SuppressWarnings("serial")
    public static ResponseContextBuilder buildWith(final HttpServerExchange exchange) {
        final RequestContext.RequestContextBuilder requestBuilder = RequestContext.builder()
                .body(exchange.getAttachment(REQUEST_BODY))
                .path(exchange.getRequestPath())
                .method(exchange.getRequestMethod())
                .scheme(exchange.getRequestScheme())
                .query(toListMap(exchange.getQueryParameters()))
                .param(toListMap(exchange.getPathParameters()))
                .form(extractFormData(exchange))
                .header(extractHeaders(exchange));

        return ResponseContext.builder()
                .now(LocalDateTime.now())
                .global(new HashMap<String, Object>() {{
                    put(HOSTNAME, exchange.getHostName());
                }})
                .request(requestBuilder.build());
    }

    private static Map<String, List<String>> extractHeaders(HttpServerExchange exchange) {
        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        return requestHeaders.getHeaderNames().stream().collect(toMap(HttpString::toString, requestHeaders::get));
    }

    private static Map<String, List<String>> extractFormData(final HttpServerExchange exchange) {
        final Map<String, List<String>> formMap = new LinkedHashMap<>();
        final FormData attachment = exchange.getAttachment(FORM_DATA);
        if (attachment != null) {
            attachment.forEach(s -> {
                List<String> collect = attachment.get(s).stream()
                        .map(FormValue::getValue)
                        .collect(toList());
                formMap.put(s, collect);
            });
        }
        return formMap;
    }
}
