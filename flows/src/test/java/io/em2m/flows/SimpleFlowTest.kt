package io.em2m.flows

import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.name.Names
import org.junit.Test
import org.slf4j.LoggerFactory
import rx.Observable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


typealias Context = Map<String, Any?>


@Singleton
class LoggingFlow @Inject constructor(@Named("JAVA_ENV") val env: String) : MainFlow<Context>() {

    val LOG = LoggerFactory.getLogger(SimpleFlowTest::class.java)

    override fun call(obs: Observable<Context>): Observable<Context> {
        return obs.doOnNext { context ->
            LOG.debug("$env: $context")
        }
    }

}

class TestModule : Module {

    override fun configure(binder: Binder) {
        //binder.bind(LoggingFlow::class.java)
        binder.bindConstant().annotatedWith(Names.named("JAVA_ENV")).to("dev")
    }

}

class NoOpTransformer(override val priority: Int = Priorities.INIT) : Transformer<Context> {

    override fun call(obs: Observable<Context>): Observable<Context> {
        return obs
    }

}


class SimpleFlowTest {

    @Test
    fun testSimple() {
        val injector = Guice.createInjector(TestModule())
        val resolver = LookupFlowResolver(injector, mapOf(
                "test" to LoggingFlow::class
        ))
        val runtime = BasicProcessor(resolver)
        Observable.just(mapOf("key" to "v1"),
                mapOf("key" to "v2"),
                mapOf("key" to "v3")).compose(runtime.transformer("test"))
                .test().assertCompleted().assertNoErrors()
    }

    @Test
    fun testBuilder() {
        val processor = BasicProcessor.Builder<Context>()
                .module(TestModule())
                .transformer(NoOpTransformer())
                .flow("test", LoggingFlow::class)
                .build()
        val source = Observable.just<Context>(mapOf("key" to "v2"), mapOf("key" to "v3"))
        processor.process("test", source).test().assertCompleted().assertNoErrors()
    }

}