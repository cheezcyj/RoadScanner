package com.roadscanner.service.upload;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocalRestTemplateServiceTest {

    private final LocalRestTemplateService service = new LocalRestTemplateService();

    @Test
    public void returnsUnknownWhenLocalMlIsDisabled() {
        assertEquals("44", service.callFlaskApi("/local-files/example.png"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankImageUrl() {
        service.callFlaskApi(" ");
    }
}
