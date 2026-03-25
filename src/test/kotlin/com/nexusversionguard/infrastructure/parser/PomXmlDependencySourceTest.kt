package com.nexusversionguard.infrastructure.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PomXmlDependencySourceTest {
    private val propertyResolver = MavenPropertyResolver()
    private val source = PomXmlDependencySource(propertyResolver)

    @Test
    fun `extracts simple dependency`() {
        val pom =
            """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>32.1.3-jre</version>
                    </dependency>
                </dependencies>
            </project>
            """.trimIndent()

        val deps = source.extractDependencies(pom)

        assertThat(deps).hasSize(1)
        assertThat(deps[0].groupId).isEqualTo("com.google.guava")
        assertThat(deps[0].artifactId).isEqualTo("guava")
        assertThat(deps[0].version).isEqualTo("32.1.3-jre")
        assertThat(deps[0].isPropertyBased).isFalse()
    }

    @Test
    fun `extracts multiple dependencies`() {
        val pom =
            """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-api</artifactId>
                        <version>2.0.9</version>
                    </dependency>
                    <dependency>
                        <groupId>ch.qos.logback</groupId>
                        <artifactId>logback-classic</artifactId>
                        <version>1.4.14</version>
                    </dependency>
                </dependencies>
            </project>
            """.trimIndent()

        val deps = source.extractDependencies(pom)

        assertThat(deps).hasSize(2)
        assertThat(deps[0].groupId).isEqualTo("org.slf4j")
        assertThat(deps[1].groupId).isEqualTo("ch.qos.logback")
    }

    @Test
    fun `resolves property-based version`() {
        val pom =
            """
            <project>
                <properties>
                    <jackson.version>2.15.3</jackson.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>${'$'}{jackson.version}</version>
                    </dependency>
                </dependencies>
            </project>
            """.trimIndent()

        val deps = source.extractDependencies(pom)

        assertThat(deps).hasSize(1)
        assertThat(deps[0].version).isEqualTo("2.15.3")
        assertThat(deps[0].rawVersion).isEqualTo("\${jackson.version}")
        assertThat(deps[0].isPropertyBased).isTrue()
    }

    @Test
    fun `handles dependency without version tag`() {
        val pom =
            """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>no-version</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """.trimIndent()

        val deps = source.extractDependencies(pom)

        assertThat(deps).isEmpty()
    }

    @Test
    fun `handles empty pom`() {
        val pom = "<project></project>"

        val deps = source.extractDependencies(pom)

        assertThat(deps).isEmpty()
    }

    @Test
    fun `marks unresolved property as not resolved`() {
        val pom =
            """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>lib</artifactId>
                        <version>${'$'}{missing.property}</version>
                    </dependency>
                </dependencies>
            </project>
            """.trimIndent()

        val deps = source.extractDependencies(pom)

        assertThat(deps).hasSize(1)
        assertThat(deps[0].isResolved).isFalse()
        assertThat(deps[0].isPropertyBased).isTrue()
    }
}
