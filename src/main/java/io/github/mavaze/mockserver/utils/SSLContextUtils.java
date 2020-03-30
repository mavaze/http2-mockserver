package io.github.mavaze.mockserver.utils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

public class SSLContextUtils {

    private static final char[] STORE_PASSWORD = "password".toCharArray();

    public static SSLContext createSSLContext(final String keyStore, final String trustStore) throws Exception {
        return createSSLContext(loadKeyStore(keyStore), loadKeyStore(trustStore));
    }

    public static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password("key"));
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    public static KeyStore loadKeyStore(String name) throws Exception {
        String storeLoc = System.getProperty(name);
        final InputStream stream;
        if(storeLoc == null) {
            stream = ApplicationUtils.getResource(name).openStream();
        } else {
            stream = Files.newInputStream(Paths.get(storeLoc));
        }

        if (stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try (InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, password(name));
            return loadedKeystore;
        }
    }

    static char[] password(String name) {
        String pw = System.getProperty(name + ".password");
        return pw != null ? pw.toCharArray() : STORE_PASSWORD;
    }
}
