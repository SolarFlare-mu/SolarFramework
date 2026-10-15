package org.solarframework.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static void replaceAllStringInDir(File file, String string, String newstring) {
        if (file.getName().contains(string)) {
            File newfile = new File(file.getPath().replaceAll(string, newstring));
            file.renameTo(newfile);
            file = newfile;
        }
        if (file.isDirectory()) {
            if (file.listFiles() != null) {
                for (File f : Objects.requireNonNull(file.listFiles())) {
                    replaceAllStringInDir(f, string, newstring);
                }
            }
        } else if (file.getName().contains(".txt")) {
            try {
                Scanner scan = new Scanner(file, StandardCharsets.UTF_8);
                String texts = "";
                while (scan.hasNextLine()) {
                    texts = texts + scan.nextLine() + System.lineSeparator();
                }
                scan.close();
                texts = texts.replaceAll(string, newstring);
                FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8);
                PrintWriter pw = new PrintWriter(fw);
                pw.print(texts);
                pw.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** The extension in lower case and without its dot, or "" when the name has none. A name that is only a dotted prefix (".env") counts as having none. */
    public static String getExtension(String fileName) {
        String name = safeFileName(fileName, "", null);
        int dot = name.lastIndexOf('.');
        return dot <= 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * The last segment of a name that came from outside, in either slash, without the control characters and quotes
     * that would break a download header, and ending in the extension the file actually is - a wrong one replaced, a
     * missing one added, a null extension leaving the name alone. A header saying {@code cv.docx} on a PDF opens the
     * wrong program on the reader's machine.
     */
    public static String safeFileName(String given, String fallback, String extension) {
        String name = fallback;
        if (!StringUtils.isBlank(given)) {
            String last = given.substring(Math.max(given.lastIndexOf('/'), given.lastIndexOf('\\')) + 1).replaceAll("[\\p{Cntrl}\"]", "").trim();
            if (!last.isEmpty()) name = last;
        }
        String wanted = StringUtils.isBlank(extension) ? "" : extension.replaceFirst("^\\.", "").toLowerCase(Locale.ROOT);
        // matched without regard to case, so a name its author wrote as CV.PDF is left as they wrote it
        if (wanted.isEmpty() || wanted.equals(getExtension(name))) return name;
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + "." + wanted;
    }

    /** Copies a file, or a whole folder into {@code to}, making the folders it needs on the way. */
    public static void copyTree(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) {
            copyFile(from, to);
            return;
        }
        try (Stream<Path> tree = Files.walk(from)) {
            for (Path file : tree.filter(Files::isRegularFile).toList()) copyFile(file, to.resolve(from.relativize(file)));
        }
    }
    private static void copyFile(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Extracts a ZIP beneath target, rejecting entries that escape it. Closes the input stream. */
    public static void unzip(InputStream source, Path target) throws IOException {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(source)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; zip.closeEntry()) {
                Path out = root.resolve(entry.getName().replace('\\', '/')).normalize();
                if (out.equals(root) || !out.startsWith(root)) throw new IOException("ZIP entry escapes target: " + entry.getName());
                if (entry.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out);
                }
            }
        }
    }

    /** Deepest first, since a folder cannot be deleted before what is in it. */
    public static boolean deleteTree(Path folder) {
        try (Stream<Path> tree = Files.walk(folder)) {
            tree.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            return !Files.exists(folder);
        } catch (Exception e) {
            log.error("Could not delete {}: {}", folder, e.getMessage());
            return false;
        }
    }

    /** Every file under it, added up. 0 when there is nothing there or it cannot be read. */
    public static long sizeOf(Path folder) {
        try (Stream<Path> tree = Files.walk(folder)) {
            return tree.filter(Files::isRegularFile).mapToLong(path -> path.toFile().length()).sum();
        } catch (Exception _) {
            return 0;
        }
    }

    public static String getFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    public static String getFileSize(byte[] data) {
        return getFileSize(data.length);
    }

    public static void downloadFileFromURL(String url, String targetPath, StandardCopyOption option) throws IOException {
        Path target = Paths.get(targetPath);
        Files.createDirectories(target.getParent());
        try (InputStream in = URI.create(url).toURL().openStream()) {
            log.info("Downloaded {} file from: [{}]", getFileSize(Files.copy(in, target, option)), url);
        } catch (IOException e) {
            log.error("Failed to download file from: {}", url);
        }
    }

}
