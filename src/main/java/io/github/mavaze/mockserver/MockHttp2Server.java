package io.github.mavaze.mockserver;

import io.github.mavaze.mockserver.config.StubConfig;
import io.github.mavaze.mockserver.handlers.StubbedHttp2RequestHandler;
import io.github.mavaze.mockserver.utils.SSLContextUtils;
import io.undertow.Undertow;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;

@Slf4j
public class MockHttp2Server {

    private final Undertow server;

    private static SSLContext sslContext;
    private final static String bindAddress = System.getProperty("bind.address", "localhost");

    static {
        try {
            sslContext = SSLContextUtils.createSSLContext("server.keystore", "server.truststore");
        } catch (Exception e) {
            log.error("Failed to initialize SSL Context. Disabling configured SSL ports.", e);
        }
    }

    public MockHttp2Server(@NonNull final StubConfig stubConfig) {
        final Undertow.Builder builder = Undertow.builder().setServerOption(ENABLE_HTTP2, true);
        if (stubConfig.getPorts().getHttp() > 0) {
            builder.addHttpListener(stubConfig.getPorts().getHttp(), bindAddress);
        }
        if (stubConfig.getPorts().getHttps() > 0 && sslContext != null) {
            builder.addHttpsListener(stubConfig.getPorts().getHttps(), bindAddress, sslContext);
        }

        StubbedHttp2RequestHandler handler = new StubbedHttp2RequestHandler(stubConfig.getPaths());
        server = builder.setHandler(new EagerFormParsingHandler(handler)).build();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

}
