package com.openclash.portal.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalBrowserReturnControllerTest {
    @Test
    fun `does not return before the app was backgrounded`() {
        val controller = ExternalBrowserReturnController()

        controller.markBrowserLaunched()

        assertFalse(controller.consumeReturn())
    }

    @Test
    fun `returns once after the browser hand-off completes`() {
        val controller = ExternalBrowserReturnController()

        controller.markBrowserLaunched()
        controller.markAppBackgrounded()

        assertTrue(controller.consumeReturn())
        assertFalse(controller.consumeReturn())
    }

    @Test
    fun `ignores background events without a browser hand-off`() {
        val controller = ExternalBrowserReturnController()

        controller.markAppBackgrounded()

        assertFalse(controller.consumeReturn())
    }
}
