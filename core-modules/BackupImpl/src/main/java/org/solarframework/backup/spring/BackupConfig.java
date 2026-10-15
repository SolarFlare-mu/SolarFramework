package org.solarframework.backup.spring;

import org.solarframework.backup.IBackupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** The Spring way in. The properties are the first settings; the file has the last word after that, as the mailer's mailboxes do. */
@Configuration
public class BackupConfig {

    /** One folder per area under config/, so everything the backups own is in config/backup/. */
    public static final String FILE = "config/backup/settings.json";

    @Bean
    public IBackupService solarBackup(@Value("${solar.backup.directory:backups}") String directory, @Value("${solar.backup.keep:10}") int keep, @Value("${solar.backup.keep-days:7}") int keepDays, @Value("${solar.backup.include:config}") List<String> include) {
        BackupService service = new BackupService();
        service.setDirectory(directory);
        service.setKeep(keep);
        service.setKeepDays(keepDays);
        service.setIncludedPaths(include);
        service.LoadFromFile();
        return service;
    }
}
