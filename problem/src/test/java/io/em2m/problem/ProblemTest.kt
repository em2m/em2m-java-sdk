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

    @Test
    fun filterCharactersOnProblems() {
        val problem = Problem(
            status = Problem.Status.BAD_REQUEST,
            title = "Error parsing JSON request",
            detail = "Could not resolve type id '<script>nefariousFunction()</script>'"
        )
        assertEquals("Could not resolve type id 'scriptnefariousFunction()script'", problem.detail)
    }

    @Test
    fun filterCharactersOnConvertedProblems() {
        val convertedProblem = Problem.convert(IllegalStateException("Could not resolve type id '<script>nefariousFunction()</script>'"))
        assertEquals("Could not resolve type id 'scriptnefariousFunction()script'", convertedProblem.detail)
    }

    @Test
    fun ignoredCharactersCheck() {
        val convertedProblem = Problem.convert(IllegalStateException("These characters should be preserved:!@#$%^&*()[]{}\\|'\",.?"))
        assertEquals("These characters should be preserved:!@#$%^&*()[]{}\\|'\",.?", convertedProblem.detail)
    }

    // todo - implement and check exception filtering

}
