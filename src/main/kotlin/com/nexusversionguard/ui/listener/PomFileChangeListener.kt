package com.nexusversionguard.ui.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.nexusversionguard.application.service.BackgroundScanService
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import java.util.Timer
import java.util.TimerTask

class PomFileChangeListener : BulkFileListener {
    private val logger = Logger.getInstance(PomFileChangeListener::class.java)

    @Volatile
    private var debounceTimer: Timer? = null

    override fun after(events: List<VFileEvent>) {
        val pomChanged = events.any { event ->
            val path = event.path
            path.endsWith("/pom.xml") || path.endsWith("\\pom.xml")
        }

        if (!pomChanged) return

        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) return

        debounceTimer?.cancel()
        debounceTimer = Timer("DependencyScreamer-PomDebounce", true)
        debounceTimer?.schedule(
            object : TimerTask() {
                override fun run() {
                    for (project in ProjectManager.getInstance().openProjects) {
                        if (project.isDisposed) continue
                        val basePath = project.basePath ?: continue

                        val projectPomChanged = events.any { event ->
                            event.path.replace("\\", "/").startsWith(basePath.replace("\\", "/"))
                        }

                        if (projectPomChanged) {
                            logger.info("Dependency Screamer: pom.xml changed, triggering rescan for ${project.name}")
                            BackgroundScanService.getInstance(project)
                                .runScanAndUpdateBadge(showNotification = false)
                        }
                    }
                }
            },
            2000,
        )
    }
}
