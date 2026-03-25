package com.nexusversionguard.ui.startup

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import java.nio.file.Path

class NexusGuardStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(NexusGuardStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) {
            logger.info("Dependency Screamer: Nexus not configured, skipping startup scan")
            return
        }

        val basePath = project.basePath ?: return
        val pomFile = VfsUtil.findFile(Path.of(basePath, "pom.xml"), true) ?: return
        val content = String(pomFile.contentsToByteArray())

        val service = DependencyAnalysisServiceProvider.getInstance().getService()

        try {
            val results = service.analyzeDependencies(content).join()
            val outdatedCount = results.count { it.status == DependencyStatus.OUTDATED }

            if (outdatedCount > 0) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Dependency Screamer")
                    .createNotification(
                        "Dependency Screamer",
                        "$outdatedCount outdated dependency(ies) found in pom.xml",
                        NotificationType.WARNING,
                    )
                    .notify(project)
            }

            logger.info("Dependency Screamer: scanned ${results.size} dependencies, $outdatedCount outdated")
        } catch (e: Exception) {
            logger.warn("Dependency Screamer: startup scan failed", e)
        }
    }
}
