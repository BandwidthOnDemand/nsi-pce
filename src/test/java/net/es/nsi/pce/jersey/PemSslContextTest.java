package net.es.nsi.pce.jersey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import jakarta.ws.rs.ProcessingException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.net.ssl.SSLParameters;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Outbound mutual TLS from the nsi.tls.* PEM files, against a server that requires a client certificate. */
public class PemSslContextTest {
    private static final String[] PROPERTIES = {PemSslContext.CERTIFICATE, PemSslContext.PRIVATE_KEY,
        PemSslContext.TRUSTED_CERTIFICATES, "javax.net.ssl.keyStore", "javax.net.ssl.keyStorePassword"};

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Issued ca;
    private Issued client;
    private Path caPem;
    private HttpsServer https;

    @Before
    public void setUp() throws Exception {
        ca = issue("CN=Test CA", null);
        client = issue("CN=Test Client", ca);
        caPem = writePem("ca.crt", ca.certificate);
        https = startServer();
    }

    @After
    public void tearDown() {
        https.stop(0);
        for (String property : PROPERTIES) {
            System.clearProperty(property);
        }
    }

    @Test
    public void noPropertiesLeavesTheJvmDefault() {
        assertFalse(PemSslContext.fromSystemProperties().isPresent());
    }

    @Test
    public void presentsTheClientCertificateAndTrustsTheCaBundle() throws Exception {
        configure(writePem("client.crt", client.certificate, ca.certificate), writePkcs8("client.key", client), caPem);

        assertEquals(200, get());
    }

    @Test
    public void takesTheClientCertificateFromTheKeystoreWhenOnlyTrustIsPem() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry("client", client.keys.getPrivate(), "test".toCharArray(),
                new X509Certificate[] {client.certificate});
        Path keystore = folder.getRoot().toPath().resolve("client.p12");
        try (OutputStream out = Files.newOutputStream(keystore)) {
            store.store(out, "test".toCharArray());
        }
        System.setProperty("javax.net.ssl.keyStore", keystore.toString());
        System.setProperty("javax.net.ssl.keyStorePassword", "test");
        System.setProperty(PemSslContext.TRUSTED_CERTIFICATES, caPem.toString());

        assertEquals(200, get());
    }

    @Test
    public void serverRejectsACallWithoutClientCertificate() {
        System.setProperty(PemSslContext.TRUSTED_CERTIFICATES, caPem.toString());

        assertThrows(ProcessingException.class, this::get);
    }

    @Test
    public void untrustedServerIsRejected() throws Exception {
        configure(writePem("client.crt", client.certificate), writePkcs8("client.key", client),
                writePem("other.crt", issue("CN=Other CA", null).certificate));

        assertThrows(ProcessingException.class, this::get);
    }

    @Test
    public void certificateWithoutPrivateKeyIsAConfigurationError() throws Exception {
        System.setProperty(PemSslContext.CERTIFICATE, writePem("client.crt", client.certificate).toString());

        IllegalStateException ex = assertThrows(IllegalStateException.class, PemSslContext::fromSystemProperties);
        assertThat(ex.getMessage(), containsString(PemSslContext.PRIVATE_KEY));
    }

    @Test
    public void nonPkcs8KeyNamesTheConversion() throws Exception {
        // A KeyPair is written as "BEGIN EC PRIVATE KEY", as openssl does by default.
        configure(writePem("client.crt", client.certificate), writePem("client-sec1.key", client.keys), caPem);

        IllegalStateException ex = assertThrows(IllegalStateException.class, PemSslContext::fromSystemProperties);
        assertThat(ex.getMessage(), containsString("openssl pkcs8 -topk8"));
    }

    private int get() {
        RestClient restClient = new RestClient();
        try {
            return restClient.get()
                    .target("https://localhost:" + https.getAddress().getPort() + "/")
                    .request().get().getStatus();
        } finally {
            restClient.close();
        }
    }

    private static void configure(Path certificate, Path privateKey, Path trusted) {
        System.setProperty(PemSslContext.CERTIFICATE, certificate.toString());
        System.setProperty(PemSslContext.PRIVATE_KEY, privateKey.toString());
        System.setProperty(PemSslContext.TRUSTED_CERTIFICATES, trusted.toString());
    }

    private HttpsServer startServer() throws Exception {
        Issued server = issue("CN=localhost", ca);
        HttpsServer https = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(PemSslContext.create(
                writePem("server.crt", server.certificate, ca.certificate), writePkcs8("server.key", server), caPem)) {
            @Override
            public void configure(HttpsParameters params) {
                // Via SSLParameters: HttpsParameters.setNeedClientAuth is undone by its wantClientAuth.
                SSLParameters ssl = getSSLContext().getDefaultSSLParameters();
                ssl.setNeedClientAuth(true);
                params.setSSLParameters(ssl);
            }
        });
        https.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        https.start();
        return https;
    }

    private Path writePkcs8(String name, Issued issued) throws Exception {
        return writePem(name, new JcaPKCS8Generator(issued.keys.getPrivate(), null));
    }

    private Path writePem(String name, Object... objects) throws Exception {
        Path file = folder.getRoot().toPath().resolve(name);
        try (JcaPEMWriter pem = new JcaPEMWriter(Files.newBufferedWriter(file))) {
            for (Object object : objects) {
                pem.writeObject(object);
            }
        }
        return file;
    }

    private record Issued(X509Certificate certificate, KeyPair keys) {
    }

    /** A throw-away EC certificate: a self-signed CA when issuer is null, else a leaf valid for localhost. */
    private static Issued issue(String subject, Issued issuer) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keys = generator.generateKeyPair();
        X500Principal name = new X500Principal(subject);
        Instant now = Instant.now();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer == null ? name : issuer.certificate.getSubjectX500Principal(),
                BigInteger.valueOf(System.nanoTime()),
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(1, ChronoUnit.DAYS)),
                name,
                keys.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(issuer == null));
        if (issuer != null) {
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[] {
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1")}));
        }
        KeyPair signer = issuer == null ? keys : issuer.keys;
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(signer.getPrivate())));
        return new Issued(certificate, keys);
    }
}
