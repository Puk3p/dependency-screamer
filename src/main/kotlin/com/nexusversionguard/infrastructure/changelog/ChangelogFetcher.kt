package com.nexusversionguard.infrastructure.changelog

import com.intellij.openapi.diagnostic.Logger
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import java.net.HttpURLConnection
import java.net.URI

class ChangelogFetcher {
    private val logger = Logger.getInstance(ChangelogFetcher::class.java)

    fun fetch(
        groupId: String,
        artifactId: String,
    ): String? {
        val settings = NexusGuardSettings.getInstance()
        val pattern = settings.changelogUrlPattern
        if (pattern.isBlank()) return null

        val url =
            pattern
                .replace("{groupId}", groupId)
                .replace("{artifactId}", artifactId)
                .replace("{groupPath}", groupId.replace('.', '/'))

        return try {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"

            val token = settings.getChangelogTokenValue()
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "token $token")
            } else if (settings.username.isNotBlank()) {
                val auth =
                    java.util.Base64.getEncoder()
                        .encodeToString("${settings.username}:${settings.getPasswordValue()}".toByteArray())
                connection.setRequestProperty("Authorization", "Basic $auth")
            }

            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().readText()
            } else {
                logger.warn("Dependency Screamer: failed to fetch changelog from $url (HTTP ${connection.responseCode})")
                null
            }
        } catch (e: Exception) {
            logger.warn("Dependency Screamer: error fetching changelog from $url", e)
            null
        }
    }
}
