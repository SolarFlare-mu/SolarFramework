package org.solarframework.proxyserver.obj.domains;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Subdomain extends BaseDomain<Subdomain> {
    /** Excluded from the JSON: it would just be the parent {@link Domain} pointing back at the array it owns, an infinite loop. {@link Domain#getSubdomains()} sets it again on every read instead. */
    @JsonIgnore
    private Domain rootDomain;

    protected Subdomain(Domain rootDomain, String name) {
        super(rootDomain.getIp(), name, "./config/domain/" + rootDomain.getName() + "/_" + name);
        this.rootDomain = rootDomain;
    }
    protected Subdomain(Domain rootDomain, String name, String path) {
        super(rootDomain.getIp(), name, path);
        this.rootDomain = rootDomain;
    }

    /** Package-private: only {@link Domain} re-links its own subdomains, and only after loading them from JSON. */
    void bindTo(Domain rootDomain) {
        this.rootDomain = rootDomain;
    }

    public Domain getRootDomain() {
        return rootDomain;
    }

    @Override
    public String getHost() {
        return getName() + "." + rootDomain.getName();
    }

}
