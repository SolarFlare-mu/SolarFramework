package org.solarframework.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvValueTest {

    private static final String NAME = "SOLAR_ENV_VALUE_TEST_KEY";

    @AfterEach void forget() { System.clearProperty(NAME); }

    @Test void aPlainValueIsItself() {
        assertEquals("gsk_plain", EnvValue.resolve("gsk_plain"));
        assertEquals("N/A", EnvValue.resolve("N/A"));
        assertNull(EnvValue.resolve(null));
    }

    @Test void aReferenceIsReadFromTheFlag() {
        System.setProperty(NAME, " gsk_secret ");
        assertEquals("gsk_secret", EnvValue.resolve("${" + NAME + "}"));
        assertEquals("gsk_secret", EnvValue.resolve(" ${ " + NAME + " } "), "hand-written spacing is forgiven");
    }

    @Test void anUnsetReferenceIsNullRatherThanTheReferenceItself() {
        assertNull(EnvValue.resolve("${" + NAME + "}"), "sending the literal ${...} as a key would only fail later, further away");
        System.setProperty(NAME, "  ");
        assertNull(EnvValue.resolve("${" + NAME + "}"));
    }

    @Test void onlyAWholeReferenceCounts() {
        assertNull(EnvValue.nameIn("prefix-${" + NAME + "}"));
        assertNull(EnvValue.nameIn("${}"));
        assertEquals(NAME, EnvValue.nameIn("${" + NAME + "}"));
    }
}
