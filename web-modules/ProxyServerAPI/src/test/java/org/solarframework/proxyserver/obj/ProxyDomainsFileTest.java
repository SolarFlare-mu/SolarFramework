package org.solarframework.proxyserver.obj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solarframework.json.JSONItem;
import org.solarframework.proxyserver.obj.domains.Domain;
import org.solarframework.proxyserver.obj.domains.ProxyDomainsFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A domain and its subdomains must survive the round trip through the JSON file, since a restart has nothing else to rebuild the proxy from. */
class ProxyDomainsFileTest {

    private Path file;

    @BeforeEach void makeFile() throws Exception {
        file = Files.createTempFile("proxy-domains", ".json");
        Files.delete(file);
    }
    @AfterEach void removeFile() throws Exception { Files.deleteIfExists(file); }


    @Test void savesAndReloadsEveryDomainWithItsSubdomains() {
        Domain domain = new Domain("127.0.0.1", "mysite.com");
        domain.addSubdomain("www");
        domain.addSubdomain("admin", "http://localhost:8090");
        new ProxyDomainsFile(List.of(domain)).Write(file.toString());

        List<Domain> reloaded = JSONItem.ReadJSON(file.toString(), ProxyDomainsFile.class).domains();
        assertEquals(1, reloaded.size());
        Domain reloadedDomain = reloaded.getFirst();
        assertEquals("mysite.com", reloadedDomain.getName());
        assertEquals(2, reloadedDomain.getSubdomains().size());
        assertEquals("www.mysite.com", reloadedDomain.getSubdomains().getFirst().getHost(), "the parent link must be restored, since Gson never runs Subdomain's constructor");
        assertEquals("http://localhost:8090", reloadedDomain.getSubdomains().getLast().getPath());
    }

    /** No file yet must not fail the boot - it is the ordinary first run with nothing configured. */
    @Test void aMissingFileLoadsAsEmptyAndWritesItself() {
        ProxyDomainsFile loaded = JSONItem.ReadOrCreate(file.toString(), ProxyDomainsFile.class, new ProxyDomainsFile(List.of()));
        assertTrue(loaded.domains().isEmpty());
        assertTrue(Files.exists(file));
    }
}
