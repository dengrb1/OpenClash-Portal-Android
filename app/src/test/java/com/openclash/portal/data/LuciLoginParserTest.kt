package com.openclash.portal.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LuciLoginParserTest {
    private val parser = LuciLoginParser()

    @Test
    fun `extracts hidden fields and input names from login form`() {
        val html = """
            <html>
              <body>
                <form method="post" action="/cgi-bin/luci/">
                  <input type="hidden" name="token" value="abc123" />
                  <input type="text" name="username" />
                  <input type="password" name="password" />
                </form>
              </body>
            </html>
        """.trimIndent()

        val form = parser.parse("http://192.168.1.1/cgi-bin/luci/".toHttpUrl(), html)

        requireNotNull(form)
        assertEquals("http://192.168.1.1/cgi-bin/luci/", form.actionUrl.toString())
        assertEquals("username", form.usernameField)
        assertEquals("password", form.passwordField)
        assertEquals("abc123", form.hiddenFields["token"])
    }

    @Test
    fun `detects login page by password input markers`() {
        val html = """
            <form>
                <input type="password" name="luci_password" />
            </form>
        """.trimIndent()

        assertTrue(parser.looksLikeLoginPage(html))
    }
}
