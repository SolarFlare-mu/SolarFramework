package org.solarframework.proxyserver.obj;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** What a caller sent about itself never reaches the backend: every header is the proxy's own or gone. */
class CallerHeadersTest {

    private static Map<String, String> spoofed() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Forwarded-For", "6.6.6.6");
        headers.put("X-Real-IP", "6.6.6.6");
        headers.put("X-ProxyGuard-Checked", "1");
        headers.put("X-Forwarded-Proto", "http");
        headers.put("X-Forwarded-Host", "evil.com");
        headers.put("X-Forwarded-Port", "80");
        headers.put("X-Forwarded-Prefix", "/evil");
        headers.put("Forwarded", "for=6.6.6.6;host=evil.com");
        headers.put("Accept", "text/html");
        return headers;
    }

    @Test void everyClaimOfTheCallerIsReplacedOrDropped() {
        Map<String, String> headers = spoofed();

        new CallerHeaders("10.0.0.4", "mysite.com", "https", false).writeInto(headers::put, headers::remove);

        assertEquals(Map.of("X-Forwarded-For", "10.0.0.4", "X-Real-IP", "10.0.0.4", "X-ProxyGuard-Checked", "0", "X-Forwarded-Proto", "https",
                "X-Forwarded-Host", "mysite.com", "Accept", "text/html"), headers);
    }

    @Test void aRequestThatPassedAContributedRuleIsMarkedChecked() {
        Map<String, String> headers = spoofed();

        new CallerHeaders("10.0.0.4", "mysite.com", "https", true).writeInto(headers::put, headers::remove);

        assertEquals("1", headers.get("X-ProxyGuard-Checked"));
    }

    @Test void noHostMeansNoForwardedHost() {
        Map<String, String> headers = spoofed();

        new CallerHeaders("10.0.0.4", null, "https", false).writeInto(headers::put, headers::remove);

        assertFalse(headers.containsKey("X-Forwarded-Host"));
    }
}
