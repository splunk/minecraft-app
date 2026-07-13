package com.splunk.sharedmc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;

/**
 * Writes documents straight into a Splunk KV-store collection via the splunkd REST API
 * ({@code .../storage/collections/data/<collection>/batch_save}).
 *
 * <p>This is deliberately NOT a {@link SplunkConnection}/HEC path: the HTTP Event Collector
 * can only write events into an <em>index</em>, never into a KV store. Populating a KV store
 * requires the management REST endpoint, which uses a different host/port (splunkd mgmt,
 * default 8089, HTTPS) and a different credential ({@code Authorization: Bearer <token>}),
 * not the HEC token.
 *
 * <p>batch_save upserts by each document's {@code _key}, so re-sending the same player UUID
 * overwrites the previous snapshot rather than appending — which is exactly what we want for
 * non-time-series snapshot data.
 *
 * <p>SSL note: splunkd's management port presents a self-signed certificate by default. To
 * keep this usable in a lab/self-hosted Minecraft deployment, the client trusts all certs and
 * skips hostname verification. For a hardened deployment, replace the trust strategy with a
 * truststore containing the splunkd cert.
 */
public class KvStoreConnection {

    private static final String LOGGER_PREFIX = "KvStoreConnection - ";
    private static final String BATCH_SAVE_URL =
            "https://%s:%s/servicesNS/nobody/%s/storage/collections/data/%s/batch_save";

    private final Logger logger;
    private final String url;
    private final String token;

    public KvStoreConnection(String host, int port, String app, String collection, String token) {
        this.logger = LogManager.getLogger(LOGGER_PREFIX + host + ':' + port + '/' + collection);
        this.token = token;
        this.url = String.format(BATCH_SAVE_URL, host, port, app, collection);
    }

    /**
     * Upserts a batch of documents. The body must be a JSON array of objects, each of which
     * should carry a {@code _key} to make the write idempotent.
     *
     * @param jsonArrayBody JSON array string, e.g. {@code [{"_key":"...","name":"..."}, ...]}.
     * @return true if splunkd accepted the batch (2xx).
     */
    public boolean batchSave(String jsonArrayBody) {
        final CloseableHttpClient client = buildClient();
        if (client == null) {
            return false;
        }
        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + token);
            post.setEntity(new StringEntity(jsonArrayBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                int code = response.getCode();
                if (code > 199 && code < 300) {
                    return true;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                logger.error("KV-store batch_save failed (HTTP {}): {}", code, new String(out.toByteArray()));
                return false;
            }
        } catch (final IOException e) {
            logger.error("Unable to write to KV store!", e);
            return false;
        } finally {
            try {
                client.close();
            } catch (final IOException ignored) {
                // closing best-effort
            }
        }
    }

    private CloseableHttpClient buildClient() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();
            SSLConnectionSocketFactory sslFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslFactory)
                    .build();
            return HttpClients.custom().setConnectionManager(cm).build();
        } catch (final Exception e) {
            logger.error("Unable to build TLS client for KV store connection", e);
            return null;
        }
    }
}
