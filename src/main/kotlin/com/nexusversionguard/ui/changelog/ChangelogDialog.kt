package com.nexusversionguard.ui.changelog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.nexusversionguard.infrastructure.changelog.ChangelogEntry
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingConstants

class ChangelogDialog(
    project: Project,
    private val artifactId: String,
    private val fromVersion: String,
    private val toVersion: String,
    private val entries: List<ChangelogEntry>,
) : DialogWrapper(project, false) {
    init {
        title = "What changed in $artifactId"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(550, 400)

        val headerLabel = JBLabel("$artifactId: $fromVersion → $toVersion", SwingConstants.LEFT)
        headerLabel.font = headerLabel.font.deriveFont(Font.BOLD, 14f)
        headerLabel.border = JBUI.Borders.empty(0, 0, 8, 0)
        panel.add(headerLabel, BorderLayout.NORTH)

        if (entries.isEmpty()) {
            val emptyLabel = JBLabel("No changelog entries found between these versions.")
            emptyLabel.border = JBUI.Borders.empty(20)
            panel.add(emptyLabel, BorderLayout.CENTER)
        } else {
            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

            for (entry in entries) {
                val entryPane = JTextPane()
                entryPane.contentType = "text/html"
                entryPane.isEditable = false
                entryPane.isOpaque = false

                val dateStr = if (entry.date != null) " — ${entry.date}" else ""
                val html =
                    buildString {
                        append("<html><body style='font-family:sans-serif;font-size:11px;'>")
                        append("<h3 style='margin:4px 0;'>v${entry.version}$dateStr</h3>")
                        append("<div style='margin-left:8px;white-space:pre-wrap;'>")
                        append(markdownToSimpleHtml(entry.content))
                        append("</div>")
                        append("</body></html>")
                    }
                entryPane.text = html
                entryPane.border = JBUI.Borders.empty(4, 0, 8, 0)
                contentPanel.add(entryPane)
            }

            val scrollPane = JBScrollPane(contentPanel)
            scrollPane.border = JBUI.Borders.empty()
            panel.add(scrollPane, BorderLayout.CENTER)
        }

        return panel
    }

    private fun markdownToSimpleHtml(md: String): String {
        return md.lines().joinToString("\n") { line ->
            when {
                line.startsWith("### ") -> "<b>${line.removePrefix("### ")}</b>"
                line.startsWith("- ") -> "• ${line.removePrefix("- ")}"
                line.startsWith("* ") -> "• ${line.removePrefix("* ")}"
                line.isBlank() -> "<br/>"
                else -> line
            }
        }
    }

    override fun createActions() = arrayOf(okAction)
}
