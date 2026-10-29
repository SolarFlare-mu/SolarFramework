package org.solarframework.proxyserver.obj;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * What the proxy tells the application behind it when it forwards a request. The proxy is the edge, so whatever the caller sent for these is only its own
 * unverified claim: each is replaced by what the proxy itself saw, or dropped - never appended to, never passed on. {@code X-ProxyGuard-Checked: 1} is trusted
 * from a loopback address ({@code IpUtils.alreadyCheckedByProxy}), which behind a local proxy is every request, and {@code IpUtils.callerAddress} takes the first
 * {@code X-Forwarded-For} entry, so a caller's own would skip the backend's allow list and limit, or pass for another address.
 *
 * @param alreadyChecked whether the request passed a contributed rule's address check and spent its permit here, which the backend then does not do again
 */
public record CallerHeaders(String clientIp, String host, String scheme, boolean alreadyChecked) {
    /** Never passed on: the proxy has nothing true to put in them. */
    public static final List<String> DROPPED = List.of("Forwarded", "X-Forwarded-Prefix", "X-Forwarded-Port");

    /** Writes the headers through whatever header map the server has: {@code set} replaces a header's every value, {@code remove} takes it away. */
    public void writeInto(BiConsumer<String, String> set, Consumer<String> remove) {
        set.accept("X-Forwarded-For", clientIp);
        set.accept("X-Real-IP", clientIp);
        set.accept("X-ProxyGuard-Checked", alreadyChecked ? "1" : "0");
        set.accept("X-Forwarded-Proto", scheme);
        if (host == null) remove.accept("X-Forwarded-Host");
        else set.accept("X-Forwarded-Host", host);
        DROPPED.forEach(remove);
    }
}
