package io.github.mavaze.mockserver.handlers;

import io.github.mavaze.mockserver.config.StubConfig.Stubbing.ResponseSpecification.Format;
import io.github.mavaze.mockserver.config.StubConfig.Stubbing.ResponseSpecification.ResponseBody;

import java.util.Optional;

public interface ResponseBodyResolver {

    boolean supports(Format format);

    ResponseBodyResolver getNext();

    Optional<String> resolve(ResponseBody body, ResponseContext ctx);

    default String handle(ResponseBody body, ResponseContext ctx) {
        return resolve(body, ctx)
                .orElseGet(() -> {
                    if (getNext() != null) {
                        try {
                            return getNext().handle(body, ctx);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                });
    }
}