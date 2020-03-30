package io.github.mavaze.mockserver.handlers;

import io.github.mavaze.mockserver.config.StubConfig.Stubbing;
import io.github.mavaze.mockserver.config.StubConfig.Stubbing.ResponseSpecification;
import io.github.mavaze.mockserver.utils.ApplicationUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatcher;
import io.undertow.util.PathTemplateMatcher.PathMatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.mvel2.templates.TemplateRuntime;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.mavaze.mockserver.utils.ApplicationUtils.toJson;

@Slf4j
@RequiredArgsConstructor
public class StubbedHttp2RequestHandler implements HttpHandler {

    private final List<Stubbing> stubs;

    private final static ResponseBodyResolver responseBodyResolver = new CompositeResponseBodyResolver();

    public static final AttachmentKey<String> REQUEST_BODY = AttachmentKey.create(String.class);

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        final Optional<Stubbing> stubbing = requestMatch(exchange);
        exchange.putAttachment(REQUEST_BODY, parseBlocking(exchange));

        final ResponseContext context = ResponseContext.buildWith(exchange).build();
        final Optional<ResponseSpecification> responseSpec = stubbing.flatMap(stub -> responseMatch(stub, context));
        final Optional<String> responseBody = responseSpec.flatMap(spec -> {
            log.info("Matched response specification {}", spec);
            return responseBodyResolver.resolve(spec.getBody(), context);
        }).map(template -> {
            try {
                return (String) TemplateRuntime.eval(template, context);
            } catch (Exception e) {
                log.error("Failed to render template. Returning raw template.", e);
                return template;
            }
        });

        responseBody.ifPresent(resp -> {
            exchange.setStatusCode(responseSpec.get().getStatus());
            final String contentType = responseSpec.get().getHeaders().get("Content-Type");
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
            exchange.getResponseSender().send(
                    contentType.equals("application/json") ? (resp) : resp);
        });
    }

    private String parseBlocking(final HttpServerExchange exchange) throws IOException {
        exchange.startBlocking();
        return ApplicationUtils.read(exchange.getInputStream());
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
        log.info("Matched stub {}", stub);

        return stub.getResponses().stream().filter(a -> {
            String selector = a.getSelector();
            if (selector == null || "default".equals(selector.toLowerCase())) {
                return true;
            }

            Serializable compileExpression = MVEL.compileExpression(selector);
            //VariableResolverFactory functionFactory = new MapVariableResolverFactory(context);
            return (Boolean) MVEL.executeExpression(compileExpression, context);
        }).findFirst();
    }
}
