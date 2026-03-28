package com.openclash.portal.model

enum class RouterProtocol(val scheme: String, val defaultPort: Int) {
    HTTP("http", 80),
    HTTPS("https", 443),
}

