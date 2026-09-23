package org.solarframework.proxyserver;

import org.solarframework.proxyserver.obj.domains.Domain;
import org.solarframework.proxyserver.spring.ProxyBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
        setupProxy();
    }

    private static void setupProxy() throws Exception {
        Domain H1 = new Domain(ProxyBuilder.LOCALHOST, "mysite.com", "https://www.youtube.com");

        Domain H2 = new Domain(ProxyBuilder.LOCALHOST, "test2.com", "C:/Website/test2/");
        H2.addSubdomain("www", "C:/Website/test2_www/");

        Domain H3 = new Domain(ProxyBuilder.LOCALHOST, "myothersite.com", "http://localhost:8080");

        ProxyBuilder builder = new ProxyBuilder()
                .registerDomains(H1, H2, H3)
                .regenerateCerts()
        ;
        builder.build();
    }
}
