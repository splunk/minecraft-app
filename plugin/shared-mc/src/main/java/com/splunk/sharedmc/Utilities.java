package com.splunk.sharedmc;


// A pseudo static class
public final class Utilities {


    private Utilities() {

    }

    /**
     * Sanitizes a string, removing any control codes that might exist in the string.
     *
     * @param message the message to be sanitized.
     * @return the sanitized message.
     */
    public static String sanitizeString(String message) {

        if ( message != null ) {
            return message.replaceAll("§\\S", ""); // Remove the formatting codes
        }
        else
        {
            return message;
        }
    }

}
