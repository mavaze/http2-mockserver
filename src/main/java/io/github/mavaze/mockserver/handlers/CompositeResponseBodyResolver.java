package io.github.mavaze.mockserver.handlers;

import io.github.mavaze.mockserver.config.StubConfig.Stubbing.ResponseSpecification.Format;
import io.github.mavaze.mockserver.config.StubConfig.Stubbing.ResponseSpecification.ResponseBody;
import io.github.mavaze.mockserver.utils.ApplicationUtils;
import io.undertow.UndertowMessages;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Optional;

import static io.github.mavaze.mockserver.config.Constants.RESPONSES_FOLDER;
import static java.io.File.separator;
import static java.lang.String.join;
import static java.util.Optional.ofNullable;

@Slf4j
public class CompositeResponseBodyResolver implements ResponseBodyResolver {

    private final ResponseBodyResolver emptyResponseResolver = new EmptyResponseResolver(null);
    private final ResponseBodyResolver requestResponseResolver = new RequestResponseResolver(emptyResponseResolver);
    private final ResponseBodyResolver fileResponseResolver = new FileResponseResolver(requestResponseResolver);
    private final ResponseBodyResolver inlineResponseResolver = new InlineResponseResolver(fileResponseResolver);

    public final boolean supports(Format format) {
        return format == null;
    }

    public final ResponseBodyResolver getNext() { return null; }

    public final Optional<String> resolve(ResponseBody body, ResponseContext ctx) {
        if (body.getFormat() != null) {
            return resolve(body.getFormat(), body, ctx);
        }
        return Optional.ofNullable(inlineResponseResolver.handle(body, ctx));
    }

    public final Optional<String> resolve(Format format, ResponseBody body, ResponseContext ctx) {
        ResponseBodyResolver resolver = inlineResponseResolver;
        while (resolver != null) {
            if(resolver.supports(format)) {
                return resolver.resolve(body, ctx);
            }
            resolver = resolver.getNext();
        }
        throw new IllegalStateException("No resolver present matching the format: " + format);
    }

    @RequiredArgsConstructor
    public static class FileResponseResolver implements ResponseBodyResolver {

        @Getter
        private final ResponseBodyResolver next;

        public final boolean supports(Format format) {
            return Format.FILE == format;
        }

        @Override
        public final Optional<String> resolve(ResponseBody body, ResponseContext ctx) {
            final String file = body.getFile();
            log.info("Resolving response from file: '{}'", file);

            try (InputStream in = ApplicationUtils.getResourceAsStream(join(separator, RESPONSES_FOLDER, file))) {
                 return Optional.ofNullable(ApplicationUtils.read(in));
            } catch (Exception e) {
                throw UndertowMessages.MESSAGES.datasourceNotFound("Response body file: " + file);
            }
        }
    }

    @RequiredArgsConstructor
    public static class InlineResponseResolver implements ResponseBodyResolver {

        @Getter
        private final ResponseBodyResolver next;

        public final boolean supports(Format format) {
            return Format.INLINE == format;
        }

        @Override
        public final Optional<String> resolve(ResponseBody body, ResponseContext ctx) {
            log.info("Resolving response from inline content: '{}'", body.getInline());
            return Optional.of(body.getInline());
        }
    }

    @RequiredArgsConstructor
    public static class RequestResponseResolver implements ResponseBodyResolver {

        @Getter
        private final ResponseBodyResolver next;

        public final boolean supports(Format format) {
            return Format.REQUEST == format;
        }

        @Override
        public final Optional<String> resolve(ResponseBody body, ResponseContext ctx) {
            final String requestBody = ctx.getRequest().getBody().toString();
            log.info("Resolving response from original request: '{}'", requestBody);
            return ofNullable(requestBody);
        }
    }

    @RequiredArgsConstructor
    public static class EmptyResponseResolver implements ResponseBodyResolver {

        @Getter
        private final ResponseBodyResolver next;

        public final boolean supports(Format format) {
            return Format.EMPTY == format;
        }

        @Override
        public final Optional<String> resolve(ResponseBody body, ResponseContext ctx) {
            return Optional.of("");
        }
    }
}
