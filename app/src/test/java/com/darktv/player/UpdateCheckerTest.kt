package com.darktv.player

import org.junit.Assert.*
import org.junit.Test

class UpdateCheckerTest {
    @Test fun comparesStableVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("v1.10.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("2.0", "1.99.0"))
        assertFalse(UpdateChecker.isNewer("v1.5", "1.5.0"))
        assertFalse(UpdateChecker.isNewer("1.4.9", "1.5.0"))
        assertFalse(UpdateChecker.isNewer("v2.0.0-beta", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("latest", "1.0.0"))
    }
}
