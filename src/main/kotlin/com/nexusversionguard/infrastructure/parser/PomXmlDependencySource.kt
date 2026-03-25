package com.nexusversionguard.infrastructure.parser

import com.nexusversionguard.domain.model.MavenCoordinates
import com.nexusversionguard.domain.model.MavenDependency
import com.nexusversionguard.domain.port.DependencySource
import com.nexusversionguard.domain.port.PropertyResolver
import java.util.regex.Pattern

class PomXmlDependencySource(
    private val propertyResolver: PropertyResolver,
) : DependencySource {
    private val dependencyBlockPattern =
        Pattern.compile(
            "<dependency>(.*?)</dependency>",
            Pattern.DOTALL,
        )
    private val groupIdPattern = Pattern.compile("<groupId>([^<]+)</groupId>")
    private val artifactIdPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>")
    private val versionPattern = Pattern.compile("<version>([^<]+)</version>")
    private val propertyRefPattern = Pattern.compile("\\$\\{.+}")

    override fun extractDependencies(content: String): List<MavenDependency> {
        val properties = propertyResolver.resolveProperties(content)
        val lines = content.lines()
        val dependencies = mutableListOf<MavenDependency>()

        val matcher = dependencyBlockPattern.matcher(content)
        while (matcher.find()) {
            val block = matcher.group(1)
            val dependency = parseDependencyBlock(block, matcher.start(), properties, lines)
            if (dependency != null) {
                dependencies.add(dependency)
            }
        }

        return dependencies
    }

    private fun parseDependencyBlock(
        block: String,
        blockStartOffset: Int,
        properties: Map<String, String>,
        lines: List<String>,
    ): MavenDependency? {
        val groupId = extractTagValue(block, groupIdPattern) ?: return null
        val artifactId = extractTagValue(block, artifactIdPattern) ?: return null
        val rawVersion = extractTagValue(block, versionPattern) ?: return null

        val isPropertyBased = propertyRefPattern.matcher(rawVersion).matches()
        val resolvedVersion =
            if (isPropertyBased) {
                propertyResolver.resolveValue(rawVersion, properties)
            } else {
                rawVersion
            }

        val versionLineNumber = findVersionLineNumber(blockStartOffset, block, lines)

        return MavenDependency(
            coordinates = MavenCoordinates(groupId.trim(), artifactId.trim()),
            version = resolvedVersion.trim(),
            rawVersion = rawVersion.trim(),
            lineNumber = versionLineNumber,
            isPropertyBased = isPropertyBased,
        )
    }

    private fun extractTagValue(
        block: String,
        pattern: Pattern,
    ): String? {
        val matcher = pattern.matcher(block)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun findVersionLineNumber(
        blockStartOffset: Int,
        block: String,
        lines: List<String>,
    ): Int {
        val versionMatcher = versionPattern.matcher(block)
        if (!versionMatcher.find()) return 0

        val versionOffsetInBlock = versionMatcher.start()
        val absoluteOffset = blockStartOffset + versionOffsetInBlock

        var charCount = 0
        for ((index, line) in lines.withIndex()) {
            charCount += line.length + 1
            if (charCount > absoluteOffset) {
                return index + 1
            }
        }

        return 0
    }
}
