package com.nexusversionguard.ui.settings

import com.intellij.openapi.options.Configurable
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import javax.swing.JComponent

class NexusGuardConfigurable : Configurable {
    private var settingsPanel: NexusGuardSettingsPanel? = null

    override fun getDisplayName(): String = "Dependency Screamer"

    override fun createComponent(): JComponent {
        val panel = NexusGuardSettingsPanel()
        settingsPanel = panel
        return panel.rootPanel
    }

    override fun isModified(): Boolean {
        val settings = NexusGuardSettings.getInstance()
        val panel = settingsPanel ?: return false

        return panel.baseUrl != settings.baseUrl ||
            panel.repositories != settings.repositories ||
            panel.username != settings.username ||
            panel.password != settings.password ||
            panel.ignoreSnapshots != settings.ignoreSnapshots ||
            panel.timeoutSeconds != settings.timeoutSeconds
    }

    override fun apply() {
        val settings = NexusGuardSettings.getInstance()
        val panel = settingsPanel ?: return

        settings.baseUrl = panel.baseUrl
        settings.repositories = panel.repositories
        settings.username = panel.username
        settings.password = panel.password
        settings.ignoreSnapshots = panel.ignoreSnapshots
        settings.timeoutSeconds = panel.timeoutSeconds

        DependencyAnalysisServiceProvider.getInstance().invalidate()
    }

    override fun reset() {
        val settings = NexusGuardSettings.getInstance()
        val panel = settingsPanel ?: return

        panel.baseUrl = settings.baseUrl
        panel.repositories = settings.repositories
        panel.username = settings.username
        panel.password = settings.password
        panel.ignoreSnapshots = settings.ignoreSnapshots
        panel.timeoutSeconds = settings.timeoutSeconds
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
