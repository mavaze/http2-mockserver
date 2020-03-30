package io.github.mavaze.mockserver.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mavaze.mockserver.config.StubConfig.Stubbing;
import io.github.mavaze.mockserver.config.StubConfig.Stubbing.ResponseSpecification;
import io.github.mavaze.mockserver.utils.ApplicationUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatcher;
import io.undertow.util.PathTemplateMatcher.PathMatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.mvel2.templates.TemplateRuntime;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class StubbedHttp2RequestHandler implements HttpHandler {

    private final List<Stubbing> stubs;

    private final static ResponseBodyResolver responseBodyResolver = new CompositeResponseBodyResolver();

    public static final AttachmentKey<Object> REQUEST_BODY = AttachmentKey.create(Object.class);

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        final Object requestBody = parseBlocking(exchange);
        String queryString = exchange.getQueryString();
        queryString = ApplicationUtils.isEmpty(queryString) ? "" : "?" + queryString;
        log.trace("Request received: {} {}{}", exchange.getRequestMethod(), exchange.getRequestPath(), queryString);
        log.trace(" with Headers: {}", exchange.getRequestHeaders());
        log.trace(" and Body: {}", requestBody);

        final Stubbing stubbing = requestMatch(exchange)
                .orElseThrow(() -> new RuntimeException("Request did not match with any of stub mappings."));
        log.info("Matched stub for given request '{}'", stubbing.getName());
        exchange.putAttachment(REQUEST_BODY, requestBody);

        final ResponseContext context = ResponseContext.buildWith(exchange).build();
        final ResponseSpecification responseSpec = responseMatch(stubbing, context)
                .orElseThrow(() -> new RuntimeException("Request did not match with any of response selectors. You may supply a default response."));
        log.info("Matched response specification '{}'", responseSpec.getName());

        responseBodyResolver.resolve(responseSpec.getBody(), context).map(template -> {
            try {
                return (String) TemplateRuntime.eval(template, context);
            } catch (Exception e) {
                log.error("Failed to render template. Returning raw template. Reason: {}", e.getMessage());
                return template;
            }
        }).ifPresent(resp -> {
            exchange.setStatusCode(responseSpec.getStatus());
            final String contentType = responseSpec.getHeaders().get("Content-Type");
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
            if (!resp.trim().isEmpty()) {
                exchange.getResponseSender().send(resp);
            }
        });
    }

    private Object parseBlocking(final HttpServerExchange exchange) throws IOException {
        exchange.startBlocking();
        String requestBody = ApplicationUtils.read(exchange.getInputStream());
        HeaderValues contentTypeHeader = exchange.getRequestHeaders().get("Content-Type");
        if (!ApplicationUtils.isEmpty(contentTypeHeader) && contentTypeHeader.contains("application/json")) {
            return (! ApplicationUtils.isEmpty(requestBody))
                    ? new ObjectMapper().readValue(requestBody, Object.class)
                    : new Object();
        }
        return requestBody;
    }

    private Optional<Stubbing> requestMatch(final HttpServerExchange exchange) {
        return stubs.stream().filter(s -> {
            final List<String> methods = Arrays.asList(s.getRequest().getMethods().split(","));
            return methods.contains(exchange.getRequestMethod().toString());
        }).filter(s -> {
            final PathTemplateMatcher<Boolean> matcher = new PathTemplateMatcher<>();
            matcher.add(s.getRequest().getUri(), true);
            PathMatchResult<Boolean> match = matcher.match(exchange.getRequestPath());
            return Optional.ofNullable(match).map(m -> {
                for (Map.Entry<String, String> entry : m.getParameters().entrySet()) {
                    exchange.addPathParam(entry.getKey(), entry.getValue());
                }
                return true;
            }).orElse(false);
        }).findFirst();
    }

    private Optional<ResponseSpecification> responseMatch(final Stubbing stub, ResponseContext context) {
        return stub.getResponses().stream().filter(spec -> {
            if (spec.getBody() == null) {
                log.warn("Response specification doesn't have body specified. Skipping ...");
                return false;
            }
            String selector = spec.getSelector();
            if (selector == null || "default".equals(selector.toLowerCase())) {
                log.info("Response specification with no or default selector selected.");
                return true;
            }
            return MVEL.evalToBoolean(selector, context);
        }).findFirst();
    }
}
