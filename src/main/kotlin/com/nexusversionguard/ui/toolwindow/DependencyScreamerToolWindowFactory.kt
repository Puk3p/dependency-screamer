package com.nexusversionguard.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DependencyScreamerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val panel = DependencyScreamerToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.rootPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
