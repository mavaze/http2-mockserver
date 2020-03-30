package io.github.mavaze.mockserver.handlers;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.mavaze.mockserver.config.Constants.HOSTNAME;
import static io.github.mavaze.mockserver.handlers.StubbedHttp2RequestHandler.REQUEST_BODY;
import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

@Data
@Builder
public class ResponseContext {

    private RequestContext request;
    private Map<String, String> global;

    @Data
    @Builder
    public static class RequestContext {
        private String path;
        private String body;
        private String scheme;
        private HttpString method;
        private Iterable<HeaderValues> header;

        private Map<String, ? extends Collection<String>> form;
        private Map<String, ? extends Collection<String>> query;
        private Map<String, ? extends Collection<String>> param;
    }

    public static ResponseContextBuilder buildWith(final HttpServerExchange exchange) {
        final RequestContext.RequestContextBuilder requestBuilder = RequestContext.builder()
                .body(exchange.getAttachment(REQUEST_BODY))
                .path(exchange.getRequestPath())
                .method(exchange.getRequestMethod())
                .scheme(exchange.getRequestScheme())
                .query(exchange.getQueryParameters())
                .param(exchange.getPathParameters())
                .form(extractFormData(exchange))
                .header(exchange.getRequestHeaders());

        return ResponseContext.builder()
            .global(singletonMap(HOSTNAME, exchange.getHostName()))
            .request(requestBuilder.build());
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
