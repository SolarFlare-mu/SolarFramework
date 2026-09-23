package org.solarframework.mail;

import org.solarframework.mail.dto.MailboxDefinition;

import java.util.List;
import java.util.Set;

/** Every mailbox the installation posts from, read and written as one JSON file, as {@code IDatabaseManager} holds its data sources. */
public interface IMailManager {

    /** The one named "Default" if there is one, otherwise the first registered, otherwise null. */
    IMailService getDefaultService();

    /** An unknown name falls back to the default, so a caller naming a mailbox somebody removed still posts. */
    IMailService getService(String name);

    boolean hasService(String name);
    Set<IMailService> getServices();
    boolean addService(IMailService service);
    boolean removeService(String name);
    boolean renameService(String oldName, String newName);
    List<MailboxDefinition> getMailboxDefinitions();

    /** Copies the default's port, authentication and TLS, which is all a second mailbox on the same server needs. */
    IMailService makeNewService(String name, String host, String username, String password, String from);

    IMailService makeNewService(String name, String host, int port, String username, String password, String from);

    /** The file becomes the whole truth: a mailbox it does not name stops existing. */
    void LoadFromFile(String path);

    void SaveAsFile(String path);
}
