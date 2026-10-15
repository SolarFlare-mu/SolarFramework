package org.solarframework.mail.spring;

import org.solarframework.core.util.SolarHome;
import org.solarframework.mail.IMailManager;
import org.solarframework.mail.IMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;

/**
 * The Spring way in. The properties seed the default mailbox on a first run; the file has the last word after that,
 * which is how a mailbox added on a screen survives a restart and a removed one stays removed.
 */
@Configuration
public class MailConfig {

    /** One folder per area under config/, so everything the mailboxes own is in config/mail/. */
    public static final String FILE = "config/mail/mailboxes.json";

    /** Seeding is gated on there being no file yet because {@link MailManager#LoadFromFile} clears and rebuilds from the file: on any later start the seeded mailbox is added and thrown away in the same breath. */
    @Bean(destroyMethod = "close")
    public IMailManager solarMailManager(@Value("${spring.mail.host:#{null}}") String host, @Value("${spring.mail.port:587}") int port, @Value("${spring.mail.username:#{null}}") String username, @Value("${spring.mail.password:#{null}}") String password, @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean auth, @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTls) {
        MailManager manager = new MailManager();
        if (host != null && Files.notExists(SolarHome.pathTo(FILE))) {
            IMailService seeded = manager.makeNewService(IMailService.DEFAULT, host, port, username, password, username);
            seeded.setAuth(auth);
            seeded.setStartTls(startTls);
        }
        manager.LoadFromFile();
        return manager;
    }
}
