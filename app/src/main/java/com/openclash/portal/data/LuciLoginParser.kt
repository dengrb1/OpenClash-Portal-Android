package com.openclash.portal.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup

class LuciLoginParser {
    fun parse(pageUrl: HttpUrl, html: String): LoginForm? {
        val document = Jsoup.parse(html, pageUrl.toString())
        val form = document.select("form:has(input[type=password])").first()
            ?: document.select("form").first()
            ?: return null

        val action = form.attr("action").ifBlank { pageUrl.toString() }
        val actionUrl = pageUrl.resolve(action) ?: action.toHttpUrl()
        val usernameInput = form.select("input[type=text], input[name*=user], input[name*=login]").first()
        val passwordInput = form.select("input[type=password]").first()
        val hiddenInputs = form.select("input[type=hidden]")
            .associate { input -> input.attr("name") to input.attr("value") }
            .filterKeys { it.isNotBlank() }

        return LoginForm(
            actionUrl = actionUrl,
            method = form.attr("method").ifBlank { "post" }.lowercase(),
            hiddenFields = hiddenInputs,
            usernameField = usernameInput?.attr("name").orEmpty().ifBlank { "luci_username" },
            passwordField = passwordInput?.attr("name").orEmpty().ifBlank { "luci_password" },
        )
    }

    fun looksLikeLoginPage(html: String): Boolean {
        val lowered = html.lowercase()
        return lowered.contains("luci_password") ||
            lowered.contains("input type=\"password\"") ||
            lowered.contains("login")
    }
}

data class LoginForm(
    val actionUrl: HttpUrl,
    val method: String,
    val hiddenFields: Map<String, String>,
    val usernameField: String,
    val passwordField: String,
)
