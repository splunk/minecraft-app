package com.splunk.logtosplunk;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.Logger;

/**
 * Knows a single Splunk instance by its host:port and forwards data to it.
 */
public class SingleSplunkConnection implements SplunkConnector, Runnable {
    private static final String LOGGER_PREFIX = "SplunkConnection - ";
    private static final String DEFAULT_RECONNECT_TIME = "10";

    /**
     * Interval in seconds between attempts to connect to splunk.
     */
    private static final int RECONNECT_TIME =
            Integer.valueOf(System.getProperty("splunk_mc.reconnect_time", DEFAULT_RECONNECT_TIME));

    private final Logger logger;
    private final String host;
    private final int port;
    private Socket socket;
    private boolean connected;

    /**
     * This classes data buffer.
     */
    private final Queue<String> data = new LinkedList<String>();

    /**
     * Constructor. Determines which Splunk instance this will connect to based on the host:port passed in. Set up a
     * shutdown hook to send any remaining messages to Splunk on close..
     *
     * @param host Host of Splunk to connect to.
     * @param port Port of Splunk to connect to.
     * @param startImmediately If true, creates a thread and starts this Splunk on construction.
     */
    public SingleSplunkConnection(String host, int port, boolean startImmediately) {
        this.host = host;
        this.port = port;
        logger = getLogger(LOGGER_PREFIX + host + ':' + port);
        connected = false;

        addFlushShutdownHook();

        if (startImmediately) {
            new Thread(this).start();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (connect()) {
                if (!sendData()) {
                    logger.error(String.format("Failed to send a message, will retry in %s seconds", RECONNECT_TIME));
                }
            }

            try {
                Thread.sleep(1000 * RECONNECT_TIME);
            } catch (InterruptedException e) {
                //eat exception.
            }
        }
    }

    /**
     * Ques up a message to send to this Spunk connections' Splunk instance.
     *
     * @param message The message to send.
     */
    @Override
    public void sendToSplunk(String message) {
        String stampedMsg = Calendar.getInstance().getTime().toString() + ' ' + message + "\r\n\r\n";

        boolean sent = false;
        while (!sent) {
            synchronized (data) {
                sent = data.offer(stampedMsg);
            }
        }
    }

    /**
     * Access if this SplunkConnection is connected to its Splunk instance.
     *
     * @return True if it is connected to its Splunk instance.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() ;
    }

    /**
     * Tries to connect to this Splunk Connections associated Splunk @ its {@code host:port}.
     *
     * @return True if the socket was successfully initialized.
     */
    private boolean connect() {
        boolean wasConnected = socket != null;
        socket = getSocket();
        connected = socket != null;

        if (connected && !wasConnected) {
            logger.info(
                    "LogToSplunk connected with Splunk instance " + host + ':' + port);
        } else if (!connected && wasConnected) {
            logger.error(String.format("Lost connection to splunk instance: %s:%s!", host, port));
        }

        return connected;
    }

    /**
     * Attempts to open a socket to the desired Splunk.
     *
     * @return A Socket if opened successfully, or null if not.
     */
    private Socket getSocket() {
        final String errMsg = "Problem connecting to splunk";
        try {
            return new Socket(host, port);
        } catch (Exception e) {
            logger.error(errMsg, e);
            return null;
        }
    }

    /**
     * Tries to send this Splunk connection's payload {@code data} to a Splunk via the {@link
     * SingleSplunkConnection#send(String)} method. Aborts if it fails to send a message.
     */
    private boolean sendData() {
        while (!data.isEmpty()) {
            String message = data.peek();
            if (send(message)) {
                synchronized (data) {
                    data.remove();
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Ships a message off to a Splunk.
     *
     * @param message The message to send to Splunk.
     * @return True if the operation succeeded.
     */
    private boolean send(String message) {
        try {
            socket.getOutputStream().write(message.getBytes("UTF-8"));
            return true;
        } catch (IOException e) {
            logger.debug("Unable to send message!");
            return false;
        }
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
}
