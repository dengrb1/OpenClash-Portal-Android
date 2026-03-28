# OpenClash Portal

Android app for logging into an OpenWrt router and opening OpenClash directly inside the app. The app also resolves and opens Zashboard and MetaCubeXD from OpenClash status data.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- OkHttp
- WebView
- DataStore
- AndroidX Security Crypto

## Features

- Router discovery from current gateway plus common OpenWrt LAN addresses
- LuCI login with `root + password`
- Session cookie persistence across app restarts
- Direct OpenClash entry instead of landing on the OpenWrt admin home page
- Automatic Zashboard and MetaCubeXD URL resolution from `/cgi-bin/luci/admin/services/openclash/status`
- Manual URL override fields for OpenClash, Zashboard, and MetaCubeXD
- Optional HTTPS mode with per-host trust for self-signed certificates

## Build prerequisites

- JDK 17
- Android SDK with API 36 / Build Tools 35.0.0 or newer
- Gradle wrapper is included

## Notes for this machine

The project configured successfully with:

- Gradle `8.13`
- Android Gradle Plugin `8.13.2`
- Java runtime `17`

On this Windows environment, Gradle dependency resolution required using the Windows certificate store:

```powershell
$env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
```

If Android SDK is not installed, Gradle will stop with an `SDK location not found` error until `ANDROID_HOME` or `local.properties` is configured.
