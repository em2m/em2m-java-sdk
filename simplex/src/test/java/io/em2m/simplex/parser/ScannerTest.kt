package io.em2m.simplex.parser

import io.em2m.simplex.parser2.Parser
import io.em2m.simplex.parser2.Scanner
import org.junit.Test
import kotlin.test.Ignore

@Ignore
class ScannerTest {


    @Test
    fun testScanner() {
        val s = Scanner("5+3 == 8")
        val tokens = s.scanTokens()
        val expr = Parser(tokens).parse()
        expr?.call(emptyMap())
    }

}