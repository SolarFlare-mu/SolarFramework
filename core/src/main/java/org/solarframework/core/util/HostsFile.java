package org.solarframework.core.util;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Keeps caller-owned hosts entries separate from the machine's other entries. */
public class HostsFile {
    private final Path path;
    private final String commentTag;
    private final Logger logger;

    public HostsFile(String commentTag, Logger logger) {
        this(Path.of(System.getProperty("os.name").toLowerCase().contains("win") ? "C:\\Windows\\System32\\drivers\\etc\\hosts" : "/etc/hosts"), commentTag, logger);
    }
    public HostsFile(Path path, String commentTag, Logger logger) {
        this.path = path;
        this.commentTag = commentTag;
        this.logger = logger;
    }


    public List<String> getHosts() {
        try {
            return Files.readAllLines(path).stream().filter(line -> line.contains(commentTag)).map(line -> line.split(" ")[1]).collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    public void add(String ip, List<String> hosts) throws IOException {
        for (String host : hosts) {
            if (Files.readAllLines(path).stream().noneMatch(line -> line.contains(host))) {
                List<String> lines = Files.readAllLines(path);
                String prefix = lines.getLast().isBlank() ? "" : "\n";
                Files.write(path, (prefix + ip + " " + host + " " + commentTag).getBytes(), StandardOpenOption.APPEND);
                logger.info("Host domain [{}] added to [C:/Windows/System32/drivers/etc/hosts]", host);
            }
        }
    }
    public void remove(List<String> hosts) throws IOException {
        for (String host : hosts) {
            List<String> keptLines = Files.readAllLines(path).stream().filter(line -> !(line.contains(host) && line.contains(commentTag))).collect(Collectors.toList());
            Files.write(path, keptLines, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Host domain [{}] removed from [C:/Windows/System32/drivers/etc/hosts]", host);
        }
    }
    public void clear() throws IOException {
        List<String> keptLines = Files.readAllLines(path).stream().filter(line -> !line.contains(commentTag)).collect(Collectors.toList());
        Files.write(path, keptLines, StandardOpenOption.TRUNCATE_EXISTING);
        logger.info("Deleted all program-made hosts from [C:/Windows/System32/drivers/etc/hosts]");
    }
}
