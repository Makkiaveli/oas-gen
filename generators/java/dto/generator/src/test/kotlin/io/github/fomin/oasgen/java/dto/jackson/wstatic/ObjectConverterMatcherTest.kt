package io.github.fomin.oasgen.java.dto.jackson.wstatic

import io.github.fomin.oasgen.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

internal class ObjectConverterMatcherTest {

    @Test
    fun `matches object`() {
        val objectConverterMatcher = ObjectConverterMatcher("com.example")
        val converterRegistry = ConverterRegistry(
            CompositeConverterMatcher(
                listOf(
                    objectConverterMatcher
                )
            )
        )
        val fragmentRegistry = FragmentRegistry(
            InMemoryContentLoader(
                mapOf(
                    "dto.yaml" to "type: object"
                )
            )
        )
        val jsonSchema = JsonSchema(fragmentRegistry.get(Reference.root("dto.yaml")), null)
        val converterWriter = objectConverterMatcher.match(converterRegistry, jsonSchema)
            ?: error("Expected non-null value")
        assertThrows<Exception> {
            converterWriter.stringParseExpression("any")
        }
        assertThrows<Exception> {
            converterWriter.stringWriteExpression("any")
        }
    }

    @Test
    fun `simple dto`() {
        val stringConverterMatcher = StringConverterMatcher()
        val objectConverterMatcher = ObjectConverterMatcher("com.example.simple")
        val converterRegistry = ConverterRegistry(
            CompositeConverterMatcher(
                listOf(
                    stringConverterMatcher,
                    objectConverterMatcher
                )
            )
        )
        jsonSchemaTestCase(
            JavaDtoWriter(converterRegistry),
            File("build/test-schemas/dto/simple"),
            "dto.yaml",
            File("../expected-dto/src/simple/java")
        )
    }

    @Test
    fun javadoc() {
        val stringConverterMatcher = StringConverterMatcher()
        val objectConverterMatcher = ObjectConverterMatcher("com.example.javadoc")
        val converterRegistry = ConverterRegistry(
            CompositeConverterMatcher(
                listOf(
                    stringConverterMatcher,
                    objectConverterMatcher
                )
            )
        )
        jsonSchemaTestCase(
            JavaDtoWriter(converterRegistry),
            File("build/test-schemas/dto/documentation"),
            "dto.yaml",
            File("../expected-dto/src/javadoc/java")
        )
    }
}
