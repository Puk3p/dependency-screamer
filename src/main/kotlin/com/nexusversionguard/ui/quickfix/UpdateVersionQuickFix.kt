package com.nexusversionguard.ui.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlTag

class UpdateVersionQuickFix(
    private val latestVersion: String,
) : LocalQuickFix {
    override fun getFamilyName(): String = "Dependency Screamer"

    override fun getName(): String = "Update version to $latestVersion"

    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor,
    ) {
        val element = descriptor.psiElement ?: return
        val versionTag = findVersionTag(element)
        versionTag?.value?.text = latestVersion
    }

    private fun findVersionTag(element: com.intellij.psi.PsiElement): XmlTag? {
        var current = element
        while (current.parent != null) {
            if (current is XmlTag && current.name == "version") {
                return current
            }
            current = current.parent
        }
        return null
    }
}
