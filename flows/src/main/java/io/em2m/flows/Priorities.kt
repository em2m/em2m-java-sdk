package io.em2m.flows

class Priorities {

    companion object {
        val INIT = 1000
        val PRE_AUTHENTICATE = 1500
        val AUTHENTICATE = 2000
        val PRE_PARSE = 2500
        val PARSE = 3000
        val PRE_AUTHORIZE = 3500
        val AUTHORIZE = 4000
        val POST_AUTHORIZE = 4500
        val MAIN = 5000
        val AUDIT = 6000
        val RESPONSE = 7000
        val COMPLETE = 8000
        val ERROR = 9000
    }

}
