package org.solarframework.backup.spring;

import org.solarframework.backup.IBackupService;

/**
 * Where the running implementation lives, mirroring {@code DatabaseRegistry} and {@code MailRegistry}.
 * <pre>
 * import static org.solarframework.backup.spring.BackupRegistry.SolarBackup;
 * SolarBackup.take("before the upgrade");
 * </pre>
 */
public class BackupRegistry {

    public static IBackupService SolarBackup;
}
