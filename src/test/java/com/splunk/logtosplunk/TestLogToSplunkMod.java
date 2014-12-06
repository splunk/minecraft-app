package com.splunk.logtosplunk;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class TestLogToSplunkMod {

    private LogToSplunkMod mod;
    private SplunkMessagePreparerSpy spy;

    @Before
    public void setUp(){
        spy = new SplunkMessagePreparerSpy();
        mod = new LogToSplunkMod(spy);
    }

    @Test
    public void testInit(){
        mod.init(null);//init event not used.
        assertEquals("Splunk for Minecraft initialized.", spy.getMessage());
    }
}
