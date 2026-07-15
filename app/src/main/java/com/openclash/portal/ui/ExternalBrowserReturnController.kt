package com.openclash.portal.ui

/**
 * Tracks a browser hand-off without treating the activity's current resume state as a return.
 * A return is valid only after the app has actually been backgrounded by the launched browser.
 */
internal class ExternalBrowserReturnController {
    private var awaitingReturn = false
    private var appWasBackgrounded = false

    fun markBrowserLaunched() {
        awaitingReturn = true
        appWasBackgrounded = false
    }

    fun markAppBackgrounded() {
        if (awaitingReturn) {
            appWasBackgrounded = true
        }
    }

    fun consumeReturn(): Boolean {
        if (!awaitingReturn || !appWasBackgrounded) return false

        awaitingReturn = false
        appWasBackgrounded = false
        return true
    }
}
