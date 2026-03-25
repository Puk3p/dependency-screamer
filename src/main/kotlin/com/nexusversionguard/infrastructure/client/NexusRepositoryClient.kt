package com.nexusversionguard.infrastructure.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexusversionguard.domain.model.MavenCoordinates
import com.nexusversionguard.domain.model.NexusConfig
import com.nexusversionguard.domain.model.VersionInfo
import com.nexusversionguard.domain.port.ArtifactRepositoryClient
import com.nexusversionguard.domain.port.VersionComparator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CompletableFuture

class NexusRepositoryClient(
    private val config: NexusConfig,
    private val versionComparator: VersionComparator,
) : ArtifactRepositoryClient {
    private val httpClient: HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
            .build()

    private val gson = Gson()

    override fun fetchAvailableVersions(coordinates: MavenCoordinates): CompletableFuture<List<VersionInfo>> {
        val futures =
            config.repositories.map { repository ->
                fetchVersionsFromRepository(coordinates, repository)
            }

        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            futures.flatMap { it.join() }.distinctBy { it.version }
        }
    }

    override fun fetchLatestVersion(coordinates: MavenCoordinates): CompletableFuture<VersionInfo?> {
        return fetchAvailableVersions(coordinates).thenApply { versions ->
            versionComparator.findLatest(versions, config.ignoreSnapshots)
        }
    }

    override fun isAvailable(): CompletableFuture<Boolean> {
        val url = "${config.baseUrl.trimEnd('/')}/service/rest/v1/status"

        return buildRequest(url).thenCompose { request ->
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        }.thenApply { response ->
            response.statusCode() == 200
        }.exceptionally {
            false
        }
    }

    private fun fetchVersionsFromRepository(
        coordinates: MavenCoordinates,
        repository: String,
    ): CompletableFuture<List<VersionInfo>> {
        val url = buildSearchUrl(coordinates, repository)

        return buildRequest(url).thenCompose { request ->
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        }.thenApply { response ->
            if (response.statusCode() == 200) {
                parseVersionsResponse(response.body())
            } else {
                emptyList()
            }
        }.exceptionally {
            emptyList()
        }
    }

    private fun buildSearchUrl(
        coordinates: MavenCoordinates,
        repository: String,
    ): String {
        val base = config.baseUrl.trimEnd('/')
        return "$base/service/rest/v1/search?" +
            "repository=$repository" +
            "&group=${coordinates.groupId}" +
            "&name=${coordinates.artifactId}"
    }

    private fun buildRequest(url: String): CompletableFuture<HttpRequest> {
        return CompletableFuture.supplyAsync {
            val builder =
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
                    .GET()

            if (config.requiresAuth) {
                val credentials = "${config.username}:${config.password}"
                val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
                builder.header("Authorization", "Basic $encoded")
            }

            builder.build()
        }
    }

    private fun parseVersionsResponse(body: String): List<VersionInfo> {
        val jsonObject = gson.fromJson(body, JsonObject::class.java)
        val items = jsonObject.getAsJsonArray("items") ?: return emptyList()

        return items.mapNotNull { item ->
            val obj = item.asJsonObject
            val version = obj.get("version")?.asString
            if (version != null) {
                VersionInfo(
                    version = version,
                    isSnapshot = version.uppercase().endsWith("-SNAPSHOT"),
                )
            } else {
                null
            }
        }
    }
}
