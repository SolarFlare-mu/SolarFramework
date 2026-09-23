package org.solarframework.proxyserver.obj.domains;

import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

/** Every domain the proxy serves, replacing the {@code web_domain}/{@code web_subdomain} tables - a full reload is
 *  already how the admin screen saved them, so a JSON file loses nothing a query would have given it. */
public class ProxyDomainsFile extends JSONItem<ProxyDomainsFile> {
    private final List<Domain> domains;

    @Override
    public String file() { return "config/proxy/domains.json"; }

    public ProxyDomainsFile(List<Domain> domains) {
        this.domains = domains;
    }

    public List<Domain> domains() { return domains; }

    public static List<Domain> read() { return ReadOrCreate(ProxyDomainsFile.class, new ProxyDomainsFile(new ArrayList<>())).domains(); }
}
