package org.solarframework.core.util;

import java.nio.charset.StandardCharsets;

/**
 * What can be told about a PDF from its bytes alone, with no library to carry. An upload box needs exactly this: a
 * name ending in {@code .pdf} proves nothing, since the browser takes it from the file the reader chose.
 */
public final class PDFUtils {

    private static final byte[] HEADER = "%PDF-".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] END = "%%EOF".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] ENCRYPT = "/Encrypt".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] TYPE = "/Type".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] PAGE = "/Page".getBytes(StandardCharsets.ISO_8859_1);

    private PDFUtils() {}

    public static boolean isPDF(byte[] file) { return headerAt(file) >= 0; }

    /** What the file says of itself, "1.7" or "2.0", or null when it is not a PDF at all. */
    public static String getVersion(byte[] file) {
        int at = headerAt(file);
        if (at < 0) return null;
        StringBuilder version = new StringBuilder();
        for (int i = at + HEADER.length; i < file.length && version.length() < 3; i++) {
            char c = (char) (file[i] & 0xFF);
            if (c != '.' && (c < '0' || c > '9')) break;
            version.append(c);
        }
        return version.isEmpty() ? null : version.toString();
    }

    /** An upload cut short by a dropped connection keeps its header and loses its end marker. */
    public static boolean isComplete(byte[] file) { return file != null && indexOf(file, END, Math.max(0, file.length - 2048)) >= 0; }

    /**
     * Whether the document is locked. The whole file is searched rather than only the trailer, so a stream whose
     * bytes happen to spell {@code /Encrypt} is called locked when it is not - wrong in the safe direction, since it
     * asks for a plainer copy rather than storing one nobody can open.
     */
    public static boolean isEncrypted(byte[] file) { return file != null && indexOf(file, ENCRYPT, 0) >= 0; }

    /**
     * How many pages, or <b>-1 when it cannot be told</b> - never 0 for a file it failed to read. A PDF that packs
     * its objects into compressed streams, which every writer since 1.5 may do, keeps them where a search cannot see
     * them. Treat -1 as "do not know" and let the file through.
     */
    public static int getPageCount(byte[] file) {
        if (!isPDF(file)) return -1;
        int pages = 0;
        for (int at = indexOf(file, TYPE, 0); at >= 0; at = indexOf(file, TYPE, at + 1)) {
            int after = skipSpace(file, at + TYPE.length);
            if (!matchesAt(file, PAGE, after)) continue;
            int end = after + PAGE.length;
            if (end < file.length && isNameCharacter(file[end])) continue; // "/Pages" is the tree that holds them
            pages++;
        }
        return pages == 0 ? -1 : pages;
    }

    /** Some writers stamp bytes before the header, so the whole first kilobyte is searched, as readers do. */
    private static int headerAt(byte[] file) {
        if (file == null || file.length < HEADER.length + 3) return -1;
        return indexOf(file, HEADER, 0, Math.min(file.length, 1024));
    }

    private static boolean isNameCharacter(byte b) {
        char c = (char) (b & 0xFF);
        return c == '_' || c == '-' || c == '+' || Character.isLetterOrDigit(c);
    }
    private static int skipSpace(byte[] file, int from) {
        int at = from;
        while (at < file.length && Character.isWhitespace((char) (file[at] & 0xFF))) at++;
        return at;
    }
    private static boolean matchesAt(byte[] file, byte[] wanted, int at) {
        if (at < 0 || at + wanted.length > file.length) return false;
        for (int i = 0; i < wanted.length; i++) if (file[at + i] != wanted[i]) return false;
        return true;
    }
    private static int indexOf(byte[] file, byte[] wanted, int from) { return indexOf(file, wanted, from, file.length); }
    private static int indexOf(byte[] file, byte[] wanted, int from, int until) {
        for (int at = Math.max(0, from); at <= until - wanted.length; at++) if (matchesAt(file, wanted, at)) return at;
        return -1;
    }
}
