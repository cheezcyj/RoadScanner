package com.roadscanner.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the client address without trusting forwarding headers from an
 * arbitrary internet peer. Only exact proxy addresses configured by the
 * operator may contribute an X-Forwarded-For chain.
 */
@Component
public class ClientAddressResolver {
    private static final int MAX_FORWARDED_HEADER_LENGTH = 1024;
    private static final int MAX_FORWARDED_HOPS = 32;

    private final Set<String> trustedProxyAddresses;

    @Autowired
    public ClientAddressResolver(
            @Value("${roadscanner.security.trusted-proxies:}") String configuredAddresses) {
        this.trustedProxyAddresses = parseTrustedAddresses(configuredAddresses);
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String directPeer = normalizeToken(request.getRemoteAddr());
        if (directPeer == null || !trustedProxyAddresses.contains(directPeer)) {
            return directPeer;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.length() > MAX_FORWARDED_HEADER_LENGTH) {
            return directPeer;
        }

        String[] hops = forwarded.split(",", -1);
        if (hops.length == 0 || hops.length > MAX_FORWARDED_HOPS) {
            return directPeer;
        }
        for (int index = hops.length - 1; index >= 0; index--) {
            String candidate = normalizeToken(hops[index]);
            if (!isIpLiteral(candidate)) {
                return directPeer;
            }
            if (!trustedProxyAddresses.contains(candidate)) {
                return candidate;
            }
        }
        return directPeer;
    }

    private static Set<String> parseTrustedAddresses(String configuredAddresses) {
        if (configuredAddresses == null || configuredAddresses.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> addresses = new HashSet<>();
        for (String rawAddress : configuredAddresses.split(",")) {
            String address = normalizeToken(rawAddress);
            if (isIpLiteral(address)) {
                addresses.add(address);
            }
        }
        return Collections.unmodifiableSet(addresses);
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() >= 2
                && normalized.charAt(0) == '['
                && normalized.charAt(normalized.length() - 1) == ']') {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean isIpLiteral(String value) {
        if (value == null) {
            return false;
        }
        if (value.indexOf(':') >= 0) {
            if (!value.matches("[0-9A-Fa-f:.%]+")) {
                return false;
            }
            try {
                return InetAddress.getByName(value) instanceof Inet6Address;
            } catch (Exception ignored) {
                return false;
            }
        }

        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || !octet.matches("\\d{1,3}")) {
                return false;
            }
            if (Integer.parseInt(octet) > 255) {
                return false;
            }
        }
        return true;
    }
}
