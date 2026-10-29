package org.solarframework.proxyserver.spring;

import org.solarframework.proxyserver.obj.CallerHeaders;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

/**
 * Has the last word on what a forwarded request says about its caller. The gateway's own {@code XForwardedHeadersFilter} <i>appends</i> the socket address to
 * whatever {@code X-Forwarded-For} the caller sent, and a backend takes the first entry ({@code IpUtils.callerAddress}); nothing of it touched
 * {@code X-ProxyGuard-Checked}, which a backend trusts from loopback. Ordered last, after the gateway's header filters (order 0) and its hop-by-hop one
 * ({@code LOWEST_PRECEDENCE - 1}): the routing filter applies them as one ordered list. This gateway applies no rate limits, so no call is ever already checked.
 */
@Component
public class CallerHeadersFilter implements HttpHeadersFilter, Ordered {

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        HttpHeaders out = new HttpHeaders();
        out.putAll(input);
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        String clientIp = remote == null || remote.getAddress() == null ? "unknown" : remote.getAddress().getHostAddress();
        new CallerHeaders(clientIp, exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST), exchange.getRequest().getURI().getScheme(), false).writeInto(out::set, out::remove);
        return out;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
