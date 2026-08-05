package com.roadscanner.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class ClientAddressResolverTest {

    @Test
    public void untrustedPeerCannotSpoofForwardedAddress() {
        ClientAddressResolver resolver = new ClientAddressResolver("10.0.0.10");
        MockHttpServletRequest request = request("198.51.100.7", "203.0.113.99");

        assertEquals("198.51.100.7", resolver.resolve(request));
    }

    @Test
    public void trustedProxyChainReturnsRightmostUntrustedClient() {
        ClientAddressResolver resolver = new ClientAddressResolver("10.0.0.10,10.0.0.11");
        MockHttpServletRequest request = request(
                "10.0.0.10",
                "192.0.2.20, 203.0.113.8, 10.0.0.11");

        assertEquals("203.0.113.8", resolver.resolve(request));
    }

    @Test
    public void malformedForwardedChainFallsBackToDirectProxy() {
        ClientAddressResolver resolver = new ClientAddressResolver("10.0.0.10");
        MockHttpServletRequest request = request("10.0.0.10", "attacker-value");

        assertEquals("10.0.0.10", resolver.resolve(request));
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
