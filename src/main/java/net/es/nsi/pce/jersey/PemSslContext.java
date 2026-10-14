package net.es.nsi.pce.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the SSLContext for outbound calls from PEM files named by the nsi.tls.* system properties.
 * What these leave unset comes from the javax.net.ssl.* properties, as in Jersey's default context.
 */
public final class PemSslContext {
    public static final String CERTIFICATE = "nsi.tls.certificate";
    public static final String PRIVATE_KEY = "nsi.tls.privateKey";
    public static final String TRUSTED_CERTIFICATES = "nsi.tls.trustedCertificates";

    private static final Logger log = LoggerFactory.getLogger(PemSslContext.class);
    private static final Pattern PKCS8_PEM =
            Pattern.compile("-----BEGIN PRIVATE KEY-----(.*?)-----END PRIVATE KEY-----", Pattern.DOTALL);
    private static final List<String> KEY_ALGORITHMS = List.of("RSA", "EC", "Ed25519", "Ed448");
    // Protects the key inside the in-memory KeyStore only; it is never written anywhere.
    private static final char[] IN_MEMORY_PASSWORD = "nsi-pce".toCharArray();

    private PemSslContext() {
    }

    /**
     * @return the SSLContext, or empty when no nsi.tls.* property is set.
     * @throws IllegalStateException if only one of the certificate and private key is set, or a file
     *         cannot be read.
     */
    public static Optional<SSLContext> fromSystemProperties() {
        Path certificate = property(CERTIFICATE);
        Path privateKey = property(PRIVATE_KEY);
        Path trusted = property(TRUSTED_CERTIFICATES);
        if ((certificate == null) != (privateKey == null)) {
            throw new IllegalStateException("Set both " + CERTIFICATE + " and " + PRIVATE_KEY + ", or neither");
        }
        if (certificate == null && trusted == null) {
            return Optional.empty();
        }
        log.info("Outbound TLS: client certificate {}, trusted certificates {}",
                Objects.toString(certificate, "from javax.net.ssl"), Objects.toString(trusted, "from javax.net.ssl"));
        return Optional.of(create(certificate, privateKey, trusted));
    }

    /**
     * @param certificate client certificate and chain, or null to leave the identity to javax.net.ssl.
     * @param privateKey its PKCS#8 private key; null exactly when certificate is.
     * @param trusted CA bundle, or null to leave trust to javax.net.ssl and the JVM's cacerts.
     */
    static SSLContext create(Path certificate, Path privateKey, Path trusted) {
        try {
            SslConfigurator ssl = SslConfigurator.newInstance(true);
            if (certificate != null) {
                ssl.keyStore(identityStore(certificate, privateKey)).keyPassword(IN_MEMORY_PASSWORD);
            }
            if (trusted != null) {
                ssl.trustStore(trustStore(trusted));
            }
            return ssl.createSSLContext();
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("Cannot load the nsi.tls.* PEM files: " + ex.getMessage(), ex);
        }
    }

    private static Path property(String name) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    private static KeyStore identityStore(Path certificate, Path privateKey)
            throws GeneralSecurityException, IOException {
        KeyStore store = emptyKeyStore();
        store.setKeyEntry("client", readPrivateKey(privateKey), IN_MEMORY_PASSWORD,
                readCertificates(certificate).toArray(new Certificate[0]));
        return store;
    }

    private static KeyStore trustStore(Path trusted) throws GeneralSecurityException, IOException {
        KeyStore store = emptyKeyStore();
        List<Certificate> certificates = readCertificates(trusted);
        for (int i = 0; i < certificates.size(); i++) {
            store.setCertificateEntry("ca-" + i, certificates.get(i));
        }
        return store;
    }

    private static KeyStore emptyKeyStore() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        return store;
    }

    /** Reads every certificate in a PEM file: a leaf followed by its chain, or a CA bundle. */
    private static List<Certificate> readCertificates(Path file) throws GeneralSecurityException, IOException {
        try (InputStream in = Files.newInputStream(file)) {
            List<Certificate> certificates = List.copyOf(CertificateFactory.getInstance("X.509").generateCertificates(in));
            if (certificates.isEmpty()) {
                throw new GeneralSecurityException("No certificate in " + file);
            }
            return certificates;
        }
    }

    /** Reads a PKCS#8 private key; cert-manager writes one with privateKey.encoding: PKCS8. */
    private static PrivateKey readPrivateKey(Path file) throws GeneralSecurityException, IOException {
        Matcher pem = PKCS8_PEM.matcher(Files.readString(file));
        if (!pem.find()) {
            throw new GeneralSecurityException("No PKCS#8 \"BEGIN PRIVATE KEY\" block in " + file
                    + "; convert it with: openssl pkcs8 -topk8 -nocrypt -in <key> -out <key.pkcs8>");
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(pem.group(1)));
        return KEY_ALGORITHMS.stream()
                .map(algorithm -> generatePrivate(algorithm, spec))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new GeneralSecurityException("Unsupported private key type in " + file));
    }

    private static PrivateKey generatePrivate(String algorithm, PKCS8EncodedKeySpec spec) {
        try {
            return KeyFactory.getInstance(algorithm).generatePrivate(spec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            return null;
        }
    }
}
