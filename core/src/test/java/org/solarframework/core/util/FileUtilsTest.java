package org.solarframework.core.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void getFileSizeFormatsBytes() {
        assertEquals("500 B", FileUtils.getFileSize(500L));
    }

    /** The name arrives from whoever uploaded the file, so it may carry a path in either slash, characters that would break a download header, or nothing at all. */
    @Test
    void safeFileNameKeepsOnlyTheNameItself() {
        assertEquals("cv.pdf", FileUtils.safeFileName("C:\\Users\\someone\\Desktop\\cv.pdf", "document.pdf", null));
        assertEquals("cv.pdf", FileUtils.safeFileName("/home/someone/cv.pdf", "document.pdf", null));
        assertEquals("cv.pdf", FileUtils.safeFileName("cv\".pdf", "document.pdf", null));
        assertEquals("document.pdf", FileUtils.safeFileName("   ", "document.pdf", null));
        assertEquals("document.pdf", FileUtils.safeFileName(null, "document.pdf", null));
        assertEquals("document.pdf", FileUtils.safeFileName("folder/", "document.pdf", null));
    }

    @Test
    void copiesAndDeletesAWholeTreeAndAddsItUp() throws Exception {
        Path from = Files.createTempDirectory("tree");
        Files.writeString(from.resolve("top.txt"), "12345");
        Files.createDirectories(from.resolve("inside"));
        Files.writeString(from.resolve("inside").resolve("deep.txt"), "123");
        Path to = Files.createTempDirectory("tree-copy").resolve("copied");

        FileUtils.copyTree(from, to);
        assertEquals("12345", Files.readString(to.resolve("top.txt")));
        assertEquals("123", Files.readString(to.resolve("inside").resolve("deep.txt")), "a folder inside the folder comes too");
        assertEquals(8, FileUtils.sizeOf(to));
        assertEquals(0, FileUtils.sizeOf(to.resolve("nothing here")));

        assertTrue(FileUtils.deleteTree(to));
        assertFalse(Files.exists(to));
        FileUtils.deleteTree(from);
    }

    @Test
    void getExtensionReadsWhatIsAfterTheLastDot() {
        assertEquals("pdf", FileUtils.getExtension("cv.PDF"));
        assertEquals("gz", FileUtils.getExtension("/tmp/backup.tar.gz"));
        assertEquals("", FileUtils.getExtension("README"));
        assertEquals("", FileUtils.getExtension(".env"), "a dotted prefix is a hidden name, not an extension");
        assertEquals("", FileUtils.getExtension("cv."));
        assertEquals("", FileUtils.getExtension(null));
    }

    /** The name must say what the file is: a download header reading cv.docx on a PDF opens the wrong program. */
    @Test
    void safeFileNameCanGuaranteeTheExtension() {
        assertEquals("cv.pdf", FileUtils.safeFileName("cv.docx", "document.pdf", "pdf"));
        assertEquals("cv.pdf", FileUtils.safeFileName("cv", "document.pdf", ".pdf"));
        assertEquals("cv.PDF", FileUtils.safeFileName("C:\\Users\\someone\\cv.PDF", "document.pdf", "pdf"), "already a pdf, so the name is left as its author wrote it");
        assertEquals("document.pdf", FileUtils.safeFileName(null, "document.pdf", "pdf"));
        assertEquals("cv.docx", FileUtils.safeFileName("cv.docx", "document", ""), "no extension asked for leaves the name alone");
    }
    @Test
    void getFileSizeFormatsKilobytes() {
        assertEquals("2.0 KB", FileUtils.getFileSize(2048L));
    }
    @Test
    void getFileSizeFormatsMegabytes() {
        assertEquals("5.0 MB", FileUtils.getFileSize(5L * 1024 * 1024));
    }
    @Test
    void getFileSizeFormatsGigabytes() {
        assertEquals("2.0 GB", FileUtils.getFileSize(2L * 1024 * 1024 * 1024));
    }
    @Test
    void getFileSizeFromByteArrayUsesArrayLength() {
        assertEquals("3 B", FileUtils.getFileSize(new byte[3]));
    }
}
