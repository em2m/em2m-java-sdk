package io.em2m.actions.xforms

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlSanitizerNoCssTest {

    private val sanitizer = JacksonRequestTransformer.HtmlSanitizer(jacksonObjectMapper())

    @Test
    fun `basic allowed tags are preserved`() {
        val input = """<p style="color: red">Hello <b>World</b></p>"""
        val output = sanitizer.sanitizePayload(input)

        assertTrue(output.contains("<p"), "p tag should be present")
        assertTrue(output.contains("style=\"color: red\""), "style attribute should be preserved")
        assertTrue(output.contains("<b>World</b>"), "b tag should be preserved")
    }

    @Test
    fun `script tags are removed but text remains`() {
        val input = """<div>before<script>alert('x')</script>after</div>"""
        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<script", ignoreCase = true))
        assertTrue(output.contains("before"))
        assertTrue(output.contains("after"))
    }

    @Test
    fun `unknown tags are dropped but inner text remains`() {
        val input = """<foo><bar>text</bar></foo>"""
        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<foo"), "foo tag should be removed")
        assertFalse(output.contains("<bar"), "bar tag should be removed")
        assertTrue(output.contains("text"), "inner text should remain")
    }

    @Test
    fun `style blocks are preserved as-is`() {
        val input = """
            <style>
              body { background-image: url("javascript:alert(1)"); }
              p { color: red; }
            </style>
            <p>ok</p>
        """.trimIndent()

        val output = sanitizer.sanitizePayload(input)

        assertTrue(output.contains("<style"), "style tag should be preserved")
        assertTrue(output.contains("background-image"), "CSS inside style should be preserved")
        assertTrue(output.contains("javascript:alert(1)", ignoreCase = true), "CSS javascript: should still be there")
        assertTrue(output.contains("<p>ok</p>"), "p tag should be preserved")
    }

    @Test
    fun `inline style with javascript url is preserved`() {
        val input = """<div style="background-image: url(javascript:alert(1)); color: red;">X</div>"""
        val output = sanitizer.sanitizePayload(input)

        assertTrue(output.contains("<div"), "div tag should be present")
        assertTrue(output.contains("background-image"), "CSS property should be preserved")
        assertTrue(output.contains("javascript:alert(1)", ignoreCase = true), "CSS javascript: should still be there")
        assertTrue(output.contains("color: red"), " Safe-looking property also preserved")
    }

    @Test
    fun `tag soup with scripts and unknown tags`() {
        val input = """
            <div>
              hi
              <<script>alert(1)//</script>
              <unknown>secret</unknown>
              <p>para</p>
            </div>
        """.trimIndent()

        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<script", ignoreCase = true))
        assertFalse(output.contains("<unknown", ignoreCase = true))
        assertTrue(output.contains("<div"), "div should remain")
        assertTrue(output.contains("<p>para</p>"), "p should remain")
        assertTrue(output.contains("secret"), "inner text of unknown tag should remain")
    }

    @Test
    fun `json payload sanitizes text fields`() {
        val input = """
            {
              "title": "<h1 onclick='alert(1)'>Hello</h1>",
              "body": "<div>before<script>alert('x')</script>after</div>",
              "styleBlock": "<style>body{background-image:url(javascript:alert(1))}</style><p>ok</p>"
            }
        """.trimIndent()

        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<script", ignoreCase = true))

        assertFalse(output.contains("<h1"), "h1 should be removed by safelist")
        assertTrue(output.contains("Hello"), "inner text from h1 should be preserved")

        assertTrue(output.contains("before"), "before text preserved")
        assertTrue(output.contains("after"), "after text preserved")

        assertTrue(output.contains("<style"), "style tag should be preserved")
        assertTrue(output.contains("javascript:alert(1)", ignoreCase = true), "CSS javascript: preserved")
        assertTrue(output.contains("<p>ok</p>"), "p tag preserved in styleBlock")
    }

    @Test
    fun `array of messages in json`() {
        val input = """
            {
              "messages": [
                "<div style='color: blue'>ok</div>",
                "<div>before<script>alert(2)</script>after</div>"
              ]
            }
        """.trimIndent()

        val output = sanitizer.sanitizePayload(input)

        assertTrue(output.contains("color: blue"), "first message style preserved")
        assertFalse(output.contains("<script", ignoreCase = true), "script in second message removed")
        assertTrue(output.contains("before"), "second message before text preserved")
        assertTrue(output.contains("after"), "second message after text preserved")
    }

    @Test
    fun `scan payload x oncut is neutralized`() {
        val input = """"><x oncut=ns(801)>"""
        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<x", ignoreCase = true), "<x> tag should be removed")
        assertFalse(output.contains("oncut", ignoreCase = true), "event handler attr should be removed")
    }

    @Test
    fun `scan payload search fields script injection is neutralized`() {
        val input = "\"--></style></scRipt><scRipt>netsparker(0x000C16)</scRipt>"
        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<script", ignoreCase = true), "script start tags should be removed")
        assertFalse(output.contains("</script", ignoreCase = true), "script end tags should be removed")
        assertFalse(output.contains("</style>"), "stray closing style tags should not remain as markup")
        assertTrue(output.contains("netsparker(0x000C16)"), "payload text may remain, but without executable script tag")
    }

    @Test
    fun `scan payload as json in search fields structure`() {
        val input = """
            {
              "search": {
                "fields": [
                  { "name": "normal" },
                  { "name": "\"--></style></scRipt><scRipt>netsparker(0x00ABCD)</scRipt>" }
                ]
              }
            }
        """.trimIndent()

        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<script", ignoreCase = true), "script tags should be stripped from JSON values")
        assertFalse(output.contains("</script", ignoreCase = true))
        assertFalse(output.contains("</style>"), "closing style tags should not survive as markup")
        assertTrue(output.contains("netsparker(0x00ABCD)"), "payload text may remain for debugging/logging")
    }

    @Test
    fun `scan payload with attribute breakout and script tag`() {
        val input = """ "><img src=x onerror=alert(1)><scRipt>netsparker(123)</scRipt> """
        val output = sanitizer.sanitizePayload(input)

        assertFalse(output.contains("<script", ignoreCase = true))
        assertFalse(output.contains("</script", ignoreCase = true))
        assertTrue(output.contains("<img"), "img tag should be preserved by safelist")
        assertFalse(output.contains("onerror", ignoreCase = true), "event handler should be removed")
        assertFalse(output.contains("\"\"><img"), "should not re-introduce unsafe attribute context")
    }
}
