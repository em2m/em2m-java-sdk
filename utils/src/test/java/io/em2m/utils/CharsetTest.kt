package io.em2m.utils

import org.junit.Test
import java.io.ByteArrayOutputStream

class CharsetTest {

    private fun brokenCode(characters: List<Char>): String {
        val inputStream = characters.toString().byteInputStream()

        val buffer = ByteArray(8192)
        val outputStream = ByteArrayOutputStream()

        inputStream.buffered().use { input ->
            outputStream.use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val sanitizedChunk = String(buffer, 0, bytesRead)
                    output.write(sanitizedChunk.toByteArray())
                }
            }
        }

        return outputStream.toString()
    }

    private fun fixedCode(characters: List<Char>): String {
        val charset = Charsets.UTF_8
        val inputStream = characters.toString().byteInputStream(charset)
        return inputStream.reader(charset).use { it.readText() }
    }

    @Test
    fun `U+2019 Right Single Quotation Mark`() {
        val characters = mutableListOf<Char>()
        repeat(8191) { characters.add('a') }
        // https://www.compart.com/en/unicode/U+2019
        // Character represented as 3 bytes: 0xE2 0x80 0x99
        characters.add('’')

        val brokenRepresentation = brokenCode(characters)
        val fixedRepresentation = fixedCode(characters)

        assert('�' in brokenRepresentation)
        assert('�' !in fixedRepresentation)
    }

    @Test
    fun `U+2665 Heart Emoji`() {
        val characters = mutableListOf<Char>()
        repeat(8191) { characters.add('a') }
        // https://www.compart.com/en/unicode/U+2665
        // Character represented as 3 bytes: 0xE2 0x99 0xA5
        characters.add('♥')

        val brokenRepresentation = brokenCode(characters)
        val fixedRepresentation = fixedCode(characters)

        assert('�' in brokenRepresentation)
        assert('�' !in fixedRepresentation)
    }

}
