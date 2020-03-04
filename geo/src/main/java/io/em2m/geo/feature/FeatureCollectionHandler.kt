package io.em2m.geo.feature

import org.locationtech.jts.geom.Envelope


class FeatureCollectionHandler : FeatureHandler {

    val collection = FeatureCollection()

    override fun begin() {}

    override fun end() {}

    override fun handle(feature: Feature) {
        collection.features.add(feature)
    }

    override fun handle(bbox: Envelope) {
        collection.bbox = bbox
    }

}