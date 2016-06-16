package com.splunk.sharedmc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

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

    private String server;
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
     * @param server Name of Minecraft server.
     * @param startImmediately If true, creates a thread and starts this Splunk on construction.
     */
    public SingleSplunkConnection(String host, int port, String server, String token, boolean startImmediately) {
        logger = LogManager.getLogger(LOGGER_PREFIX + host + ':' + port);
        this.token = token;
        url = String.format(BASE_URL, host, port);
        httpClient = HttpClients.createDefault();

        this.server = server;

        addFlushShutdownHook();

        if (startImmediately) {
            new Thread(this).start();
        }
    }

    @Override
    public void run() {
        while (true) {
            sendData();
            try {
                Thread.sleep(1000 * RECONNECT_TIME);
            } catch (final InterruptedException e) {
                //eat exception.
            }
        }
    }

    /**
     * Queues up a message to send to this Spunk connections' Splunk instance.
     *
     * @param message The message to send.
     */
    @Override
    public void sendToSplunk(String message) {
        JSONObject event = new JSONObject();
        message = Calendar.getInstance().getTime().toString() + ' ' + message + " server=" + (this.server).trim();
        event.put("event", message);

        messagesToSend.append(event.toString());
    }

    private boolean sendData() {
        boolean success = false;
        // probably a better way to do this.
        if (messagesOnRunway == null && messagesToSend.length() > 0) {
            messagesOnRunway = messagesToSend;
            messagesToSend = new StringBuilder();
        }else{
            // no messages to send, so safe to say messages have been sent.
            return messagesOnRunway == null;
        }
        try {
            logger.info("Sending data to splunk...");
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Splunk " + token);
            StringEntity entity = new StringEntity(messagesOnRunway.toString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(post);
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode > 199 && responseCode < 300) {
                messagesOnRunway = null;
                success = true;
            } else {
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                response.getEntity().writeTo(outstream);
                byte[] responseBody = outstream.toByteArray();
                logger.error(new String(responseBody));
            }

            response.close();
            post.completed();
        } catch (final IOException e) {
            logger.error("Unable to send message!", e);
            success = false;
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
