package com.nexusversionguard.infrastructure.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MavenPropertyResolverTest {
    private val resolver = MavenPropertyResolver()

    @Test
    fun `resolveProperties extracts simple properties`() {
        val pom =
            """
            <project>
                <properties>
                    <jackson.version>2.15.3</jackson.version>
                    <spring.version>6.1.0</spring.version>
                </properties>
            </project>
            """.trimIndent()

        val result = resolver.resolveProperties(pom)

        assertThat(result).containsEntry("jackson.version", "2.15.3")
        assertThat(result).containsEntry("spring.version", "6.1.0")
    }

    @Test
    fun `resolveProperties handles empty properties block`() {
        val pom =
            """
            <project>
                <properties>
                </properties>
            </project>
            """.trimIndent()

        val result = resolver.resolveProperties(pom)

        assertThat(result).isEmpty()
    }

    @Test
    fun `resolveProperties handles no properties block`() {
        val pom =
            """
            <project>
                <dependencies/>
            </project>
            """.trimIndent()

        val result = resolver.resolveProperties(pom)

        assertThat(result).isEmpty()
    }

    @Test
    fun `resolveProperties resolves transitive references`() {
        val pom =
            """
            <project>
                <properties>
                    <base.version>2.0</base.version>
                    <lib.version>${'$'}{base.version}.1</lib.version>
                </properties>
            </project>
            """.trimIndent()

        val result = resolver.resolveProperties(pom)

        assertThat(result["lib.version"]).isEqualTo("2.0.1")
    }

    @Test
    fun `resolveValue replaces property reference`() {
        val properties = mapOf("jackson.version" to "2.15.3")

        val result = resolver.resolveValue("\${jackson.version}", properties)

        assertThat(result).isEqualTo("2.15.3")
    }

    @Test
    fun `resolveValue returns original when property not found`() {
        val properties = emptyMap<String, String>()

        val result = resolver.resolveValue("\${unknown.version}", properties)

        assertThat(result).isEqualTo("\${unknown.version}")
    }

    @Test
    fun `resolveValue returns plain value unchanged`() {
        val properties = mapOf("x" to "y")

        val result = resolver.resolveValue("1.2.3", properties)

        assertThat(result).isEqualTo("1.2.3")
    }
}
