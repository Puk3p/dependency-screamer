package com.nexusversionguard.ui.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.nexusversionguard.application.service.BackgroundScanService
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings

class NexusGuardStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(NexusGuardStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) {
            logger.info("Dependency Screamer: Nexus not configured, skipping startup scan")
            return
        }

        val bgService = BackgroundScanService.getInstance(project)

        if (settings.autoScanOnOpen) {
            bgService.runScanAndUpdateBadge(showNotification = true)
        }

        if (settings.backgroundScanEnabled) {
            bgService.startPeriodicScan()
        }
    }
}
