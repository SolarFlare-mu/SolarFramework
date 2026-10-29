package org.solarframework.proxyserver.spring;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.headers.ForwardedHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.RemoveHopByHopHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Through the gateway's own header filters, in the order its routing applies them, a caller's claims about itself never reach the backend. */
class CallerHeadersFilterTest {

    private static MockServerWebExchange spoofingCall() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("https://mysite.com/api/x")
                .remoteAddress(new InetSocketAddress("10.0.0.4", 50000))
                .header("Host", "mysite.com")
                .header("X-Forwarded-For", "6.6.6.6")
                .header("X-Real-IP", "6.6.6.6")
                .header("X-ProxyGuard-Checked", "1")
                .header("X-Forwarded-Host", "evil.com")
                .header("X-Forwarded-Proto", "http")
                .header("Forwarded", "for=6.6.6.6")
                .header("Accept", "text/html"));
    }

    /** The routing filter's list, sorted as Spring sorts an injected list: the gateway's defaults, then this one. */
    private static HttpHeaders forwarded(MockServerWebExchange exchange) {
        List<HttpHeadersFilter> filters = new ArrayList<>(List.of(new CallerHeadersFilter(), new XForwardedHeadersFilter(), new ForwardedHeadersFilter(), new RemoveHopByHopHeadersFilter()));
        filters.sort(AnnotationAwareOrderComparator.INSTANCE);
        return HttpHeadersFilter.filterRequest(filters, exchange);
    }

    @Test void theBackendIsToldTheSocketsAddressAndNothingTheCallerClaimed() {
        HttpHeaders headers = forwarded(spoofingCall());

        assertEquals(List.of("10.0.0.4"), headers.get("X-Forwarded-For"));
        assertEquals(List.of("10.0.0.4"), headers.get("X-Real-IP"));
        assertEquals(List.of("0"), headers.get("X-ProxyGuard-Checked"));
        assertEquals(List.of("mysite.com"), headers.get("X-Forwarded-Host"));
        assertEquals(List.of("https"), headers.get("X-Forwarded-Proto"));
        assertNull(headers.get("Forwarded"));
        assertNull(headers.get("X-Forwarded-Port"));
        assertEquals(List.of("text/html"), headers.get("Accept"));
    }

    /** Without it, the gateway's own filter appends: the caller's address comes first, which is the entry a backend believes. */
    @Test void withoutItTheGatewayLetsTheCallerChooseItsAddress() {
        List<HttpHeadersFilter> defaults = List.of(new XForwardedHeadersFilter(), new ForwardedHeadersFilter());

        HttpHeaders headers = HttpHeadersFilter.filterRequest(defaults, spoofingCall());

        assertTrue(headers.getFirst("X-Forwarded-For").startsWith("6.6.6.6"));
        assertEquals("1", headers.getFirst("X-ProxyGuard-Checked"));
    }
}
