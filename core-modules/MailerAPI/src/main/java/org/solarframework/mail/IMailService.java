package org.solarframework.mail;

import org.solarframework.mail.dto.MailboxDefinition;

/** One mailbox: one SMTP endpoint, one sender address, and every letter posted through it - the mail counterpart of a data source. */
public interface IMailService {

    String DEFAULT = "Default";

    String getName();
    void setName(String name);
    String getHost();
    void setHost(String host);
    int getPort();
    void setPort(int port);
    String getUsername();
    void setUsername(String username);
    String getPassword();
    void setPassword(String password);
    boolean isAuth();
    void setAuth(boolean auth);
    boolean isStartTls();
    void setStartTls(boolean startTls);

    /** Blank leaves the sender address to the server, which usually stamps the username. */
    String getFrom();
    void setFrom(String from);

    default boolean isDefault() { return DEFAULT.equals(getName()); }

    /** Throws when the server refuses the letter, so a caller can tell the reader something went wrong. */
    void send(String to, String subject, String body);

    /** Opens a connection and closes it again, without posting anything. */
    boolean isAvailable();

    MailboxDefinition definition();

    default String describe() { return getName() + " -> " + getHost() + ":" + getPort(); }
}
