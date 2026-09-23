package org.solarframework.core.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** An upload box leans on these answers to refuse a file before it is stored, so each one is pinned to a document built here byte by byte. */
class PDFUtilsTest {

    private static byte[] pdf(String body) { return ("%PDF-1.7\n" + body + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1); }


    @Test void readsARealDocument() {
        byte[] file = pdf("1 0 obj << /Type /Catalog >> endobj");
        assertTrue(PDFUtils.isPDF(file));
        assertEquals("1.7", PDFUtils.getVersion(file));
        assertTrue(PDFUtils.isComplete(file));
        assertFalse(PDFUtils.isEncrypted(file));
    }

    @Test void refusesWhatIsNotAPDF() {
        byte[] notOne = "This is a Word document that was renamed.".getBytes(StandardCharsets.ISO_8859_1);
        assertFalse(PDFUtils.isPDF(notOne));
        assertNull(PDFUtils.getVersion(notOne));
        assertFalse(PDFUtils.isPDF(null));
        assertFalse(PDFUtils.isPDF(new byte[0]));
    }

    /** Some writers stamp bytes before the header; a reader still opens the file, so this must too. */
    @Test void findsAHeaderThatIsNotAtTheVeryStart() {
        byte[] file = ("\n\n   %PDF-2.0\n1 0 obj << /Type /Page >> endobj\n%%EOF").getBytes(StandardCharsets.ISO_8859_1);
        assertTrue(PDFUtils.isPDF(file));
        assertEquals("2.0", PDFUtils.getVersion(file));
    }

    /** An upload cut short keeps its header and loses its end marker - the case isPDF cannot catch on its own. */
    @Test void spotsAFileThatArrivedCutShort() {
        byte[] whole = pdf("1 0 obj << /Type /Page >> endobj");
        byte[] cut = new byte[whole.length / 2];
        System.arraycopy(whole, 0, cut, 0, cut.length);
        assertTrue(PDFUtils.isPDF(cut));
        assertFalse(PDFUtils.isComplete(cut));
        assertTrue(PDFUtils.isComplete(whole));
    }

    @Test void spotsALockedDocument() {
        assertTrue(PDFUtils.isEncrypted(pdf("trailer << /Encrypt 9 0 R /Root 1 0 R >>")));
        assertFalse(PDFUtils.isEncrypted(pdf("trailer << /Root 1 0 R >>")));
    }

    @Test void countsPagesAndNotTheTreeThatHoldsThem() {
        assertEquals(2, PDFUtils.getPageCount(pdf("1 0 obj << /Type /Pages /Count 2 >> endobj\n2 0 obj << /Type /Page >> endobj\n3 0 obj << /Type/Page >> endobj")));
        assertEquals(1, PDFUtils.getPageCount(pdf("2 0 obj << /Type /Page /Contents 4 0 R >> endobj")));
    }

    /** -1 is "cannot tell", never "empty": a PDF that packs its objects into compressed streams hides them from any search, and refusing such an upload would turn a limitation into a rejection. */
    @Test void saysItCannotTellRatherThanAnsweringZero() {
        assertEquals(-1, PDFUtils.getPageCount(pdf("1 0 obj << /Type /ObjStm /Length 40 >> stream ...compressed... endstream endobj")));
        assertEquals(-1, PDFUtils.getPageCount("not a pdf at all".getBytes(StandardCharsets.ISO_8859_1)));
    }
}
