package com.nexusversionguard.ui.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.domain.model.DependencyAnalysisResult
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

class DependencyScreamerToolWindowPanel(private val project: Project) {
    val rootPanel: JPanel = JPanel(BorderLayout())
    private val resultsPanel: JPanel = JPanel()
    private val statusLabel: JBLabel = JBLabel("Ready")
    private val scanButton: JButton = JButton("Scan pom.xml")
    private val settingsButton: JButton = JButton("Settings")

    init {
        resultsPanel.layout = BoxLayout(resultsPanel, BoxLayout.Y_AXIS)

        val topBar = JPanel(BorderLayout())
        topBar.border = JBUI.Borders.empty(4)

        val buttonsPanel = JPanel()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.X_AXIS)
        buttonsPanel.add(scanButton)
        buttonsPanel.add(javax.swing.Box.createHorizontalStrut(8))
        buttonsPanel.add(settingsButton)

        topBar.add(buttonsPanel, BorderLayout.WEST)
        topBar.add(statusLabel, BorderLayout.EAST)

        val scrollPane = JBScrollPane(resultsPanel)

        rootPanel.add(topBar, BorderLayout.NORTH)
        rootPanel.add(scrollPane, BorderLayout.CENTER)

        scanButton.addActionListener { runScan() }
        settingsButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Dependency Screamer")
        }

        showWelcome()
    }

    private fun showWelcome() {
        resultsPanel.removeAll()
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) {
            addMessage("Nexus not configured. Click Settings to set up.")
        } else {
            addMessage("Click Scan to check dependencies.")
        }
        resultsPanel.revalidate()
        resultsPanel.repaint()
    }

    private fun runScan() {
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) {
            statusLabel.text = "Not configured"
            showWelcome()
            return
        }

        val basePath = project.basePath
        if (basePath == null) {
            statusLabel.text = "No project"
            return
        }

        val pomFile = VfsUtil.findFile(Path.of(basePath, "pom.xml"), true)
        if (pomFile == null) {
            statusLabel.text = "No pom.xml found"
            resultsPanel.removeAll()
            addMessage("No pom.xml found in project root.")
            resultsPanel.revalidate()
            resultsPanel.repaint()
            return
        }

        scanButton.isEnabled = false
        statusLabel.text = "Scanning..."
        resultsPanel.removeAll()
        addMessage("Scanning dependencies...")
        resultsPanel.revalidate()
        resultsPanel.repaint()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val content = String(pomFile.contentsToByteArray())
                val service = DependencyAnalysisServiceProvider.getInstance().getService()
                val results = service.analyzeDependencies(content).join()

                SwingUtilities.invokeLater {
                    displayResults(results)
                    scanButton.isEnabled = true
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    resultsPanel.removeAll()
                    addMessage("Scan failed: ${e.message}")
                    statusLabel.text = "Error"
                    scanButton.isEnabled = true
                    resultsPanel.revalidate()
                    resultsPanel.repaint()
                }
            }
        }
    }

    private fun displayResults(results: List<DependencyAnalysisResult>) {
        resultsPanel.removeAll()

        if (results.isEmpty()) {
            addMessage("No dependencies found.")
            statusLabel.text = "No dependencies"
            resultsPanel.revalidate()
            resultsPanel.repaint()
            return
        }

        val outdated = results.filter { it.status == DependencyStatus.OUTDATED }
        val upToDate = results.filter { it.status == DependencyStatus.UP_TO_DATE }
        val errors = results.filter { it.status == DependencyStatus.ERROR || it.status == DependencyStatus.NOT_FOUND }
        val unresolved = results.filter { it.status == DependencyStatus.UNRESOLVED }

        val summary = "${results.size} deps: ${outdated.size} outdated, ${upToDate.size} ok, ${errors.size} errors"
        statusLabel.text = summary

        if (outdated.isNotEmpty()) {
            addSectionHeader("OUTDATED")
            outdated.forEach { addResultRow(it) }
        }

        if (errors.isNotEmpty()) {
            addSectionHeader("ERRORS")
            errors.forEach { addResultRow(it) }
        }

        if (unresolved.isNotEmpty()) {
            addSectionHeader("UNRESOLVED")
            unresolved.forEach { addResultRow(it) }
        }

        if (upToDate.isNotEmpty()) {
            addSectionHeader("UP TO DATE")
            upToDate.forEach { addResultRow(it) }
        }

        resultsPanel.revalidate()
        resultsPanel.repaint()
    }

    private fun addSectionHeader(title: String) {
        val label = JBLabel(title)
        label.font = label.font.deriveFont(Font.BOLD)
        label.border = JBUI.Borders.empty(8, 4, 2, 4)
        label.alignmentX = Component.LEFT_ALIGNMENT
        resultsPanel.add(label)
    }

    private fun addResultRow(result: DependencyAnalysisResult) {
        val row = JPanel(BorderLayout())
        row.border =
            BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(4),
            )
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.maximumSize = java.awt.Dimension(Int.MAX_VALUE, 48)

        val dep = result.dependency
        val coordLabel = JBLabel("${dep.groupId}:${dep.artifactId}")
        coordLabel.font = coordLabel.font.deriveFont(Font.BOLD, 12f)

        val detailText =
            when (result.status) {
                DependencyStatus.OUTDATED -> "${dep.version} -> ${result.latestVersion?.version}"
                DependencyStatus.UP_TO_DATE -> "${dep.version} (latest)"
                DependencyStatus.ERROR -> "${dep.version} (${result.errorMessage})"
                DependencyStatus.NOT_FOUND -> "${dep.version} (not found in Nexus)"
                DependencyStatus.UNRESOLVED -> "${dep.rawVersion} (unresolved)"
            }
        val detailLabel = JBLabel(detailText)

        val statusColor =
            when (result.status) {
                DependencyStatus.OUTDATED -> JBColor.ORANGE
                DependencyStatus.UP_TO_DATE -> JBColor.GREEN
                DependencyStatus.ERROR, DependencyStatus.NOT_FOUND -> JBColor.RED
                DependencyStatus.UNRESOLVED -> JBColor.GRAY
            }
        detailLabel.foreground = statusColor

        val textPanel = JPanel()
        textPanel.layout = BoxLayout(textPanel, BoxLayout.Y_AXIS)
        textPanel.isOpaque = false
        textPanel.add(coordLabel)
        textPanel.add(detailLabel)

        row.add(textPanel, BorderLayout.CENTER)
        resultsPanel.add(row)
    }

    private fun addMessage(text: String) {
        val label = JBLabel(text)
        label.border = JBUI.Borders.empty(8)
        label.alignmentX = Component.LEFT_ALIGNMENT
        resultsPanel.add(label)
    }

}
