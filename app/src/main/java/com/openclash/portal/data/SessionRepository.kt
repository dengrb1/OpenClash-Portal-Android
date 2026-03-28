package com.openclash.portal.data

import com.openclash.portal.model.OpenClashStatus
import com.openclash.portal.model.ResolvedPortalUrls
import com.openclash.portal.model.RouterProfile
import com.openclash.portal.model.RouterProtocol
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SessionRepository(
    private val preferencesStore: RouterPreferencesStore,
    private val secureStorage: SecureStorage,
    private val cookieJar: PersistentCookieJar,
    private val httpClientFactory: HttpClientFactory,
    private val loginParser: LuciLoginParser,
    private val urlResolver: OpenClashUrlResolver,
) {
    suspend fun restoreProfile(): RouterProfile? = preferencesStore.loadProfile()

    fun loadPassword(): String = secureStorage.loadPassword().orEmpty()

    suspend fun loadTrustedHosts(): Set<String> = preferencesStore.loadTrustedHosts()

    suspend fun trustHost(host: String) {
        preferencesStore.trustHost(host)
    }

    suspend fun saveProfile(profile: RouterProfile, password: String) {
        preferencesStore.saveProfile(profile)
        if (password.isNotBlank()) {
            secureStorage.savePassword(password)
        }
    }

    suspend fun clearSession(clearTrustedHosts: Boolean) {
        cookieJar.clear()
        secureStorage.clearAll()
        if (clearTrustedHosts) {
            preferencesStore.clearTrustedHosts()
        }
    }

    fun syncCookiesToWebView(urls: List<String>) {
        cookieJar.syncToWebView(urls)
    }

    fun syncCookiesFromWebView(url: String) {
        cookieJar.syncFromWebView(url)
    }

    suspend fun connect(
        profile: RouterProfile,
        password: String,
        trustedHosts: Set<String>,
    ): ConnectResult = withContext(Dispatchers.IO) {
        val unsafeHosts = effectiveTrustedHosts(profile, trustedHosts)
        when (val validation = validateSession(profile, unsafeHosts)) {
            ValidationResult.Valid -> fetchResolvedState(profile, unsafeHosts)
            ValidationResult.InvalidSession -> loginThenResolve(profile, password, unsafeHosts)
            ValidationResult.OpenClashUnavailable -> ConnectResult.Error(ConnectError.OpenClashUnavailable)
            ValidationResult.Unreachable -> ConnectResult.Error(ConnectError.RouterUnreachable)
            is ValidationResult.SslUntrusted -> ConnectResult.SslUntrusted(validation.host)
            is ValidationResult.Unknown -> ConnectResult.Error(ConnectError.Unknown(validation.message))
        }
    }

    private fun loginThenResolve(
        profile: RouterProfile,
        password: String,
        unsafeHosts: Set<String>,
    ): ConnectResult {
        if (password.isBlank()) return ConnectResult.Error(ConnectError.InvalidCredentials)
        cookieJar.clear()
        return when (val loginResult = login(profile, password, unsafeHosts)) {
            LoginResult.Success -> fetchResolvedState(profile, unsafeHosts)
            LoginResult.InvalidCredentials -> ConnectResult.Error(ConnectError.InvalidCredentials)
            LoginResult.Unreachable -> ConnectResult.Error(ConnectError.RouterUnreachable)
            is LoginResult.SslUntrusted -> ConnectResult.SslUntrusted(loginResult.host)
            is LoginResult.Unknown -> ConnectResult.Error(ConnectError.Unknown(loginResult.message))
        }
    }

    private fun fetchResolvedState(profile: RouterProfile, unsafeHosts: Set<String>): ConnectResult {
        return when (val statusResult = fetchStatus(profile, unsafeHosts)) {
            is StatusResult.Success -> ConnectResult.Success(
                status = statusResult.status,
                urls = urlResolver.resolve(profile, statusResult.status),
            )

            StatusResult.InvalidSession -> ConnectResult.Error(ConnectError.InvalidCredentials)
            StatusResult.OpenClashUnavailable -> ConnectResult.Error(ConnectError.OpenClashUnavailable)
            StatusResult.Unreachable -> ConnectResult.Error(ConnectError.RouterUnreachable)
            is StatusResult.SslUntrusted -> ConnectResult.SslUntrusted(statusResult.host)
            is StatusResult.Unknown -> ConnectResult.Error(ConnectError.Unknown(statusResult.message))
        }
    }

    private fun login(
        profile: RouterProfile,
        password: String,
        unsafeHosts: Set<String>,
    ): LoginResult {
        val client = httpClientFactory.create(allowUnsafeSslForHosts = unsafeHosts)
        val request = Request.Builder()
            .url("${profile.baseUrl}/cgi-bin/luci/")
            .get()
            .build()
        val loginPage = executeRequest(client, request)
        when (loginPage) {
            is NetworkResult.SslUntrusted -> return LoginResult.SslUntrusted(loginPage.host)
            is NetworkResult.Failure -> return LoginResult.Unknown(loginPage.message)
            null -> return LoginResult.Unreachable
            is NetworkResult.Success -> Unit
        }

        val form = loginParser.parse(loginPage.url, loginPage.body)
            ?: return LoginResult.Unknown("Unable to parse the LuCI login form.")

        val formBody = FormBody.Builder().apply {
            form.hiddenFields.forEach { (name, value) -> add(name, value) }
            add(form.usernameField, profile.username)
            add(form.passwordField, password)
        }.build()

        val submitRequest = if (form.method == "get") {
            val submitUrl = form.actionUrl.newBuilder().apply {
                form.hiddenFields.forEach { (name, value) -> addQueryParameter(name, value) }
                addQueryParameter(form.usernameField, profile.username)
                addQueryParameter(form.passwordField, password)
            }.build()
            Request.Builder()
                .url(submitUrl)
                .addHeader("Referer", loginPage.url.toString())
                .get()
                .build()
        } else {
            Request.Builder()
                .url(form.actionUrl)
                .addHeader("Referer", loginPage.url.toString())
                .post(formBody)
                .build()
        }

        when (val submit = executeRequest(client, submitRequest)) {
            is NetworkResult.SslUntrusted -> return LoginResult.SslUntrusted(submit.host)
            is NetworkResult.Failure -> return LoginResult.Unknown(submit.message)
            null -> return LoginResult.Unreachable
            is NetworkResult.Success -> Unit
        }

        return when (validateSession(profile, unsafeHosts)) {
            ValidationResult.Valid -> LoginResult.Success
            ValidationResult.InvalidSession -> LoginResult.InvalidCredentials
            ValidationResult.OpenClashUnavailable -> LoginResult.Success
            ValidationResult.Unreachable -> LoginResult.Unreachable
            is ValidationResult.SslUntrusted -> LoginResult.SslUntrusted(profile.normalizedHost)
            is ValidationResult.Unknown -> LoginResult.Unknown("Post-login session validation failed.")
        }
    }

    private fun validateSession(profile: RouterProfile, unsafeHosts: Set<String>): ValidationResult {
        val client = httpClientFactory.create(allowUnsafeSslForHosts = unsafeHosts)
        val request = Request.Builder()
            .url(profile.openClashUrl())
            .get()
            .build()
        return when (val result = executeRequest(client, request)) {
            is NetworkResult.Success -> when {
                result.code == 404 -> ValidationResult.OpenClashUnavailable
                loginParser.looksLikeLoginPage(result.body) -> ValidationResult.InvalidSession
                result.code in 200..299 -> ValidationResult.Valid
                else -> ValidationResult.Unknown("Unexpected OpenClash page response: ${result.code}")
            }

            is NetworkResult.SslUntrusted -> ValidationResult.SslUntrusted(result.host)
            is NetworkResult.Failure -> ValidationResult.Unknown(result.message)
            null -> ValidationResult.Unreachable
        }
    }

    private fun fetchStatus(profile: RouterProfile, unsafeHosts: Set<String>): StatusResult {
        val client = httpClientFactory.create(allowUnsafeSslForHosts = unsafeHosts)
        val request = Request.Builder()
            .url("${profile.baseUrl}/cgi-bin/luci/admin/services/openclash/status")
            .get()
            .build()
        return when (val result = executeRequest(client, request)) {
            is NetworkResult.Success -> {
                when {
                    result.code == 404 -> StatusResult.OpenClashUnavailable
                    loginParser.looksLikeLoginPage(result.body) -> StatusResult.InvalidSession
                    else -> runCatching {
                        StatusResult.Success(OpenClashStatus.fromJson(JSONObject(result.body)))
                    }.getOrElse {
                        StatusResult.Unknown("Unable to parse the OpenClash status response.")
                    }
                }
            }

            is NetworkResult.SslUntrusted -> StatusResult.SslUntrusted(result.host)
            is NetworkResult.Failure -> StatusResult.Unknown(result.message)
            null -> StatusResult.Unreachable
        }
    }

    private fun executeRequest(client: OkHttpClient, request: Request): NetworkResult? {
        return runCatching {
            client.newCall(request).execute().use { response ->
                NetworkResult.Success(
                    code = response.code,
                    body = response.body?.string().orEmpty(),
                    url = response.request.url,
                )
            }
        }.getOrElse { throwable ->
            when (throwable) {
                is SSLException -> NetworkResult.SslUntrusted(request.url.host)
                is SocketTimeoutException, is IOException -> null
                else -> NetworkResult.Failure(throwable.message ?: "Unknown network failure.")
            }
        }
    }

    private fun effectiveTrustedHosts(profile: RouterProfile, trustedHosts: Set<String>): Set<String> {
        return if (profile.protocol == RouterProtocol.HTTPS) {
            trustedHosts.map { it.lowercase() }.toSet()
        } else {
            emptySet()
        }
    }
}

