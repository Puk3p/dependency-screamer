package com.nexusversionguard.infrastructure.parser

import com.nexusversionguard.domain.port.PropertyResolver
import java.util.regex.Pattern

class MavenPropertyResolver : PropertyResolver {
    private val propertyTagPattern = Pattern.compile("<([^>/]+)>([^<]+)</\\1>")
    private val propertyRefPattern = Pattern.compile("\\$\\{([^}]+)}")
    private val propertiesBlockPattern =
        Pattern.compile(
            "<properties>(.*?)</properties>",
            Pattern.DOTALL,
        )

    override fun resolveProperties(content: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()

        val blockMatcher = propertiesBlockPattern.matcher(content)
        while (blockMatcher.find()) {
            val block = blockMatcher.group(1)
            val tagMatcher = propertyTagPattern.matcher(block)
            while (tagMatcher.find()) {
                val key = tagMatcher.group(1).trim()
                val value = tagMatcher.group(2).trim()
                properties[key] = value
            }
        }

        return resolveTransitive(properties)
    }

    override fun resolveValue(
        raw: String,
        properties: Map<String, String>,
    ): String {
        var resolved = raw
        val matcher = propertyRefPattern.matcher(raw)

        while (matcher.find()) {
            val propertyName = matcher.group(1)
            val propertyValue = properties[propertyName]
            if (propertyValue != null) {
                resolved = resolved.replace("\${$propertyName}", propertyValue)
            }
        }

        return resolved
    }

    private fun resolveTransitive(properties: MutableMap<String, String>): Map<String, String> {
        var changed = true
        var iterations = 0
        val maxIterations = 10

        while (changed && iterations < maxIterations) {
            changed = false
            iterations++

            for ((key, value) in properties.toMap()) {
                val resolved = resolveValue(value, properties)
                if (resolved != value) {
                    properties[key] = resolved
                    changed = true
                }
            }
        }

        return properties.toMap()
    }
}
