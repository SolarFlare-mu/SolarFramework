package org.solarframework.mail.spring;

import org.solarframework.json.JSONItem;
import org.solarframework.mail.IMailManager;
import org.solarframework.mail.dto.MailboxDefinition;

import java.util.ArrayList;
import java.util.List;

/** Mailboxes as stored in `config/mail/mailboxes.json`. */
class MailConfigFile extends JSONItem<MailConfigFile> {
    private final List<MailboxDefinition> mailboxes = new ArrayList<>();

    public MailConfigFile(IMailManager manager) {
        mailboxes.addAll(manager.getMailboxDefinitions());
    }

    @Override public String file() { return MailConfig.FILE; }
    public List<MailboxDefinition> getMailboxes() { return mailboxes; }

}
