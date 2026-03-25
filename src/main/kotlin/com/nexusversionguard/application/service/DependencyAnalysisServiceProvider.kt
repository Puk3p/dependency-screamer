package com.nexusversionguard.application.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.nexusversionguard.infrastructure.client.NexusRepositoryClient
import com.nexusversionguard.infrastructure.parser.MavenPropertyResolver
import com.nexusversionguard.infrastructure.parser.PomXmlDependencySource
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import com.nexusversionguard.infrastructure.version.SemanticVersionComparator

@Service(Service.Level.APP)
class DependencyAnalysisServiceProvider {
    @Volatile
    private var cachedService: DependencyAnalysisService? = null

    @Volatile
    private var lastConfigHash: Int = 0

    fun getService(): DependencyAnalysisService {
        val settings = NexusGuardSettings.getInstance()
        val configHash = settings.getConfig().hashCode()

        val current = cachedService
        if (current != null && configHash == lastConfigHash) {
            return current
        }

        synchronized(this) {
            val doubleCheck = cachedService
            if (doubleCheck != null && configHash == lastConfigHash) {
                return doubleCheck
            }

            val propertyResolver = MavenPropertyResolver()
            val dependencySource = PomXmlDependencySource(propertyResolver)
            val versionComparator = SemanticVersionComparator()
            val repositoryClient = NexusRepositoryClient(settings.getConfig(), versionComparator)

            val service =
                DependencyAnalysisService(
                    dependencySource = dependencySource,
                    repositoryClient = repositoryClient,
                    versionComparator = versionComparator,
                    configurationProvider = settings,
                )

            cachedService = service
            lastConfigHash = configHash
            return service
        }
    }

    fun invalidate() {
        synchronized(this) {
            cachedService?.clearCache()
            cachedService = null
            lastConfigHash = 0
        }
    }

    companion object {
        fun getInstance(): DependencyAnalysisServiceProvider {
            return ApplicationManager.getApplication().getService(DependencyAnalysisServiceProvider::class.java)
        }
    }
}
