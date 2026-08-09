package com.roadscanner.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppControllerTest {

    @Test
    public void rootRedirectsToPublicMainPage() {
        assertEquals("redirect:/main", new AppController().index());
    }
}
