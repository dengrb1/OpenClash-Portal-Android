package com.openclash.portal.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class OpenClashModalCompatibilityTest {
    @Test
    fun `targets OpenClash add configuration and similarly structured modal overlays`() {
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("#config-upload-overlay"))
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("[class*=\"-modal-overlay\"]"))
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("[class*=\"-model-overlay\"]"))
    }

    @Test
    fun `removes backdrop filter that breaks affected WebViews`() {
        assertTrue(OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("backdrop-filter: none !important"))
    }

    @Test
    fun `does not override OpenClash modal visibility layout or click handling`() {
        assertTrue(!OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("display: flex !important"))
        assertTrue(!OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains("pointer-events"))
        assertTrue(!OPENCLASH_MODAL_COMPATIBILITY_SCRIPT.contains(".modal-backdrop"))
    }
}
