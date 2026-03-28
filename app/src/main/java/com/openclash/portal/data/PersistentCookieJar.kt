package com.openclash.portal.data

import android.webkit.CookieManager
import java.util.concurrent.ConcurrentHashMap
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

class PersistentCookieJar(
    private val secureStorage: SecureStorage,
) : CookieJar {
    private val cookies = ConcurrentHashMap<String, Cookie>()

    init {
        restore()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            this.cookies[cookie.storageKey()] = cookie
        }
        removeExpired()
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        removeExpired()
        return cookies.values.filter { it.matches(url) }
    }

    fun clear() {
        cookies.clear()
        secureStorage.clearSession()
    }

    fun syncToWebView(urls: List<String>) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        urls.distinct().forEach { rawUrl ->
            val httpUrl = rawUrl.toHttpUrlOrNull() ?: return@forEach
            loadForRequest(httpUrl).forEach { cookie ->
                cookieManager.setCookie(rawUrl, cookie.toString())
            }
        }
        cookieManager.flush()
    }

    fun syncFromWebView(rawUrl: String) {
        val httpUrl = rawUrl.toHttpUrlOrNull() ?: return
        val cookieHeader = CookieManager.getInstance().getCookie(rawUrl).orEmpty()
        if (cookieHeader.isBlank()) return
        cookieHeader.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { entry ->
                val name = entry.substringBefore("=")
                val value = entry.substringAfter("=")
                val existing = cookies.values.firstOrNull { it.name == name && it.matches(httpUrl) }
                val builder = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .hostOnlyDomain(httpUrl.host)
                    .path(existing?.path ?: "/")
                if (existing?.secure == true) builder.secure()
                if (existing?.httpOnly == true) builder.httpOnly()
                val cookie = builder.build()
                cookies[cookie.storageKey()] = cookie
            }
        persist()
    }

    private fun persist() {
        val jsonArray = JSONArray()
        cookies.values.forEach { cookie ->
            jsonArray.put(
                JSONObject()
                    .put("name", cookie.name)
                    .put("value", cookie.value)
                    .put("domain", cookie.domain)
                    .put("path", cookie.path)
                    .put("expiresAt", cookie.expiresAt)
                    .put("secure", cookie.secure)
                    .put("httpOnly", cookie.httpOnly)
                    .put("hostOnly", cookie.hostOnly)
                    .put("persistent", cookie.persistent),
            )
        }
        secureStorage.saveCookies(jsonArray.toString())
    }

    private fun restore() {
        val raw = secureStorage.loadCookies().orEmpty()
        if (raw.isBlank()) return
        val jsonArray = JSONArray(raw)
        repeat(jsonArray.length()) { index ->
            val item = jsonArray.getJSONObject(index)
            val builder = Cookie.Builder()
                .name(item.getString("name"))
                .value(item.getString("value"))
                .path(item.optString("path", "/"))
            if (item.optBoolean("hostOnly", false)) {
                builder.hostOnlyDomain(item.getString("domain"))
            } else {
                builder.domain(item.getString("domain"))
            }
            if (item.optBoolean("persistent", false)) {
                builder.expiresAt(item.optLong("expiresAt", Long.MAX_VALUE))
            }
            if (item.optBoolean("secure", false)) builder.secure()
            if (item.optBoolean("httpOnly", false)) builder.httpOnly()
            val cookie = builder.build()
            cookies[cookie.storageKey()] = cookie
        }
        removeExpired()
    }

    private fun removeExpired() {
        val now = System.currentTimeMillis()
        val before = cookies.size
        cookies.entries.removeAll { (_, cookie) ->
            cookie.persistent && cookie.expiresAt < now
        }
        if (before != cookies.size) {
            persist()
        }
    }

    private fun Cookie.storageKey(): String {
        return listOf(domain, path, name, if (hostOnly) "host" else "domain").joinToString("|")
    }
}

