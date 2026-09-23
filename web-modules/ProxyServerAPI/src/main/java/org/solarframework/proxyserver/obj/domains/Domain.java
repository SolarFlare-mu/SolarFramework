package org.solarframework.proxyserver.obj.domains;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Domain extends BaseDomain<Domain> {
    private final List<Subdomain> subdomains = new ArrayList<>();

    public Domain(String ip, String name) {
        super(ip, name, "./config/domain/" + name + "/_");
    }
    public Domain(String ip, String name, String path) {
        super(ip, name, path);
    }


    /** Re-links every subdomain to this instance, which is what a subdomain's parent-name lookup needs after a fresh JSON load - Gson never runs {@link Subdomain}'s constructor. */
    public List<Subdomain> getSubdomains() {
        subdomains.forEach(s -> s.bindTo(this));
        return subdomains;
    }

    @Override
    public String getHost() {
        return getName();
    }

    public String getPath(String subdomain) {
        if (Objects.equals(subdomain, getName())) return getPath();
        Subdomain sub = getSubdomains().stream().filter(s -> s.getHost().equals(subdomain)).findFirst().orElse(null);
        return sub == null ? null : sub.getPath();
    }
    public List<String> getHosts() {
        List<String> hosts = new ArrayList<>();
        hosts.add(getHost());
        if (!subdomains.isEmpty()) hosts.addAll(subdomains.stream().map(Subdomain::getHost).toList());
        return hosts;
    }

    public Subdomain addSubdomain(String subdomain) {
        subdomains.add(new Subdomain(this, subdomain));
        return subdomains.getLast();
    }
    public Subdomain addSubdomain(String subdomain, String path) {
        subdomains.add(new Subdomain(this, subdomain, path));
        return subdomains.getLast();
    }

}
