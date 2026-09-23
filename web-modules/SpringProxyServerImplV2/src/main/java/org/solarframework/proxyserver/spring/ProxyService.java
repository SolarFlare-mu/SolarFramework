package org.solarframework.proxyserver.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.core.util.HostsFile;
import org.solarframework.proxyserver.obj.domains.BaseDomain;
import org.solarframework.proxyserver.obj.domains.Domain;
import org.solarframework.proxyserver.obj.domains.Subdomain;
import org.solarframework.certs.MKCert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.solarframework.proxyserver.spring.DomainRegistry.registry;

public class ProxyService {
    protected static ProxyService PROXYSERVER;
    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private final String COMMENT_TAG = "# Added by SolarFramework-ProxyManager";
    private final HostsFile hostsFile = new HostsFile(COMMENT_TAG, log);
    private final List<Domain> domains;


    public List<String> getAbsoluteHosts() { return hostsFile.getHosts(); }
    public List<String> getHosts() {
        return getDomains().stream().flatMap(domain -> domain.getHosts().stream()).toList();
    }
    public void addHostEntry(String ip, String hosts) throws IOException {
        addHostEntry(ip, List.of(hosts));
        if (registry != null) registry.UpdateDomains();
    }
    public void addHostEntry(String ip, List<String> hosts) throws IOException {
        hostsFile.add(ip, hosts);
        if (registry != null) registry.UpdateDomains();
    }
    public void removeHostEntry(String hosts) throws IOException {
        removeHostEntry(List.of(hosts));
        if (registry != null) registry.UpdateDomains();
    }
    public void removeHostEntry(List<String> hosts) throws IOException {
        hostsFile.remove(hosts);
        if (registry != null) registry.UpdateDomains();
    }
    public void clearHostEntries() throws IOException { hostsFile.clear(); }



    public List<Domain> getDomains() {
        return domains;
    }
    public List<Subdomain> getSubDomains() {
        return domains.stream().flatMap(domain -> domain.getSubdomains().stream()).toList();
    }
    public List<BaseDomain<?>> getAllDomains() {
        List<BaseDomain<?>> domains = new ArrayList<>();
        domains.addAll(PROXYSERVER.getDomains());
        domains.addAll(PROXYSERVER.getSubDomains());
        return domains;
    }

    public BaseDomain<?> getDomainOfHost(String host) {
        return getAllDomains().stream().filter(dom -> Objects.equals(dom.getHost(), host)).findFirst().orElse(null);
    }

    protected ProxyService(List<Domain> domains, boolean regenCerts) throws Exception {
        this.domains = domains;
        if (regenCerts) {
            clearHostEntries();
            for (Domain dom : domains) addHostEntry(dom.getIp(), dom.getHosts());
            MKCert.GenerateCertificateFor(getHosts());
        }
        List<String> trueHosts = getAbsoluteHosts();
        for (Domain domain : getDomains()) {
            if (trueHosts.contains(domain.getName())) {
                if (!domain.getPath().startsWith("http")) Files.createDirectories(Path.of(domain.getPath()));
                log.info("Loaded domain: {}", domain.getName());
            } else log.error("Failed to load domain: {}", domain.getName());

            for (Subdomain sub : domain.getSubdomains()) {
                if (trueHosts.contains(sub.getHost())) {
                    if (!sub.getPath().startsWith("http")) Files.createDirectories(Path.of(sub.getPath()));
                    log.info("└ Loaded subdomain: {}", sub.getHost());
                } else log.error("└ Failed to load subdomain: {}", sub.getHost());
            }
        }
    }


    public String stripPort(String host) {
        if (host == null) return null;
        int i = host.indexOf(':');
        return (i == -1 ? host : host.substring(0, i)).toLowerCase();
    }
}
