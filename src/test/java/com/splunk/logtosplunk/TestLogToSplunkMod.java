package com.splunk.logtosplunk;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import net.minecraftforge.fml.common.eventhandler.EventBus;

public class TestLogToSplunkMod {

    private LogToSplunkMod mod;
    private SplunkMessagePreparerSpy spy;

    @Before
    public void setUp(){
        spy = new SplunkMessagePreparerSpy();
        mod = new LogToSplunkMod(spy, mock(EventBus.class));
    }

    @Test
    public void testInit(){
        mod.init(null);//init event not used.
        assertEquals("Splunk for Minecraft initialized.", spy.getMessage());
    }
}
