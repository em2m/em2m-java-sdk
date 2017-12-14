package io.em2m.flows

class Priorities {

    companion object {
        val INIT = 1000
        val PRE_AUTHENTICATE = 1500
        val AUTHENTICATE = 2000
        val POST_AUTHENTICATE = 2500
        val PARSE = 3000
        val PRE_AUTHORIZE = 3500
        val AUTHORIZE = 4000
        val POST_AUTHORIZE = 4500
        val MAIN = 5000
        val AUDIT = 6000
        val COMPLETE = 7000
    }

}
