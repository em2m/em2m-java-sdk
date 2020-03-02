package io.em2m.geo.feature


interface FeatureFilter {
    fun accept(feature: Feature): Boolean
}