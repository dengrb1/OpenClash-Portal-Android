package com.openclash.portal.data

import com.openclash.portal.model.OpenClashStatus
import com.openclash.portal.model.RouterProfile
import com.openclash.portal.model.RouterProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenClashUrlResolverTest {
    private val resolver = OpenClashUrlResolver()

    @Test
    fun `uses router host and cn port when no forward config`() {
        val profile = RouterProfile(
            protocol = RouterProtocol.HTTP,
            host = "192.168.1.1",
            port = 80,
        )
        val status = OpenClashStatus(
            clash = true,
            daip = "192.168.1.1",
            dase = "secret123",
            cnPort = 9090,
            dbForwardDomain = null,
            dbForwardPort = null,
            dbForwardSsl = false,
            zashboardAvailable = true,
            metaCubeXdAvailable = true,
        )

        val resolved = resolver.resolve(profile, status)

        assertEquals(
            "http://192.168.1.1:9090/ui/dashboard/#/setup?host=192.168.1.1&port=9090&secret=secret123",
            resolved.zashboardUrl,
        )
        assertEquals(
            "http://192.168.1.1:9090/ui/metacubexd/#/setup?hostname=192.168.1.1&port=9090&secret=secret123",
            resolved.metaCubeXdUrl,
        )
    }

    @Test
    fun `uses forwarded https endpoint when configured`() {
        val profile = RouterProfile(
            protocol = RouterProtocol.HTTPS,
            host = "router.lan",
            port = 443,
        )
        val status = OpenClashStatus(
            clash = true,
            daip = "192.168.1.1",
            dase = "forward-secret",
            cnPort = 9090,
            dbForwardDomain = "panel.example.com",
            dbForwardPort = 9443,
            dbForwardSsl = true,
            zashboardAvailable = true,
            metaCubeXdAvailable = false,
        )

        val resolved = resolver.resolve(profile, status)

        assertEquals(
            "https://panel.example.com:9443/ui/dashboard/#/setup?host=panel.example.com&port=9443&secret=forward-secret",
            resolved.zashboardUrl,
        )
        assertNull(resolved.metaCubeXdUrl)
    }

    @Test
    fun `falls back to hash route when secret is empty`() {
        val profile = RouterProfile(
            protocol = RouterProtocol.HTTP,
            host = "192.168.0.1",
            port = 80,
        )
        val status = OpenClashStatus(
            clash = true,
            daip = "192.168.0.1",
            dase = null,
            cnPort = 9090,
            dbForwardDomain = null,
            dbForwardPort = null,
            dbForwardSsl = false,
            zashboardAvailable = true,
            metaCubeXdAvailable = false,
        )

        val resolved = resolver.resolve(profile, status)

        assertEquals("http://192.168.0.1:9090/ui/dashboard/", resolved.zashboardUrl)
    }

    @Test
    fun `uses daip for direct controller access when available`() {
        val profile = RouterProfile(
            protocol = RouterProtocol.HTTP,
            host = "router.lan",
            port = 80,
        )
        val status = OpenClashStatus(
            clash = true,
            daip = "192.168.50.1",
            dase = "secret123",
            cnPort = 9090,
            dbForwardDomain = null,
            dbForwardPort = null,
            dbForwardSsl = false,
            zashboardAvailable = true,
            metaCubeXdAvailable = true,
        )

        val resolved = resolver.resolve(profile, status)

        assertEquals(
            "http://router.lan:9090/ui/dashboard/#/setup?host=192.168.50.1&port=9090&secret=secret123",
            resolved.zashboardUrl,
        )
        assertEquals(
            "http://router.lan:9090/ui/metacubexd/#/setup?hostname=192.168.50.1&port=9090&secret=secret123",
            resolved.metaCubeXdUrl,
        )
    }
}

