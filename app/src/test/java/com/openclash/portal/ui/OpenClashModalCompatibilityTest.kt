package com.openclash.portal.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class OpenClashModalCompatibilityTest {
    @Test
    fun `targets OpenClash add configuration and similarly structured modal overlays`() {
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("#config-upload-overlay"))
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("#config-upload-model"))
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("[class*=\"-modal-overlay\"].show"))
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("[class*=\"-model-overlay\"].show"))
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("max-height: calc(100vh - 32px)"))
    }

    @Test
    fun `removes backdrop filter that breaks affected WebViews`() {
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("backdrop-filter: none !important"))
    }
}
