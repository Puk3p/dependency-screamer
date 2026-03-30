package com.nexusversionguard.ui.toolwindow

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.domain.model.DependencyAnalysisResult
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer

class DependencyScreamerToolWindowPanel(private val project: Project) {
    val rootPanel: JPanel = JPanel(CardLayout())

    private val setupCard = JPanel(BorderLayout())
    private val mainCard = JPanel(BorderLayout())
    private val resultsPanel = JPanel()
    private val summaryPanel = JPanel()

    private var spinnerTimer: Timer? = null
    private var revealTimer: Timer? = null

    private val accentGreen = JBColor(Color(46, 160, 67), Color(63, 185, 80))
    private val accentOrange = JBColor(Color(210, 153, 34), Color(227, 179, 65))
    private val accentRed = JBColor(Color(218, 54, 51), Color(248, 81, 73))
    private val subtleText = JBColor(Color(130, 130, 130), Color(140, 140, 140))
    private val cardBg = JBColor(Color(245, 245, 245), Color(43, 45, 48))
    private val cardBorder = JBColor(Color(225, 225, 225), Color(60, 63, 65))

    init {
        buildSetupCard()
        buildMainCard()

        rootPanel.add(setupCard, "setup")
        rootPanel.add(mainCard, "main")

        showCorrectCard()
    }

    private fun showCorrectCard() {
        val layout = rootPanel.layout as CardLayout
        val settings = NexusGuardSettings.getInstance()
        if (settings.isConfigured()) {
            layout.show(rootPanel, "main")
        } else {
            layout.show(rootPanel, "setup")
        }
    }

    private fun buildSetupCard() {
        setupCard.border = JBUI.Borders.empty(24)

        val center = JPanel()
        center.layout = BoxLayout(center, BoxLayout.Y_AXIS)
        center.isOpaque = false

        center.add(Box.createVerticalGlue())

        val title = JBLabel("Dependency Screamer")
        title.font = title.font.deriveFont(Font.BOLD, 18f)
        title.alignmentX = Component.CENTER_ALIGNMENT
        center.add(title)

        center.add(Box.createVerticalStrut(8))

        val subtitle = JBLabel("Check your Nexus dependencies for updates")
        subtitle.foreground = subtleText
        subtitle.font = subtitle.font.deriveFont(13f)
        subtitle.alignmentX = Component.CENTER_ALIGNMENT
        center.add(subtitle)

        center.add(Box.createVerticalStrut(24))

        val stepsPanel = JPanel()
        stepsPanel.layout = BoxLayout(stepsPanel, BoxLayout.Y_AXIS)
        stepsPanel.isOpaque = false
        stepsPanel.alignmentX = Component.CENTER_ALIGNMENT
        stepsPanel.maximumSize = Dimension(320, 120)

        addSetupStep(stepsPanel, "1", "Set your Nexus URL and repository")
        addSetupStep(stepsPanel, "2", "Add credentials if needed")
        addSetupStep(stepsPanel, "3", "Set group filter (e.g. com.endava)")

        center.add(stepsPanel)
        center.add(Box.createVerticalStrut(24))

        val configButton = JButton("Open Settings")
        configButton.alignmentX = Component.CENTER_ALIGNMENT
        configButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        configButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Dependency Screamer")
            SwingUtilities.invokeLater { showCorrectCard() }
        }
        center.add(configButton)

        center.add(Box.createVerticalGlue())

