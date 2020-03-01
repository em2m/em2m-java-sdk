package io.em2m.geo.feature

import io.em2m.geo.feature.Feature
import io.em2m.geo.feature.FeatureFilter
import io.em2m.geo.feature.FeatureHandler
import io.em2m.geo.feature.FeatureParser
import org.locationtech.jts.geom.Envelope

abstract class AbstractFeatureParser : FeatureParser {

    private var handler: FeatureHandler? = null
    private var filter: FeatureFilter? = null

    @Throws(Exception::class)
    protected fun begin() {
        handler!!.begin()
    }

    @Throws(Exception::class)
    protected fun end() {
        handler!!.end()
    }

    override fun filter(filter: FeatureFilter): FeatureParser {
        this.filter = filter
        return this
    }

    protected fun handle(bbox: Envelope) {
        handler!!.handle(bbox)
    }

    protected fun handle(feature: Feature) {
        try {
            if (filter == null || filter!!.accept(feature)) {
                handler!!.handle(feature)
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    override fun handler(handler: FeatureHandler): FeatureParser {
        this.handler = handler
        return this
    }

}
