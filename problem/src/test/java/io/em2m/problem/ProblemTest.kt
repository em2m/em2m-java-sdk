package io.em2m.problem

import org.junit.Test
import kotlin.test.assertEquals


class ProblemTest {

    @Test
    fun sanityCheck() {
        val problem = Problem(title = "title")
        val title = problem.title
        assertEquals("title", title)
    }

    // todo - implement and check exception filtering

}