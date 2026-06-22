package com.splunk.sharedmc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class SingleSplunkConnectionTest {

    @Test
    public void buildHecEnvelope_wrapsMessageInEventField() {
        String out = SingleSplunkConnection.buildHecEnvelope("hello world");
        JsonObject parsed = JsonParser.parseString(out).getAsJsonObject();
        assertEquals("hello world", parsed.get("event").getAsString());
    }

    @Test
    public void buildHecEnvelope_escapesQuotes() {
        String out = SingleSplunkConnection.buildHecEnvelope("a \"quoted\" value");
        JsonObject parsed = JsonParser.parseString(out).getAsJsonObject();
        assertEquals("a \"quoted\" value", parsed.get("event").getAsString());
        assertTrue(out.contains("\\\""));
    }
}
