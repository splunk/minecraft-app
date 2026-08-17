package com.splunk.sharedmc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.io.CloseMode;

/**
 * Knows a single Splunk instance by its host:port and forwards data to it.
 */
public class SingleSplunkConnection implements SplunkConnection, Runnable {
    private static final String LOGGER_PREFIX = "SplunkConnection - ";
    private static final String DEFAULT_RECONNECT_TIME = "10";
    private String BASE_URL = "http://%s:%s/services/collector/event/1.0";

    /**
     * Interval in seconds between attempts to connect to Splunk.
     */
    private static final int RECONNECT_TIME =
            Integer.valueOf(System.getProperty("splunk_mc.reconnect_time", DEFAULT_RECONNECT_TIME));

    private final Logger logger;
    private final String url;

    private CloseableHttpClient httpClient;
    private CloseableHttpResponse response;
    
    private String token;

    // lazy
    private StringBuilder messagesToSend = new StringBuilder();
    private StringBuilder messagesOnRunway;

    /**
     * Constructor. Determines which Splunk instance this will connect to based on the host:port passed in. Set up a
     * shutdown hook to send any remaining messages to Splunk on close.
     *
     * @param host Host of Splunk to connect to.
     * @param port Port of Splunk to connect to.
     * @param startImmediately If true, creates a thread and starts this Splunk on construction.
     */
    public SingleSplunkConnection(String host, int port, String token, boolean startImmediately) {
        logger = LogManager.getLogger(LOGGER_PREFIX + host + ':' + port);
        this.token = token;
        url = String.format(BASE_URL, host, port);

        addFlushShutdownHook();

        if (startImmediately) {
            new Thread(this).start();
        }
    }

    @Override
    public void run() {
        while (true) {
            // Never let a single send failure kill the sender thread -- otherwise one
            // transient error (e.g. HEC briefly unreachable at startup) permanently stops
            // all logging until the server restarts.
            try {
                sendData();
            } catch (final Throwable t) {
                logger.error("Unexpected error while sending to Splunk; will retry.", t);
            }
            try {
                Thread.sleep(1000 * RECONNECT_TIME);
            } catch (final InterruptedException e) {
                //eat exception.
            }
        }
    }

    /**
     * Wraps a raw event message in the Splunk HEC envelope: {"event": <message>}.
     * Package-private for testing.
     */
    static String buildHecEnvelope(String message) {
        com.google.gson.JsonObject event = new com.google.gson.JsonObject();
        event.addProperty("event", message);
        return event.toString();
    }

    /**
     * Queues up a message to send to this Spunk connections' Splunk instance.
     *
     * @param message The message to send.
     */
    @Override
    public void sendToSplunk(String message) {
        messagesToSend.append(buildHecEnvelope(message));
    }

    private boolean sendData() {
        boolean success = false;
        if (messagesOnRunway == null) {
            // No batch in flight: promote queued messages to the runway, or bail if empty.
            if (messagesToSend.length() == 0) {
                return true; // nothing to send
            }
            messagesOnRunway = messagesToSend;
            messagesToSend = new StringBuilder();
        }
        // else: a previous batch failed to send and is still on the runway -- retry it
        // (the old code returned here without retrying, so any failed batch was stranded
        // forever and never reached Splunk).
        // Reset per-attempt so the finally block never closes a stale/previous response, and
        // so a null check correctly detects an execute() that threw before assigning.
        httpClient = null;
        response = null;
        try {
            logger.info("Sending data to splunk...");
            httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Splunk " + token);
            StringEntity entity = new StringEntity(messagesOnRunway.toString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            response = httpClient.execute(post);
            int responseCode = response.getCode();

            if (responseCode > 199 && responseCode < 300) {
                messagesOnRunway = null;
                success = true;
            } else {
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                response.getEntity().writeTo(outstream);
                byte[] responseBody = outstream.toByteArray();
                logger.error(new String(responseBody));
            }

            //post.completed();
        } catch (final IOException e) {
            logger.error("Unable to send message!", e);
            success = false;
        }finally{
            // Null-guard both closes: when execute() throws, response stays null. The old
            // code dereferenced it here, turning every send failure into an uncaught NPE
            // that killed the sender thread for good.
            if (response != null) {
                try {
                    response.close(CloseMode.GRACEFUL);
                } catch (final Exception e) {
                    logger.warn("Error closing Splunk response.", e);
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close(CloseMode.GRACEFUL);
                } catch (final Exception e) {
                    logger.warn("Error closing Splunk HTTP client.", e);
                }
            }
        }

        return success;
    }

    /**
     * Adds a shutdown hook that flushes this classes data buffer ({@code data}) by sending it to Splunk.
     */
    private void addFlushShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        logger.info("Shutting down: attempting to send remaining data.");
                        if (sendData()) {
                            logger.info("Remaining data sent!");
                        } else {
                            logger.error("Couldn't send all remaining data to Splunk!");
                            // TODO: Write data to a log file, 'unsent_data.splunk' or some such...
                        }
                    }
                });
    }

    private void initHttpClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

    }
}
