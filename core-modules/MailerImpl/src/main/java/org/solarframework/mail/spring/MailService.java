package org.solarframework.mail.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.mail.IMailService;
import org.solarframework.mail.dto.MailboxDefinition;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * {@link IMailService} over one {@link JavaMailSenderImpl}, owning its sender as {@code DatabaseService} owns its
 * Hikari pool. No stereotype and no properties of its own: that is what lets the mail stack run inside a plugin.
 */
public class MailService implements IMailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private String name = DEFAULT;
    private String host;
    private int port = 587;
    private String username;
    private String password;
    private String from;
    private boolean auth = true;
    private boolean startTls = true;
    private JavaMailSenderImpl sender;

    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public String getHost() { return host; }
    @Override public int getPort() { return port; }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public String getFrom() { return from == null || from.isBlank() ? username : from; }
    @Override public boolean isAuth() { return auth; }
    @Override public boolean isStartTls() { return startTls; }
    @Override public void setFrom(String from) { this.from = from; }
    @Override public void setHost(String host) { this.host = host; reset(); }
    @Override public void setPort(int port) { this.port = port <= 0 ? 587 : port; reset(); }
    @Override public void setUsername(String username) { this.username = username; reset(); }
    @Override public void setPassword(String password) { this.password = password; reset(); }
    @Override public void setAuth(boolean auth) { this.auth = auth; reset(); }
    @Override public void setStartTls(boolean startTls) { this.startTls = startTls; reset(); }

    /** Drops what was built from the old settings so the next letter goes through the new ones. */
    private void reset() { sender = null; }

    private JavaMailSenderImpl sender() {
        if (sender != null) return sender;
        sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        return sender;
    }

    /** Only the recipient and the subject are logged: a body may carry a single-use link or somebody's own words. */
    @Override public void send(String to, String subject, String body) {
        SimpleMailMessage email = new SimpleMailMessage();
        if (getFrom() != null) email.setFrom(getFrom());
        email.setTo(to);
        email.setSubject(subject);
        email.setText(body);
        sender().send(email);
        log.info("Sent \"{}\" to {} through {}", subject, to, name);
    }

    @Override public boolean isAvailable() {
        try {
            sender().testConnection();
            return true;
        } catch (Exception e) {
            log.warn("Mailbox {} is not reachable: {}", name, e.getMessage());
            return false;
        }
    }

    @Override public MailboxDefinition definition() { return new MailboxDefinition(name, host, port, username, password, from, auth, startTls); }

    @Override public String toString() { return describe(); }
}
