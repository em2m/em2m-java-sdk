package io.em2m.flows

import com.google.inject.Binder
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.name.Names
import org.junit.Test
import rx.Observable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


typealias Context = MutableMap<String, Any?>

@Singleton
class LoggingFlow @Inject constructor(@Named("JAVA_ENV") private val env: String) : FlowSupport<Context>() {

    override fun main(obs: Observable<Context>): Observable<Context> {
        return obs.doOnNext { context ->
            println("$env: $context")
        }
    }

}

class TestModule : Module {

    override fun configure(binder: Binder) {
        binder.bindConstant().annotatedWith(Names.named("JAVA_ENV")).to("dev")
    }

}

class SimpleTransformer(priority: Int = Priorities.INIT) : TransformerSupport<Context>(priority) {

    override fun doOnNext(ctx: Context) {
        ctx["key2"] = "v2"
    }

}

class SimpleBuilder : AbstractBuilder<Context>() {

    override fun resolver(injector: Injector): FlowResolver<Context> {
        return object : FlowResolver<Context> {
            override fun findFlow(context: Context): Flow<Context>? {
                return injector.getInstance(LoggingFlow::class.java)
            }
        }
    }

}

class SimpleFlowTest {

    @Test
    fun testBuilder() {
        val processor = SimpleBuilder()
                .module(TestModule())
                .transformer(SimpleTransformer())
                .build()
        val value: Context = (mutableMapOf("key1" to "v1"))
        processor.process(value).test().assertCompleted().assertNoErrors()
    }

}