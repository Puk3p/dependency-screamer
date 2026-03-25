package com.nexusversionguard.ui.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlTag
import com.nexusversionguard.application.service.DependencyAnalysisServiceProvider
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.infrastructure.settings.NexusGuardSettings
import com.nexusversionguard.ui.quickfix.UpdateVersionQuickFix

class OutdatedDependencyInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor {
        val settings = NexusGuardSettings.getInstance()
        if (!settings.isConfigured()) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : XmlElementVisitor() {
            override fun visitXmlTag(tag: XmlTag) {
                if (tag.name != "dependency") return

                val parentTag = tag.parentTag ?: return
                if (parentTag.name != "dependencies") return

                val groupIdTag = tag.findFirstSubTag("groupId") ?: return
                val artifactIdTag = tag.findFirstSubTag("artifactId") ?: return
                val versionTag = tag.findFirstSubTag("version") ?: return

                val groupId = groupIdTag.value.text
                val artifactId = artifactIdTag.value.text
                val coordinatesKey = "$groupId:$artifactId"

                val service = DependencyAnalysisServiceProvider.getInstance().getService()
                val cached = service.getCachedResult(coordinatesKey) ?: return

                when (cached.status) {
                    DependencyStatus.OUTDATED -> {
                        val latest = cached.latestVersion?.version ?: return
                        holder.registerProblem(
                            versionTag,
                            "Newer version available in Nexus: $latest",
                            UpdateVersionQuickFix(latest),
                        )
                    }
                    DependencyStatus.NOT_FOUND -> {
                        holder.registerProblem(
                            versionTag,
                            "Artifact not found in configured Nexus repositories",
                        )
                    }
                    DependencyStatus.ERROR -> {
                        val message = cached.errorMessage ?: "Unknown error"
                        holder.registerProblem(versionTag, "Nexus check failed: $message")
                    }
                    DependencyStatus.UNRESOLVED -> {
                        holder.registerProblem(
                            versionTag,
                            "Could not resolve version property: ${cached.dependency.rawVersion}",
                        )
                    }
                    DependencyStatus.UP_TO_DATE -> {}
                }
            }
        }
    }

}
