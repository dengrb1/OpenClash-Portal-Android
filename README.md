# OpenClash Portal

## 项目简介

OpenClash Portal 是一个 Android 客户端，用于登录 OpenWrt 路由器并在应用内直接打开 OpenClash 页面。应用还会从 OpenClash 状态接口自动解析并跳转到 Zashboard 与 MetaCubeXD，减少在浏览器中手动查找入口的步骤。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- OkHttp
- WebView
- DataStore
- AndroidX Security Crypto

## 功能特性

- 基于当前网关和常见 OpenWrt 局域网地址的路由器发现
- 支持 `root + 密码` 的 LuCI 登录
- 会话 Cookie 持久化（重启应用后可恢复）
- 直接进入 OpenClash 页面，避免先落到 OpenWrt 管理首页
- 自动从 `/cgi-bin/luci/admin/services/openclash/status` 解析 Zashboard 与 MetaCubeXD 地址
- 支持手动覆盖 OpenClash、Zashboard、MetaCubeXD 的访问 URL
- 可选 HTTPS 模式，并支持按主机信任自签名证书

## 构建前置要求

- JDK 17
- Android SDK（API 36，Build Tools 35.0.0 或更高版本）
- 项目已包含 Gradle Wrapper

## 本机环境说明

当前项目在以下版本组合下可完成配置：

- Gradle `8.13`
- Android Gradle Plugin `8.13.2`
- Java Runtime `17`

在某些 Windows 环境中，Gradle 解析依赖可能需要使用系统证书存储：

```powershell
$env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
```

如果未安装 Android SDK，Gradle 会报 `SDK location not found`，需要先配置 `ANDROID_HOME` 或 `local.properties`。
