package org.solarframework.mail.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.json.JSONItem;
import org.solarframework.mail.IMailManager;
import org.solarframework.mail.IMailService;
import org.solarframework.mail.dto.MailboxDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.solarframework.mail.spring.MailRegistry.DefaultMailService;
import static org.solarframework.mail.spring.MailRegistry.SolarMailManager;

/**
 * {@link IMailManager} over {@link MailService} mailboxes, mirroring {@code DatabaseManager} and {@code AIManager}.
 * {@code new MailManager()} is the whole setup with no context; {@link MailConfig} is the same object as a bean.
 */
public class MailManager implements IMailManager {

    private static final Logger log = LoggerFactory.getLogger(MailManager.class);

    private final Map<String, IMailService> services = new ConcurrentHashMap<>();
    /** Which mailbox a caller naming none gets. Null until one is registered. */
    private volatile String defaultName;

    public MailManager() { SolarMailManager = this; }

    @Override public IMailService getDefaultService() { return defaultName == null ? null : services.get(defaultName); }
    @Override public boolean hasService(String name) { return services.containsKey(name); }
    @Override public Set<IMailService> getServices() { return new LinkedHashSet<>(services.values()); }

    /** A null name asks for the default outright, and must not reach the map, which refuses one. */
    @Override public IMailService getService(String name) { return name == null ? getDefaultService() : services.getOrDefault(name, getDefaultService()); }

    @Override public IMailService makeNewService(String name, String host, String username, String password, String from) {
        IMailService like = getDefaultService();
        return makeNewService(name, host, like == null ? 587 : like.getPort(), username, password, from);
    }

    @Override public IMailService makeNewService(String name, String host, int port, String username, String password, String from) {
        IMailService like = getDefaultService();
        MailService service = new MailService();
        service.setName(name);
        service.setHost(host);
        service.setPort(port);
        service.setUsername(username);
        service.setPassword(password);
        service.setFrom(from);
        service.setAuth(like == null || like.isAuth());
        service.setStartTls(like == null || like.isStartTls());
        addService(service);
        return service;
    }

    @Override public boolean addService(IMailService service) {
        if (service == null || services.put(service.getName(), service) == service) return false;
        rememberDefault(service.getName());
        log.info("Added mailbox {}", service.describe());
        return true;
    }

    @Override public boolean removeService(String name) {
        if (services.remove(name) == null) return false;
        if (name.equals(defaultName)) forgetDefault();
        log.info("Removed mailbox {}", name);
        return true;
    }
    /** Releases registrations from a context that is stopping, so its static entry points cannot retain old mailboxes. */
    public void close() {
        log.info("Closing mail manager with {} mailbox(es)", services.size());
        IMailService registeredDefault = getDefaultService();
        services.clear();
        defaultName = null;
        if (SolarMailManager == this) SolarMailManager = null;
        if (DefaultMailService == registeredDefault) DefaultMailService = null;
    }

    @Override public boolean renameService(String oldName, String newName) {
        if (oldName.equals(newName)) return true;
        IMailService service = services.get(oldName);
        if (service == null || services.containsKey(newName)) return false;
        services.remove(oldName);
        service.setName(newName);
        services.put(newName, service);
        if (oldName.equals(defaultName)) defaultName = newName;
        DefaultMailService = getDefaultService();
        return true;
    }

    /** The first mailbox registered is the default until one actually called "Default" arrives, which then takes it. */
    private void rememberDefault(String name) {
        if (defaultName == null || IMailService.DEFAULT.equals(name)) defaultName = name;
        DefaultMailService = getDefaultService();
    }
    private void forgetDefault() {
        defaultName = services.keySet().stream().findFirst().orElse(null);
        DefaultMailService = getDefaultService();
    }

    @Override public List<MailboxDefinition> getMailboxDefinitions() {
        List<MailboxDefinition> out = new ArrayList<>();
        for (IMailService s : services.values()) out.add(s.definition());
        return out;
    }

    /** Clearing first is what makes a removal on a managing screen stick: the file is the whole truth afterwards. */
    @Override public void LoadFromFile() {
        MailConfigFile file = JSONItem.ReadOrCreate(MailConfigFile.class, new MailConfigFile(this));
        services.clear();
        defaultName = null;
        for (MailboxDefinition d : file.getMailboxes()) {
            IMailService target = makeNewService(d.name(), d.host(), d.port(), d.username(), d.password(), d.from());
            target.setAuth(d.auth());
            target.setStartTls(d.startTls());
        }
        log.info("Loaded {} mailbox(es)", services.size());
    }
    @Override public void SaveAsFile() {
        if (new MailConfigFile(this).Write() == null) log.error("Failed to write mail config file: {}", MailConfig.FILE);
    }

    @Override public String toString() { return "MailManager[" + services.keySet() + "]"; }
}
