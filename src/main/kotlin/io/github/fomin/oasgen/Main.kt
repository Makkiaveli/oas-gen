package io.github.fomin.oasgen

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.github.fomin.oasgen.java.rest.operations.JavaSpringRestOperationsWriter
import io.github.fomin.oasgen.java.spring.mvc.JavaSrpingMvcServerWriter
import io.github.fomin.oasgen.typescript.axios.AxiosClientWriter
import org.apache.commons.cli.*
import java.io.File
import kotlin.system.exitProcess

const val BASE_DIR = "base-dir"
const val SCHEMA = "schema"
const val COMPONENTS = "components"
const val OUTPUT_DIR = "output-dir"
const val NAMESPACE = "namespace"
const val GENERATOR = "generator"

private val writerFactories = mapOf(
        "java-spring-mvc" to ::JavaSrpingMvcServerWriter,
        "java-spring-rest-operations" to ::JavaSpringRestOperationsWriter,
        "typescript-axios" to ::AxiosClientWriter
)

fun main(args: Array<String>) {
    val options = Options()
            .addRequiredOption("b", BASE_DIR, true, "base directory")
            .addRequiredOption("s", SCHEMA, true, "schema file")
            .addOption(
                    Option.builder("c").longOpt(COMPONENTS)
                            .hasArgs().desc("component files").build()
            )
            .addRequiredOption("o", OUTPUT_DIR, true, "output directory")
            .addRequiredOption("n", NAMESPACE, true, "namespace")
            .addRequiredOption("g", GENERATOR, true, "generator identifier")

    val parser = DefaultParser()
    val commandLine: CommandLine
    try {
        commandLine = parser.parse(options, args)
    } catch (exp: ParseException) {
        println(exp.message)
        val formatter = HelpFormatter()
        formatter.printHelp("java", options)
        exitProcess(1)
    }

    val baseDirArg = commandLine.getOptionValue(BASE_DIR)
    val schemaFileArg = commandLine.getOptionValue(SCHEMA)
    val componentsArg = commandLine.getOptionValues(COMPONENTS) ?: emptyArray()
    val outputDirArg = commandLine.getOptionValue(OUTPUT_DIR)
    val namespaceArg = commandLine.getOptionValue(NAMESPACE)
    val generatorId = commandLine.getOptionValue(GENERATOR)

    val yamlMapper = ObjectMapper(YAMLFactory())
    val jsonMapper = ObjectMapper()
    val mapTypeReference = object : TypeReference<Map<*, *>>() {}
    val baseUri = File(baseDirArg).toURI()

    fun loadMap(filePathStr: String): RootFragment {
        val mapper = when (val extension = filePathStr.substring(filePathStr.lastIndexOf(".") + 1)) {
            "json" -> jsonMapper
            "yaml" -> yamlMapper
            "yml" -> yamlMapper
            else -> error("Unsupported extension $extension")
        }
        val file = File(filePathStr)
        val map = mapper.readValue(file, mapTypeReference)
        val fileUri = file.toURI()
        val relativeFilePath = baseUri.relativize(fileUri)
        return RootFragment(relativeFilePath.toString(), map)
    }

    val schemaRootFragment = loadMap(schemaFileArg)

    val componentFragments = componentsArg.map { componentArg ->
        loadMap(componentArg)
    }

    val fragmentRegistry = FragmentRegistry(componentFragments + schemaRootFragment)
    val openApiSchema = OpenApiSchema(fragmentRegistry.get(Reference.root(schemaRootFragment.path)), null)

    val writerFactory = writerFactories[generatorId] ?: error("Can't find generator $generatorId")
    val writer = writerFactory(namespaceArg)

    val outputFiles = writer.write(listOf(openApiSchema))
    val outputDir = File(outputDirArg)
    outputDir.mkdirs()
    outputFiles.forEach { outputFile ->
        val generatedFile = File(outputDir, outputFile.path)
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText(outputFile.content)
    }

}
