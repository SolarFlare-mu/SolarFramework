package org.solarframework.mail.spring;

import org.solarframework.mail.IMailManager;
import org.solarframework.mail.IMailService;

/**
 * Where the running implementation lives, mirroring {@code DatabaseRegistry} and {@code AIRegistry}.
 * <pre>
 * import static org.solarframework.mail.spring.MailRegistry.DefaultMailService;
 * DefaultMailService.send("someone@example.com", "Subject", "Body");
 * </pre>
 */
public class MailRegistry {

    public static IMailService DefaultMailService;
    public static IMailManager SolarMailManager;
}