        setupCard.add(center, BorderLayout.CENTER)
    }

    private fun addSetupStep(
        parent: JPanel,
        number: String,
        text: String,
    ) {
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
        row.isOpaque = false
        row.maximumSize = Dimension(Int.MAX_VALUE, 32)

        val badge =
            object : JLabel(number, SwingConstants.CENTER) {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = accentGreen
                    g2.fillOval(0, 0, width - 1, height - 1)
                    g2.color = Color.WHITE
                    g2.font = font.deriveFont(Font.BOLD, 11f)
                    val fm = g2.fontMetrics
                    val x = (width - fm.stringWidth(text)) / 2
                    val y = (height + fm.ascent - fm.descent) / 2
                    g2.drawString(number, x, y)
                }
            }
        badge.preferredSize = Dimension(22, 22)
        badge.minimumSize = Dimension(22, 22)
        badge.maximumSize = Dimension(22, 22)

        val label = JBLabel(text)
        label.font = label.font.deriveFont(12.5f)

        row.add(badge)
        row.add(label)
        parent.add(row)
    }

    private fun buildMainCard() {
        val toolbar = JPanel(BorderLayout())
        toolbar.border = JBUI.Borders.empty(6, 10)

        val leftActions = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        leftActions.isOpaque = false

        val scanButton = JButton("Scan")
        scanButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        scanButton.addActionListener { runScan(scanButton) }
        leftActions.add(scanButton)

        val settingsLink = JBLabel("Settings")
        settingsLink.foreground = subtleText
        settingsLink.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        settingsLink.font = settingsLink.font.deriveFont(11.5f)
        settingsLink.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Dependency Screamer")
                    SwingUtilities.invokeLater { showCorrectCard() }
                }

                override fun mouseEntered(e: MouseEvent?) {
                    settingsLink.foreground = JBColor.foreground()
                }

                override fun mouseExited(e: MouseEvent?) {
                    settingsLink.foreground = subtleText
                }
            },
        )
        leftActions.add(settingsLink)

        toolbar.add(leftActions, BorderLayout.WEST)

        summaryPanel.layout = FlowLayout(FlowLayout.RIGHT, 8, 0)
        summaryPanel.isOpaque = false
        toolbar.add(summaryPanel, BorderLayout.EAST)

        resultsPanel.layout = BoxLayout(resultsPanel, BoxLayout.Y_AXIS)
        resultsPanel.border = JBUI.Borders.empty(4, 10, 10, 10)
        val scrollPane = JBScrollPane(resultsPanel)
        scrollPane.border = BorderFactory.createEmptyBorder()

        mainCard.add(toolbar, BorderLayout.NORTH)
        mainCard.add(scrollPane, BorderLayout.CENTER)

        showReadyState()
    }

    private fun showReadyState() {
        resultsPanel.removeAll()
        val msg = JBLabel("Click Scan to analyze dependencies")
        msg.foreground = subtleText
        msg.border = JBUI.Borders.empty(20, 0)
        msg.alignmentX = Component.LEFT_ALIGNMENT
        resultsPanel.add(msg)
        resultsPanel.revalidate()
        resultsPanel.repaint()
    }

    private fun runScan(scanButton: JButton) {
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) {
            showCorrectCard()
            return
        }

        val basePath = project.basePath ?: return
        val pomFile = VfsUtil.findFile(Path.of(basePath, "pom.xml"), true)
        if (pomFile == null) {
            resultsPanel.removeAll()
            showEmptyState("No pom.xml found in project root")
            return
        }

        scanButton.isEnabled = false
        scanButton.text = "Scanning..."
        resultsPanel.removeAll()
        summaryPanel.removeAll()

        val spinnerPanel = createSpinner()
        resultsPanel.add(spinnerPanel)
        resultsPanel.revalidate()
        resultsPanel.repaint()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val content = String(pomFile.contentsToByteArray())
                val service = DependencyAnalysisServiceProvider.getInstance().getService()
                val results = service.analyzeDependencies(content).join()

                SwingUtilities.invokeLater {
                    stopSpinner()
                    displayResultsAnimated(results)
                    scanButton.isEnabled = true
                    scanButton.text = "Scan"
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    stopSpinner()
                    resultsPanel.removeAll()
                    showEmptyState("Scan failed: ${e.message}")
                    scanButton.isEnabled = true
                    scanButton.text = "Scan"
                }
            }
        }
    }

    private fun displayResults(results: List<DependencyAnalysisResult>) {
        resultsPanel.removeAll()
        summaryPanel.removeAll()

        if (results.isEmpty()) {
            val filter = NexusGuardSettings.getInstance().groupFilter
            if (filter.isNotBlank()) {
                showEmptyState("No dependencies matching filter: $filter")
            } else {
                showEmptyState("No dependencies found in pom.xml")
            }
            return
        }

        val outdated = results.filter { it.status == DependencyStatus.OUTDATED }
        val upToDate = results.filter { it.status == DependencyStatus.UP_TO_DATE }
        val errors =
            results.filter {
                it.status == DependencyStatus.ERROR || it.status == DependencyStatus.NOT_FOUND
            }
        val unresolved = results.filter { it.status == DependencyStatus.UNRESOLVED }

        addSummaryBadge("${outdated.size} outdated", accentOrange, outdated.isNotEmpty())
        addSummaryBadge("${upToDate.size} ok", accentGreen, upToDate.isNotEmpty())
        if (errors.isNotEmpty()) addSummaryBadge("${errors.size} errors", accentRed, true)
        summaryPanel.revalidate()

        if (outdated.isNotEmpty()) {
            addSection("Outdated", accentOrange, outdated)
        }
        if (errors.isNotEmpty()) {
            addSection("Errors", accentRed, errors)
        }
        if (unresolved.isNotEmpty()) {
            addSection("Unresolved", subtleText, unresolved)
        }
        if (upToDate.isNotEmpty()) {
            addSection("Up to date", accentGreen, upToDate)
        }

        resultsPanel.add(Box.createVerticalGlue())
        resultsPanel.revalidate()
        resultsPanel.repaint()
    }

    private fun displayResultsAnimated(results: List<DependencyAnalysisResult>) {
        resultsPanel.removeAll()
        summaryPanel.removeAll()

        if (results.isEmpty()) {
            val filter = NexusGuardSettings.getInstance().groupFilter
            if (filter.isNotBlank()) {
                showEmptyState("No dependencies matching filter: $filter")
            } else {
                showEmptyState("No dependencies found in pom.xml")
            }
            return
        }

        val outdated = results.filter { it.status == DependencyStatus.OUTDATED }
        val upToDate = results.filter { it.status == DependencyStatus.UP_TO_DATE }
        val errors =
            results.filter {
                it.status == DependencyStatus.ERROR || it.status == DependencyStatus.NOT_FOUND
            }
        val unresolved = results.filter { it.status == DependencyStatus.UNRESOLVED }

        addSummaryBadge("${outdated.size} outdated", accentOrange, outdated.isNotEmpty())
        addSummaryBadge("${upToDate.size} ok", accentGreen, upToDate.isNotEmpty())
        if (errors.isNotEmpty()) addSummaryBadge("${errors.size} errors", accentRed, true)
        summaryPanel.revalidate()

        val allComponents = mutableListOf<JPanel>()

        if (outdated.isNotEmpty()) {
            allComponents.addAll(buildSection("Outdated", accentOrange, outdated))
        }
        if (errors.isNotEmpty()) {
            allComponents.addAll(buildSection("Errors", accentRed, errors))
        }
        if (unresolved.isNotEmpty()) {
            allComponents.addAll(buildSection("Unresolved", subtleText, unresolved))
        }
        if (upToDate.isNotEmpty()) {
            allComponents.addAll(buildSection("Up to date", accentGreen, upToDate))
        }

        allComponents.forEach { it.isVisible = false }
        allComponents.forEach { resultsPanel.add(it) }
        resultsPanel.add(Box.createVerticalGlue())
        resultsPanel.revalidate()

        var index = 0
        revealTimer?.stop()
        revealTimer = Timer(50, null)
        revealTimer?.addActionListener {
            if (index < allComponents.size) {
                allComponents[index].isVisible = true
                index++
            } else {
                revealTimer?.stop()
            }
        }
        revealTimer?.start()
    }

    private fun buildSection(
        title: String,
        color: Color,
        items: List<DependencyAnalysisResult>,
    ): List<JPanel> {
        val panels = mutableListOf<JPanel>()

        val header = JPanel(BorderLayout())
        header.isOpaque = false
        header.alignmentX = Component.LEFT_ALIGNMENT
        header.border = JBUI.Borders.empty(12, 0, 4, 0)
        header.maximumSize = Dimension(Int.MAX_VALUE, 36)

        val leftHeader = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        leftHeader.isOpaque = false

        val dot =
            object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = color
                    g2.fillOval(2, 2, 8, 8)
                }
            }
        dot.preferredSize = Dimension(12, 12)
        dot.isOpaque = false

        val label = JBLabel("$title (${items.size})")
        label.font = label.font.deriveFont(Font.BOLD, 12f)
        label.foreground = color
        label.border = JBUI.Borders.emptyLeft(4)

        leftHeader.add(dot)
        leftHeader.add(label)
        header.add(leftHeader, BorderLayout.WEST)

        if (title == "Outdated" && items.isNotEmpty()) {
            val cardPanels = mutableListOf<JPanel>()
            items.forEach { cardPanels.add(buildCardPanel(it)) }

            val updateAllLink = createUpdateAllLink(items, cardPanels)
            header.add(updateAllLink, BorderLayout.EAST)

            panels.add(header)
            panels.addAll(cardPanels)
        } else {
            panels.add(header)
            items.forEach { panels.add(buildCardPanel(it)) }
        }

        return panels
    }

    private fun createSpinner(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = JBUI.Borders.empty(30, 0)
        panel.alignmentX = Component.LEFT_ALIGNMENT

        val spinner =
            object : JPanel() {
                private var angle = 0

                init {
                    preferredSize = Dimension(32, 32)
                    isOpaque = false
                }

                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    val cx = width / 2
                    val cy = height / 2
                    val r = 12
                    for (i in 0 until 8) {
                        val a = Math.toRadians((angle + i * 45).toDouble())
                        val x = cx + (r * Math.cos(a)).toInt()
                        val y = cy + (r * Math.sin(a)).toInt()
                        val alpha = 255 - i * 30
                        g2.color =
                            JBColor(
                                Color(accentGreen.red, accentGreen.green, accentGreen.blue, alpha.coerceIn(40, 255)),
                                Color(accentGreen.red, accentGreen.green, accentGreen.blue, alpha.coerceIn(40, 255)),
                            )
                        val dotSize = 4 - (i * 0.3).toInt()
                        g2.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize.coerceAtLeast(2), dotSize.coerceAtLeast(2))
                    }
                }

                fun rotate() {
                    angle = (angle + 30) % 360
                    repaint()
                }
            }

        val spinnerWrapper = JPanel(FlowLayout(FlowLayout.CENTER))
        spinnerWrapper.isOpaque = false
        spinnerWrapper.add(spinner)

        val textLabel = JBLabel("Analyzing dependencies...")
        textLabel.foreground = subtleText
        textLabel.horizontalAlignment = SwingConstants.CENTER
        textLabel.border = JBUI.Borders.emptyTop(8)

        panel.add(spinnerWrapper, BorderLayout.CENTER)
        panel.add(textLabel, BorderLayout.SOUTH)

        spinnerTimer?.stop()
        spinnerTimer = Timer(60) { spinner.rotate() }
        spinnerTimer?.start()

        return panel
    }

    private fun stopSpinner() {
        spinnerTimer?.stop()
        spinnerTimer = null
    }

    private fun addSummaryBadge(
        text: String,
        color: Color,
        active: Boolean,
    ) {
        val badge =
            object : JLabel(" $text ") {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    val bgColor = if (active) color else cardBorder
                    g2.color =
                        JBColor(
                            Color(bgColor.red, bgColor.green, bgColor.blue, 30),
                            Color(bgColor.red, bgColor.green, bgColor.blue, 50),
                        )
                    g2.fillRoundRect(0, 0, width, height, 10, 10)
                    super.paintComponent(g)
                }
            }
        badge.foreground = if (active) color else subtleText
        badge.font = badge.font.deriveFont(Font.BOLD, 11f)
        badge.border = JBUI.Borders.empty(2, 6)
        badge.isOpaque = false
        summaryPanel.add(badge)
    }

    private fun addSection(
        title: String,
        color: Color,
        items: List<DependencyAnalysisResult>,
    ) {
        val header = JPanel(BorderLayout())
        header.isOpaque = false
        header.alignmentX = Component.LEFT_ALIGNMENT
        header.border = JBUI.Borders.empty(12, 0, 4, 0)
        header.maximumSize = Dimension(Int.MAX_VALUE, 36)

        val leftHeader = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        leftHeader.isOpaque = false

        val dot =
            object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = color
                    g2.fillOval(2, 2, 8, 8)
                }
            }
        dot.preferredSize = Dimension(12, 12)
        dot.isOpaque = false

        val label = JBLabel("$title (${items.size})")
        label.font = label.font.deriveFont(Font.BOLD, 12f)
        label.foreground = color
        label.border = JBUI.Borders.emptyLeft(4)

        leftHeader.add(dot)
        leftHeader.add(label)
        header.add(leftHeader, BorderLayout.WEST)

        if (title == "Outdated" && items.isNotEmpty()) {
            val cardPanels = mutableListOf<JPanel>()
            items.forEach { cardPanels.add(buildCardPanel(it)) }

            val updateAllLink = createUpdateAllLink(items, cardPanels)
            header.add(updateAllLink, BorderLayout.EAST)

            resultsPanel.add(header)
            cardPanels.forEach { resultsPanel.add(it) }
        } else {
            resultsPanel.add(header)
            items.forEach { addCard(it) }
        }
    }

    private fun addCard(result: DependencyAnalysisResult) {
        val wrapper = buildCardPanel(result)
        resultsPanel.add(wrapper)
    }

    private fun createUpdateAllLink(
        items: List<DependencyAnalysisResult>,
        cardPanels: List<JPanel>,
    ): JBLabel {
        val updateAllLink =
            JBLabel("<html><b><span style='text-decoration:none'>Update All</span></b></html>")
        updateAllLink.font = updateAllLink.font.deriveFont(11f)
        updateAllLink.foreground = accentGreen
        updateAllLink.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        updateAllLink.border = JBUI.Borders.emptyRight(4)
        updateAllLink.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (updateAllLink.text.contains("Updated") || updateAllLink.text.contains("Updating")) {
                        return
                    }
                    updateAllLink.text =
                        "<html><b><span style='text-decoration:none'>Updating...</span></b></html>"
                    updateAllLink.foreground = subtleText
                    updateAllLink.cursor = Cursor.getDefaultCursor()

                    ApplicationManager.getApplication().executeOnPooledThread {
                        var successCount = 0
                        for (result in items) {
                            val dep = result.dependency
                            val latestVersion = result.latestVersion?.version ?: continue
                            val success =
                                updateDependencyVersion(dep.groupId, dep.artifactId, dep.rawVersion, latestVersion)
                            if (success) successCount++
                        }

                        SwingUtilities.invokeLater {
                            updateAllLink.text =
                                "<html><b><span style='text-decoration:none'>Updated $successCount/${items.size}</span></b></html>"

                            for (cardWrapper in cardPanels) {
                                updateCardAfterBulkUpdate(cardWrapper)
                            }
                            resultsPanel.revalidate()
                            resultsPanel.repaint()
                        }
                    }
                }

                override fun mouseEntered(e: MouseEvent?) {
                    if (!updateAllLink.text.contains("Updated") && !updateAllLink.text.contains("Updating")) {
                        updateAllLink.text =
                            "<html><b><span style='text-decoration:underline'>Update All</span></b></html>"
                    }
                }

                override fun mouseExited(e: MouseEvent?) {
                    if (!updateAllLink.text.contains("Updated") && !updateAllLink.text.contains("Updating")) {
                        updateAllLink.text =
                            "<html><b><span style='text-decoration:none'>Update All</span></b></html>"
                    }
                }
            },
        )
        return updateAllLink
    }

    private fun updateCardAfterBulkUpdate(cardWrapper: JPanel) {
        for (component in cardWrapper.components) {
            if (component is JPanel) {
                updateLinksInPanel(component)
            }
        }
    }

    private fun updateLinksInPanel(panel: JPanel) {
        for (component in panel.components) {
            if (component is JPanel) {
                updateLinksInPanel(component)
            } else if (component is JBLabel) {
                if (component.text?.contains("Update") == true &&
                    !component.text.contains("Updated") &&
                    component.foreground == accentGreen
                ) {
                    component.text = "<html><span style='text-decoration:none'>Updated!</span></html>"
                    component.foreground = subtleText
                    component.cursor = Cursor.getDefaultCursor()
                }
            }
        }
    }

    private fun buildCardPanel(result: DependencyAnalysisResult): JPanel {
        val wrapper = JPanel()
        wrapper.layout = BoxLayout(wrapper, BoxLayout.Y_AXIS)
        wrapper.isOpaque = false
        wrapper.alignmentX = Component.LEFT_ALIGNMENT

        val isOutdated = result.status == DependencyStatus.OUTDATED && result.latestVersion != null
        val statusColor =
            when (result.status) {
                DependencyStatus.OUTDATED -> accentOrange
                DependencyStatus.UP_TO_DATE -> accentGreen
                DependencyStatus.ERROR, DependencyStatus.NOT_FOUND -> accentRed
                DependencyStatus.UNRESOLVED -> subtleText
            }

        val card =
            object : JPanel(BorderLayout()) {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = cardBg
                    g2.fillRoundRect(0, 0, width, height, 10, 10)
                    g2.color = cardBorder
                    g2.drawRoundRect(0, 0, width - 1, height - 1, 10, 10)
                    g2.color = statusColor
                    g2.fillRoundRect(0, 0, 4, height, 4, 4)
                }
            }
        card.isOpaque = false
        card.border = JBUI.Borders.empty(8, 14, 8, 12)
        card.alignmentX = Component.LEFT_ALIGNMENT
        card.maximumSize = Dimension(Int.MAX_VALUE, if (isOutdated) 72 else 52)

        val dep = result.dependency

        val nameLabel = JBLabel(dep.artifactId)
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD, 12.5f)

        val groupLabel = JBLabel(dep.groupId)
        groupLabel.font = groupLabel.font.deriveFont(11f)
        groupLabel.foreground = subtleText

        val leftPanel = JPanel()
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
        leftPanel.isOpaque = false
        leftPanel.add(nameLabel)
        leftPanel.add(groupLabel)

        if (isOutdated) {
            leftPanel.add(Box.createVerticalStrut(2))

            val linksRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            linksRow.isOpaque = false
            linksRow.maximumSize = Dimension(Int.MAX_VALUE, 18)

            val linkColor = JBColor(Color(56, 132, 244), Color(88, 166, 255))

            val nexusLink = JBLabel("<html><span style='text-decoration:none'>View in Nexus</span></html>")
            nexusLink.font = nexusLink.font.deriveFont(10.5f)
            nexusLink.foreground = linkColor
            nexusLink.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            nexusLink.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        val config = NexusGuardSettings.getInstance().getConfig()
                        val base = config.baseUrl.trimEnd('/')
                        val repo = config.repositories.firstOrNull() ?: ""
                        val groupPath = dep.groupId.replace(".", "%2F")
                        val version = result.latestVersion?.version ?: dep.version
                        val url = "$base/#browse/browse:$repo:$groupPath%2F${dep.artifactId}%2F$version"
                        BrowserUtil.browse(url)
                    }

                    override fun mouseEntered(e: MouseEvent?) {
                        nexusLink.text = "<html><span style='text-decoration:underline'>View in Nexus</span></html>"
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        nexusLink.text = "<html><span style='text-decoration:none'>View in Nexus</span></html>"
                    }
                },
            )
            linksRow.add(nexusLink)

            val separator = JBLabel("  ·  ")
            separator.foreground = subtleText
            separator.font = separator.font.deriveFont(10.5f)
            linksRow.add(separator)

            val updateLink = JBLabel("<html><b><span style='text-decoration:none'>Update</span></b></html>")
            updateLink.font = updateLink.font.deriveFont(10.5f)
            updateLink.foreground = accentGreen
            updateLink.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            updateLink.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        val latestVersion = result.latestVersion?.version ?: return
                        val success = updateDependencyVersion(dep.groupId, dep.artifactId, dep.rawVersion, latestVersion)
                        if (success) {
                            updateLink.text = "<html><span style='text-decoration:none'>Updated!</span></html>"
                            updateLink.foreground = subtleText
                            updateLink.cursor = Cursor.getDefaultCursor()
                        }
                    }

                    override fun mouseEntered(e: MouseEvent?) {
                        if (!updateLink.text.contains("Updated")) {
                            updateLink.text =
                                "<html><b><span style='text-decoration:underline'>Update</span></b></html>"
                        }
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        if (!updateLink.text.contains("Updated")) {
                            updateLink.text =
                                "<html><b><span style='text-decoration:none'>Update</span></b></html>"
                        }
                    }
                },
            )
            linksRow.add(updateLink)

            leftPanel.add(linksRow)
        }

        val rightPanel = JPanel()
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.Y_AXIS)
        rightPanel.isOpaque = false

        when (result.status) {
            DependencyStatus.OUTDATED -> {
                val currentLabel = JBLabel(dep.version)
                currentLabel.font = currentLabel.font.deriveFont(11f)
                currentLabel.foreground = subtleText
                currentLabel.horizontalAlignment = SwingConstants.RIGHT
                currentLabel.alignmentX = Component.RIGHT_ALIGNMENT

                val latestLabel = JBLabel(result.latestVersion?.version ?: "")
                latestLabel.font = latestLabel.font.deriveFont(Font.BOLD, 12f)
                latestLabel.foreground = statusColor
                latestLabel.horizontalAlignment = SwingConstants.RIGHT
                latestLabel.alignmentX = Component.RIGHT_ALIGNMENT

                rightPanel.add(currentLabel)
                rightPanel.add(latestLabel)
            }
            else -> {
                val versionText =
                    when (result.status) {
                        DependencyStatus.UP_TO_DATE -> dep.version
                        DependencyStatus.ERROR -> dep.version
                        DependencyStatus.NOT_FOUND -> "${dep.version} (not found)"
                        DependencyStatus.UNRESOLVED -> dep.rawVersion
                        else -> dep.version
                    }
                val versionLabel = JBLabel(versionText)
                versionLabel.font = versionLabel.font.deriveFont(Font.BOLD, 11.5f)
                versionLabel.foreground = statusColor
                versionLabel.horizontalAlignment = SwingConstants.RIGHT
                versionLabel.alignmentX = Component.RIGHT_ALIGNMENT
                rightPanel.add(versionLabel)
            }
        }

        card.add(leftPanel, BorderLayout.CENTER)
        card.add(rightPanel, BorderLayout.EAST)

        wrapper.add(Box.createVerticalStrut(4))
        wrapper.add(card)
        return wrapper
    }

    private fun updateDependencyVersion(
        groupId: String,
        artifactId: String,
        rawVersion: String,
        newVersion: String,
    ): Boolean {
        val basePath = project.basePath ?: return false
        val pomVirtualFile = LocalFileSystem.getInstance().findFileByPath("$basePath/pom.xml") ?: return false
        val psiFile = PsiManager.getInstance(project).findFile(pomVirtualFile) as? XmlFile ?: return false

        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                val rootTag = psiFile.rootTag ?: return@runWriteCommandAction
                val isPropertyBased = rawVersion.startsWith("\${")

                if (isPropertyBased) {
                    val propertyName = rawVersion.removePrefix("\${").removeSuffix("}")
                    val propertiesTag = rootTag.findFirstSubTag("properties")
                    val propertyTag = propertiesTag?.findFirstSubTag(propertyName)
                    propertyTag?.value?.text = newVersion
                } else {
                    val depsTag = findAllDependencyTags(rootTag)
                    for (depTag in depsTag) {
                        val gid = depTag.findFirstSubTag("groupId")?.value?.text ?: continue
                        val aid = depTag.findFirstSubTag("artifactId")?.value?.text ?: continue
                        if (gid == groupId && aid == artifactId) {
                            val versionTag = depTag.findFirstSubTag("version")
                            versionTag?.value?.text = newVersion
                            break
                        }
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findAllDependencyTags(rootTag: XmlTag): List<XmlTag> {
        val result = mutableListOf<XmlTag>()
        for (child in rootTag.subTags) {
            if (child.name == "dependencies") {
                result.addAll(child.findSubTags("dependency"))
            }
            if (child.name == "dependencyManagement") {
                val inner = child.findFirstSubTag("dependencies")
                if (inner != null) {
                    result.addAll(inner.findSubTags("dependency"))
                }
            }
        }
        return result
    }

    private fun showEmptyState(message: String) {
        resultsPanel.removeAll()
        val label = JBLabel(message)
        label.foreground = subtleText
        label.border = JBUI.Borders.empty(20, 0)
        label.alignmentX = Component.LEFT_ALIGNMENT
        resultsPanel.add(label)
        resultsPanel.revalidate()
        resultsPanel.repaint()
    }
}
