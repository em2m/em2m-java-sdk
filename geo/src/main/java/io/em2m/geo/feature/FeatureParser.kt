package io.em2m.geo.feature

import java.io.InputStream

interface FeatureParser {

    fun handler(handler: FeatureHandler): FeatureParser

    fun filter(filter: FeatureFilter): FeatureParser

    fun parse(`in`: InputStream)

}
