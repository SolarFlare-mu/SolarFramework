package org.solarframework.mail.dto;

import org.solarframework.mail.IMailService;

/** One {@code IMailService}'s settings, flattened for a config file. */
public record MailboxDefinition(String name, String host, int port, String username, String password, String from, boolean auth, boolean startTls) {

    public MailboxDefinition { port = port <= 0 ? 587 : port; }

    public boolean isDefault() { return IMailService.DEFAULT.equals(name); }

    @Override public String toString() { return name + " -> " + host + ":" + port + (from == null ? "" : " (" + from + ")"); }
}
