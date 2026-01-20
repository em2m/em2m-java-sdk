package io.em2m.search.es2.operations

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es2.models.Es2MappingProperty
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull

/*
* env variables:
* indexWhitelist=index-name-2025-12-13,big-macs-2005-01-15
* esIn=/Users/janedoe/Documents/esIn
*
* */
@Ignore
class Es2LoadSchemaTest {

    var inputDirectory: File = assertNotNull(System.getenv()["esIn"]?.let { path -> File(path) },
        "No env variable passed for \"esIn\". Please check your run configuration in IntelliJ.")
    var es2Directory: File = File(inputDirectory, "es2")

    val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun `verify directories`() {
        assert(inputDirectory.isDirectory && inputDirectory.exists()) {
            "Input directory \"${inputDirectory.absolutePath}\" doesn't exist."
        }
        assert(es2Directory.isDirectory && es2Directory.exists()) {
            "Es2 directory \"${es2Directory.absolutePath}\" doesn't exist."
        }
    }

    @Test
    fun `import all es2 schemas`() {
        val es2Schemas = Es2MappingProperty.load(es2Directory, objectMapper)
        println(es2Schemas)
    }

}
