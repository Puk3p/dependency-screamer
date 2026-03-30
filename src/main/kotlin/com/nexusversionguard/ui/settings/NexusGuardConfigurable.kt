package com.nexusversionguard.ui.settings

import com.intellij.openapi.options.Configurable
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import javax.swing.JComponent

class NexusGuardConfigurable : Configurable {
    private var settingsPanel: NexusGuardSettingsPanel? = null
    private var cachedPassword: String = ""
    private var cachedChangelogToken: String = ""

    override fun getDisplayName(): String = "Dependency Screamer"

    override fun createComponent(): JComponent {
        val panel = NexusGuardSettingsPanel()
        settingsPanel = panel
        reset()
        return panel.rootPanel
    }

    override fun isModified(): Boolean {
        val settings = NexusGuardSettings.getInstance()
        val panel = settingsPanel ?: return false

        return panel.baseUrl != settings.baseUrl ||
            panel.repositories != settings.repositories ||
            panel.username != settings.username ||
            panel.password != cachedPassword ||
            panel.ignoreSnapshots != settings.ignoreSnapshots ||
            panel.timeoutSeconds != settings.timeoutSeconds ||
            panel.groupFilter != settings.groupFilter ||
            panel.autoScanOnOpen != settings.autoScanOnOpen ||
            panel.backgroundScanEnabled != settings.backgroundScanEnabled ||
            panel.backgroundScanIntervalMinutes != settings.backgroundScanIntervalMinutes ||
            panel.changelogUrlPattern != settings.changelogUrlPattern ||
            panel.changelogAuthToken != cachedChangelogToken
    }

    override fun apply() {
        val settings = NexusGuardSettings.getInstance()
        val panel = settingsPanel ?: return

        settings.baseUrl = panel.baseUrl
        settings.repositories = panel.repositories
        settings.username = panel.username
        settings.password = panel.password
        cachedPassword = panel.password
        settings.ignoreSnapshots = panel.ignoreSnapshots
        settings.timeoutSeconds = panel.timeoutSeconds
        settings.groupFilter = panel.groupFilter
        settings.autoScanOnOpen = panel.autoScanOnOpen
        settings.backgroundScanEnabled = panel.backgroundScanEnabled
        settings.backgroundScanIntervalMinutes = panel.backgroundScanIntervalMinutes
        settings.changelogUrlPattern = panel.changelogUrlPattern
        settings.changelogAuthToken = panel.changelogAuthToken
        cachedChangelogToken = panel.changelogAuthToken

        DependencyAnalysisServiceProvider.getInstance().invalidate()
    }

    override fun reset() {
        val settings = NexusGuardSettings.getInstance()
        val panel = settingsPanel ?: return

        panel.baseUrl = settings.baseUrl
        panel.repositories = settings.repositories
        panel.username = settings.username
        cachedPassword = settings.getPasswordValue()
        panel.password = cachedPassword
        panel.ignoreSnapshots = settings.ignoreSnapshots
        panel.timeoutSeconds = settings.timeoutSeconds
        panel.groupFilter = settings.groupFilter
        panel.autoScanOnOpen = settings.autoScanOnOpen
        panel.backgroundScanEnabled = settings.backgroundScanEnabled
        panel.backgroundScanIntervalMinutes = settings.backgroundScanIntervalMinutes
        panel.changelogUrlPattern = settings.changelogUrlPattern
        cachedChangelogToken = settings.getChangelogTokenValue()
        panel.changelogAuthToken = cachedChangelogToken
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
