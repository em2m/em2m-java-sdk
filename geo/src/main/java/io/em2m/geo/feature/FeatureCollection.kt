package io.em2m.geo.feature

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.index.strtree.STRtree
import java.util.*

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
class FeatureCollection {
    var id: String? = null
    var bbox: Envelope? = null
    var crs: CRS? = null
    var features: MutableList<Feature> = ArrayList()
    
    fun rtee(): SpatialIndex {
        val result = STRtree()
        for (feature in features) {
            if (feature.geometry != null) {
                result.insert(feature.geometry!!.envelopeInternal, feature)
            }
        }
        return result
    }
}