sealed interface ConnectResult {
    data class Success(
        val status: OpenClashStatus,
        val urls: ResolvedPortalUrls,
    ) : ConnectResult

    data class SslUntrusted(val host: String) : ConnectResult
    data class Error(val error: ConnectError) : ConnectResult
}

sealed interface ConnectError {
    data object RouterUnreachable : ConnectError
    data object InvalidCredentials : ConnectError
    data object OpenClashUnavailable : ConnectError
    data class Unknown(val message: String) : ConnectError
}

private sealed interface LoginResult {
    data object Success : LoginResult
    data object InvalidCredentials : LoginResult
    data object Unreachable : LoginResult
    data class SslUntrusted(val host: String) : LoginResult
    data class Unknown(val message: String) : LoginResult
}

private sealed interface ValidationResult {
    data object Valid : ValidationResult
    data object InvalidSession : ValidationResult
    data object OpenClashUnavailable : ValidationResult
    data object Unreachable : ValidationResult
    data class SslUntrusted(val host: String) : ValidationResult
    data class Unknown(val message: String) : ValidationResult
}

private sealed interface StatusResult {
    data class Success(val status: OpenClashStatus) : StatusResult
    data object InvalidSession : StatusResult
    data object OpenClashUnavailable : StatusResult
    data object Unreachable : StatusResult
    data class SslUntrusted(val host: String) : StatusResult
    data class Unknown(val message: String) : StatusResult
}

private sealed interface NetworkResult {
    data class Success(
        val code: Int,
        val body: String,
        val url: HttpUrl,
    ) : NetworkResult

    data class SslUntrusted(val host: String) : NetworkResult
    data class Failure(val message: String) : NetworkResult
}
