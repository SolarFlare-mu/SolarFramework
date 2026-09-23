package org.solarframework.lang;

import org.junit.jupiter.api.AfterEach;

class L10NTest {

    @AfterEach
    void resetBundle() {
        L10N.RB = null;
    }

}
