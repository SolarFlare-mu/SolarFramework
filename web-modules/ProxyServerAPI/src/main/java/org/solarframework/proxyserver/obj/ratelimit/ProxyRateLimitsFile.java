package org.solarframework.proxyserver.obj.ratelimit;

import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

/** Manual proxy request limits, stored separately from domains because they control traffic rather than routing. */
public class ProxyRateLimitsFile extends JSONItem<ProxyRateLimitsFile> {
    private final List<RateLimitRule> rules;

    @Override
    public String file() { return "config/proxy/rate-limits.json"; }

    public ProxyRateLimitsFile(List<RateLimitRule> rules) {
        this.rules = rules;
    }

    public List<RateLimitRule> rules() { return rules; }

    public static List<RateLimitRule> read() { return ReadOrCreate(ProxyRateLimitsFile.class, new ProxyRateLimitsFile(new ArrayList<>())).rules(); }
}
