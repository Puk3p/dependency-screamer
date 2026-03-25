package com.nexusversionguard.ui.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings

class DependencyVersionAnnotator : Annotator {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (element !is XmlTag) return
        if (element.name != "version") return

        val parentTag = element.parentTag ?: return
        if (parentTag.name != "dependency") return

        val dependencyTag = parentTag
        val groupIdTag = dependencyTag.findFirstSubTag("groupId") ?: return
        val artifactIdTag = dependencyTag.findFirstSubTag("artifactId") ?: return

        val groupId = groupIdTag.value.text
        val artifactId = artifactIdTag.value.text
        val coordinatesKey = "$groupId:$artifactId"

        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) return

        val service = DependencyAnalysisServiceProvider.getInstance().getService()
        val cached = service.getCachedResult(coordinatesKey) ?: return

        when (cached.status) {
            DependencyStatus.OUTDATED -> {
                val latest = cached.latestVersion?.version ?: return
                holder.newAnnotation(
                    HighlightSeverity.WARNING,
                    "Newer version available in Nexus: $latest",
                ).range(element).create()
            }
            DependencyStatus.ERROR -> {
                holder.newAnnotation(
                    HighlightSeverity.WEAK_WARNING,
                    "Nexus check failed: ${cached.errorMessage}",
                ).range(element).create()
            }
            else -> {}
        }
    }
}
