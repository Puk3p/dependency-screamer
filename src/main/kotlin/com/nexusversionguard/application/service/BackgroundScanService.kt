package com.nexusversionguard.application.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import java.nio.file.Path
import java.util.Timer
import java.util.TimerTask
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class BackgroundScanService(
    private val project: Project,
) : Disposable {
    private val logger = Logger.getInstance(BackgroundScanService::class.java)
    private var timer: Timer? = null

    @Volatile
    private var lastOutdatedCount: Int = 0

    fun runScanAndUpdateBadge(showNotification: Boolean = true) {
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) return

        val basePath = project.basePath ?: return
        val pomFile = VfsUtil.findFile(Path.of(basePath, "pom.xml"), true) ?: return
        val content = String(pomFile.contentsToByteArray())

        val service = DependencyAnalysisServiceProvider.getInstance().getService()

        try {
            val results = service.analyzeDependencies(content).join()
            val outdatedCount = results.count { it.status == DependencyStatus.OUTDATED }

            updateBadge(outdatedCount)

            if (showNotification && outdatedCount > 0 && outdatedCount != lastOutdatedCount) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Dependency Screamer")
                    .createNotification(
                        "Dependency Screamer",
                        "$outdatedCount outdated dependency(ies) found in pom.xml",
                        NotificationType.WARNING,
                    )
                    .notify(project)
            }

            lastOutdatedCount = outdatedCount
            logger.info("Dependency Screamer: background scan — ${results.size} deps, $outdatedCount outdated")
        } catch (e: Exception) {
            logger.warn("Dependency Screamer: background scan failed", e)
        }
    }

    fun updateBadge(outdatedCount: Int) {
        SwingUtilities.invokeLater {
            val toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow("Dependency Screamer") ?: return@invokeLater
            toolWindow.stripeTitle =
                if (outdatedCount > 0) "Dep. Screamer ($outdatedCount)" else "Dependency Screamer"
        }
    }

    fun startPeriodicScan() {
        stopPeriodicScan()

        val settings = NexusGuardSettings.getInstance()
        if (!settings.backgroundScanEnabled || !settings.isConfigured()) return

        val intervalMs = settings.backgroundScanIntervalMinutes.toLong() * 60 * 1000

        timer =
            Timer("DependencyScreamer-BackgroundScan", true).also { t ->
                t.scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            runScanAndUpdateBadge(showNotification = true)
                        }
                    },
                    intervalMs,
                    intervalMs,
                )
            }

        logger.info("Dependency Screamer: periodic scan started (every ${settings.backgroundScanIntervalMinutes} min)")
    }

    fun stopPeriodicScan() {
        timer?.cancel()
        timer = null
    }

    fun restartIfNeeded() {
        stopPeriodicScan()
        startPeriodicScan()
    }

    override fun dispose() {
        stopPeriodicScan()
    }

    companion object {
        fun getInstance(project: Project): BackgroundScanService {
            return project.getService(BackgroundScanService::class.java)
        }
    }
}
