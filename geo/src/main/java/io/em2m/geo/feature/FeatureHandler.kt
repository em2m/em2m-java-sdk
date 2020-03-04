package io.em2m.geo.feature

import org.locationtech.jts.geom.Envelope

interface FeatureHandler {

    fun begin()

    fun end()

    fun handle(feature: Feature)

    fun handle(bbox: Envelope)
}