package org.solarframework.mail.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solarframework.mail.IMailService;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.solarframework.mail.spring.MailRegistry.DefaultMailService;
import static org.solarframework.mail.spring.MailRegistry.SolarMailManager;

/** The mailboxes must survive the round trip through the config file, since that file is what an installation actually posts from after its first run. */
class MailConfigFileTest {

    private Path file;

    @BeforeEach void makeFile() throws Exception {
        file = Files.createTempFile("mail-config", ".json");
        Files.delete(file);
    }
    @AfterEach void removeFile() throws Exception { Files.deleteIfExists(file); }


    @Test void savesAndReloadsEveryMailbox() {
        MailManager manager = new MailManager();
        manager.makeNewService(IMailService.DEFAULT, "smtp.example.com", 587, "agency@example.com", "secret", "no-reply@example.com").setStartTls(false);
        manager.makeNewService("notices", "smtp.other.com", 25, "notices@other.com", "pass", null);
        manager.SaveAsFile(file.toString());

        MailManager reloaded = new MailManager();
        reloaded.LoadFromFile(file.toString());
        assertEquals(2, reloaded.getServices().size());
        assertTrue(reloaded.getDefaultService().isDefault(), "the mailbox named Default must still be the default one");
        assertEquals("smtp.other.com", reloaded.getService("notices").getHost());
        assertEquals(25, reloaded.getService("notices").getPort());
        assertEquals("no-reply@example.com", reloaded.getDefaultService().getFrom());
        assertFalse(reloaded.getDefaultService().isStartTls(), "a setting turned off must not come back on");
    }

    /** A mailbox removed on a managing screen must not return on the next start - which is why loading clears first. */
    @Test void aRemovedMailboxDoesNotComeBack() {
        MailManager manager = new MailManager();
        manager.makeNewService(IMailService.DEFAULT, "smtp.example.com", 587, "agency@example.com", "secret", null);
        manager.makeNewService("notices", "smtp.other.com", 25, "notices@other.com", "pass", null);
        manager.SaveAsFile(file.toString());

        manager.removeService("notices");
        manager.SaveAsFile(file.toString());
        manager.LoadFromFile(file.toString());
        assertFalse(manager.hasService("notices"));
        assertEquals(1, manager.getServices().size());
    }

    /** No file yet is the ordinary first run: it is written from what the manager holds rather than emptying it. */
    @Test void aMissingFileIsWrittenFromWhatIsAlreadyThere() {
        MailManager manager = new MailManager();
        manager.makeNewService(IMailService.DEFAULT, "smtp.example.com", 587, "agency@example.com", "secret", null);
        manager.LoadFromFile(file.toString());
        assertTrue(Files.exists(file));
        assertEquals(1, manager.getServices().size());
        assertEquals("smtp.example.com", manager.getDefaultService().getHost());
    }

    /** The default falls to whatever is left, so a caller naming no mailbox never gets null while one still exists. */
    @Test void removingTheDefaultPromotesAnother() {
        MailManager manager = new MailManager();
        manager.makeNewService(IMailService.DEFAULT, "smtp.example.com", 587, "agency@example.com", "secret", null);
        manager.makeNewService("notices", "smtp.other.com", 25, "notices@other.com", "pass", null);
        manager.removeService(IMailService.DEFAULT);
        assertEquals("notices", manager.getDefaultService().getName());
        assertSame(manager.getDefaultService(), manager.getService("nobody registered this"));
    }

    @Test void closingRemovesTheManagersRegistrations() {
        MailManager manager = new MailManager();
        manager.makeNewService(IMailService.DEFAULT, "smtp.example.com", 587, "agency@example.com", "secret", null);

        manager.close();
        manager.close();

        assertTrue(manager.getServices().isEmpty());
        assertNull(manager.getDefaultService());
        assertNull(SolarMailManager);
        assertNull(DefaultMailService);
    }
}
